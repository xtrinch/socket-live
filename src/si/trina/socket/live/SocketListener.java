package si.trina.socket.live;

public interface SocketListener {	
	abstract public void processSocketEvent(String name, byte [] data);
}
