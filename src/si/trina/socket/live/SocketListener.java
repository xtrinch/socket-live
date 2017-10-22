package si.trina.socket.live;

public interface SocketListener {	
	abstract public void processSocketEvent(byte [] data, SocketConnection connection);
}
