package driver;

import java.io.BufferedReader;
import java.io.FileReader;

import data.PRManager;

public class OSDriver {

	public static void main(String[] args) throws Exception {
		PRManager prManager = new PRManager();

		String file = "test.txt";
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null) {
			// process the line.
			String[] tokens = line.split(" ");
			String command = tokens[0];
			if (command.equals("init")) {
				prManager.init();
			} else if (command.equals("cr")) {
				prManager.createProcess(tokens[1], Integer.parseInt(tokens[2]));
			} else if(command.equals("de")){ 
				prManager.destroyProcess(tokens[1]);
			} else if(command.equals("to")){ 
				prManager.timeOut();
			} else if(command.equals("req")){
				prManager.requestResource(tokens[1]);
			} else if(command.equals("rel")){
				prManager.releaseResource(tokens[1]);
			} else if(command.equals("rio")){
				prManager.requestIO();
			} else if(command.equals("ioc")){
				prManager.IOCompletion();
			} else if(command.equals("lsp")){
				prManager.listAllProcessesAndStatus();
			}
			prManager.printCurrentRunningProcess();
		}
		br.close();
	}

}
