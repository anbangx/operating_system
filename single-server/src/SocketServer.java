import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
	
	public static final int TIMEOUT_TIME = 1000;
	String hostname;
	int port;
	ServerSocket server;
	boolean running = false;
	
	public SocketServer(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	public void connect() throws IOException {
		server = new ServerSocket(port);
	}
	
	public void run() throws IOException {
		if(server == null)
			return;
		ServerThread serverThread = new ServerThread();
		running = true;
		serverThread.start();
	}
	
	private class ServerThread extends Thread{
		public void run(){
			try {
				Socket cliendSocket = server.accept();
				System.out.println(cliendSocket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				closeSocket();
			}
		}
	}
	
	/**
	 * Stop the ServerSocket
	 */
	public void stop() {
		running = false;
		closeSocket();
	}
	
	private void closeSocket() {
		try {
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
