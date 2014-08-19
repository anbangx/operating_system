import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;


public class KVClient {
	
	private String server;
	private int port;
	
	public KVClient(String server, int port){
		this.server = server;
		this.port = port;
	}
	
	private Socket connectHost(){
		Socket socket = null;
		try {
			socket = new Socket(server, port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return socket;
	}
	
	private void closeHost(Socket socket){
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private KVMessage sendMessageAndRecieveResponse(KVMessage mess) throws KVException {
		Socket socket = connectHost();
		mess.sendMessage(socket);
		try {
			KVMessage resp = new KVMessage(socket.getInputStream());
			return resp;
		} catch (IOException e) {
			throw new KVException(new KVMessage(KVMessage.TYPE_RESP, KVMessage.NETWORK_RECIEVE_ERROR));
		} finally{
			closeHost(socket);
		}
	}
	
	public boolean put(String key, String value) throws KVException {
		// 1. check key and value
		if(key == null || value == null)
			throw new KVException(new KVMessage(KVMessage.TYPE_RESP, KVMessage.UNKNOWN_ERROR + "null input"));
		
		// 2. put key/value to message
		KVMessage mess = new KVMessage(KVMessage.REQ_PUT);
		mess.setKey(key);
		mess.setValue(value);
		
		// 3. send and get response by socket
		sendMessageAndRecieveResponse(mess);
		
		// 4. check response
		// 4.1. if null, throw exception, 
		// 4.2. otherwise, return true
	    return true;
	}
}
