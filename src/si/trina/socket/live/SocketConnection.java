package si.trina.socket.live;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketConnection implements Runnable {

	final Logger logger = LoggerFactory.getLogger(SocketConnection.class);

	public String name, ip;
	public int port;
	public DataOutputStream outToServer;
	public InputStream inFromServer;
	public Socket socket;
	public LinkedList<ByteArrayOutputStream> toServerQueue;
	public ArrayList<SocketListener> listeners;
	public boolean connected = false;
	public Object socketObjectLock, readerLock, writerLock, toServerQueueLock, fromServerQueueLock;
	public boolean connectionError = false;
	public String charsetName = "UTF-8";
	public long reconnectInterval = 1000;
	public long checkConnectionInterval = 1000;
	public boolean readEnabled = true;
	public boolean writeEnabled = true;
	private SocketWrite socketWriteThread;
	private SocketRead socketReadThread;
	
	public SocketConnection(String name,String ip,int port) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.toServerQueue = new LinkedList<ByteArrayOutputStream>();
		this.listeners = new ArrayList<SocketListener>();
		this.readerLock = new Object();
		this.writerLock = new Object();
		this.toServerQueueLock = new Object();
		this.fromServerQueueLock = new Object();
		this.socketObjectLock = new Object();
	}
	
	public int byteArrayToInt(byte[] b) {
	    return   b[0] & 0xFF |
	            (b[1] & 0xFF) << 8 |
	            (b[2] & 0xFF) << 16 |
	            (b[3] & 0xFF) << 24;
	}

	// using least significant byte first
	public short byteArrayToUInt16(byte[] b) {
		if (b.length == 1) {
			return (short) (b[0] & 0xFF);
		}
	    return   (short) (b[0] & 0xFF |
	            (b[1] & 0xFF) << 8);
	}

	public short byteArrayToUInt8(byte[] b) {
	    return   (short) (b[0] & 0xFF);
	}

	public byte[] intToByteArray(int a) {
	    return new byte[] {
	        (byte) (a & 0xFF),
	        (byte) ((a >> 8) & 0xFF),   
	        (byte) ((a >> 16) & 0xFF),   
	        (byte) ((a >> 24) & 0xFF)
	    };
	}

	public byte[] uint16ToByteArray(short a)
	{
	    return new byte[] {
	        (byte) (a & 0xFF),
	        (byte) ((a >> 8) & 0xFF)
	    };
	}
	
	public byte[] stringToByteArray(String str, int numBytes) {
		byte[] stringBytes = null;
		try {
			stringBytes = str.getBytes(Charset.forName(this.charsetName));
		} catch (Exception e) {
			e.printStackTrace();
			return stringBytes;
		}
		byte [] bytes = new byte[numBytes];
		
		int len = stringBytes.length;
		if (len > numBytes) {
			len = numBytes;
		}
		
		for (int i=0;i<len;i++) {
			bytes[i] = stringBytes[i];
		}
		
		return bytes;
	}
	
	public String byteArrayToString(byte[] bytes) {		
		String text = "";
		try {
			text = new String(bytes, this.charsetName);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return text;
	}

	public byte[] calculateHeader(byte[] message) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		int length = message.length;
		byte[] blength = this.intToByteArray(length);
		try {
			outputStream.write(blength);
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte checksum = (byte)(blength[0] ^ blength[1] ^ blength[2] ^ blength[3]);
		outputStream.write(checksum);
		outputStream.write(0);
		byte[] ret = outputStream.toByteArray();
		return ret;
	}
	
	public int calculateDatagramLength(byte[] header) {
		// bytes from 0-3
		byte[] byteHeaderDatagramLength = new byte[] {(byte)header[0], (byte)header[1], (byte)header[2], (byte)header[3]};
		int datagramLength = this.byteArrayToInt(byteHeaderDatagramLength);
		return datagramLength;
	}
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
	
	private void connect() {
		try {
			// fix server hanging when it cannot connect to the socket
			synchronized(this.socketObjectLock) {
				this.socket = new Socket();
				// TODO: add the option to bind the socket to specific local port
				this.socket.connect(new InetSocketAddress(this.ip, this.port), 200);
			}
			synchronized (this.readerLock) {
				this.inFromServer = socket.getInputStream();
			}
			synchronized (this.writerLock) {
				this.outToServer = new DataOutputStream(socket.getOutputStream());
			}
			
			this.connected = true;
			System.out.println("Socket connected " + this.name);
		} catch (Exception e) {
			//e.printStackTrace();
			logger.error("Cannot connect to socket " + this.name + ": " + e.getMessage());
			this.connected = false;
		}
	}
	
	public void sendToServer(byte[] req) {
		ByteArrayOutputStream breq = new ByteArrayOutputStream();
		try {
			breq.write(this.calculateHeader(req));
			breq.write(req);
		} catch (IOException e) {
			e.printStackTrace();
		}
		synchronized (this.toServerQueueLock) {
			this.toServerQueue.add(breq);
		}
	}
	
	public void addListener(SocketListener machine) {
		logger.info("Adding machine to socket connection.");
		this.listeners.add(machine);
	}	
	
	@Override
	public void run() {
		logger.info("Starting socket thread " + this.name);
		
		this.connect();

		if (this.writeEnabled) {
			this.socketWriteThread = new SocketWrite(this);
			new Thread(this.socketWriteThread).start();
		}
		
		if (this.readEnabled) {
			this.socketReadThread = new SocketRead(this);
			new Thread(this.socketReadThread).start();
		}
		
		while (true) {
			if (this.socket == null || this.socket.isConnected() == false || this.socket.isClosed() == true) {
				logger.error("Socket disconnected.");
				this.connect();
				try {
					Thread.sleep(this.reconnectInterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				try {
					Thread.sleep(this.checkConnectionInterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void clearSocket() {
		synchronized(this.socketObjectLock) {
			try {
				logger.info("Clearing socket " + this.name + ": ");
				this.outToServer.close();
				this.inFromServer.close();
				this.socket.close();
			} catch (IOException e) {
				logger.error("Cannot close socket " + this.name + ": " + e.getMessage());
			}
			this.socket = null;
			this.inFromServer = null;
			this.outToServer = null;
		}
	}

}
