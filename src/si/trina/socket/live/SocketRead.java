package si.trina.socket.live;

public class SocketRead implements Runnable {

	private SocketConnection socketConnection;

	public SocketRead(SocketConnection socketConnection) {
		System.out.println("Initializing socket read.");
		this.socketConnection = socketConnection;
	}

	@Override
	public void run() {
		while (true) {
			if (this.socketConnection.socket == null || this.socketConnection.socket.isConnected() == false) {
				try {
					Thread.sleep(this.socketConnection.reconnectInterval);
					continue;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
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
							//Thread.sleep(500);
							//this.connect();
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
							//Thread.sleep(500);
							//this.connect();
							continue;
						} else {
							bytesRead += nBytes;
						}
					}
					
					if (numBytes <= 1) {
					} else {
						for (SocketListener m: this.socketConnection.listeners) {
							m.processSocketEvent(this.socketConnection.name, data);
						}
					}
					
				}
				Thread.sleep(100);
			}catch (Exception e) {
				e.printStackTrace();

				/*try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				this.connect();*/
			}
		}
	}
}
