package si.trina.socket.live;

import java.io.IOException;

public class SocketWrite implements Runnable {

	private SocketConnection socketConnection;
	
	public SocketWrite(SocketConnection readthread) {
		System.out.println("Initializing socket write.");
		this.socketConnection = readthread;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true) {
			if (this.socketConnection.socket == null || this.socketConnection.socket.isConnected() == false) {
				try {
					Thread.sleep(this.socketConnection.reconnectInterval);
					continue;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			synchronized (this.socketConnection.toServerQueueLock) {
				if (this.socketConnection.toServerQueue.size() > 0) {
					
					// Send pending requests
					synchronized (this.socketConnection.writerLock) {
						try {
							//System.out.println(Logger.getTimeStamp() +"Sending data to server in microtec write connection.");
							byte[] qq = this.socketConnection.toServerQueue.pollFirst().toByteArray();
							this.socketConnection.outToServer.write(qq);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					//System.out.println(Logger.getTimeStamp() +"Queue is empty. Not sending anything.");
				}
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
