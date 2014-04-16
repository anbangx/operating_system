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
			if (command.equals("cr")) {
				prManager.createProcess(tokens[1], Integer.parseInt(tokens[2]));
			}
		}
		br.close();
	}

}
