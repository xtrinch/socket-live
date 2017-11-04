package si.trina.socket.live;

import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketRead implements Runnable {

	final Logger logger = LoggerFactory.getLogger(SocketRead.class);

	private SocketConnection socketConnection;

	public SocketRead(SocketConnection socketConnection) {
		System.out.println("Initializing socket read.");
		this.socketConnection = socketConnection;
	}

	@Override
	public void run() {
		while (true) {
			if (this.socketConnection.socket == null 
					|| this.socketConnection.socket.isConnected() == false 
					|| this.socketConnection.socket.isClosed() == true) {
				try {
					Thread.sleep(this.socketConnection.reconnectInterval);
					continue;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			try {
				synchronized (this.socketConnection.fromServerQueueLock) {
					byte[] header = new byte[4];
					int bytesToRead=4;
					int bytesRead=0;
					while(bytesRead < bytesToRead) {
						int nBytes = this.socketConnection.inFromServer.read(header, bytesRead, bytesToRead-bytesRead);
						if (nBytes < 0) {
							continue;
						} else {
							bytesRead += nBytes;
						}
					}
					
					int numBytes = this.socketConnection.calculateDatagramLength(header);
					
					byte[] data = new byte[numBytes];
					bytesRead=0;
					bytesToRead = numBytes;
					while(bytesRead < bytesToRead) {
						int nBytes = this.socketConnection.inFromServer.read(data, bytesRead, bytesToRead-bytesRead);
						if (nBytes < 0) {
							continue;
						} else {
							bytesRead += nBytes;
						}
					}
					
					if (numBytes <= 1) {
					} else {
						for (SocketListener m: this.socketConnection.listeners) {
							m.processSocketEvent(data, this.socketConnection);
						}
					}
					
				}
			}catch (Exception e) {
				if (e instanceof SocketException) {
					// force reconnect
					logger.error("Socket disconnect triggered from read thread.");
					this.socketConnection.clearSocket();
				}
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
}
