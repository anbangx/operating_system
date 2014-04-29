package driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

import pr.core.PRManager;

public class PRDriver {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		Scanner input = new Scanner(System.in);
		System.out.println("1. Input from a file ");
		System.out.println("2. Enter command in interactive shell");
		System.out.println("Enter 1 or 2:");

		int choice = input.nextInt();
		switch (choice) {
		case 1:
			executeFromInputFile();
			break;
		case 2:
			executeInCommandLine();
			break;
		default:
			throw new Exception("Choose wrong option!");
		}

		input.close();
	}

	public static void executeFromInputFile() throws Exception {
		Scanner input = new Scanner(System.in);
		System.out.println("Provide the input path: ");
		String file = input.nextLine();
		File f = new File(file);
		String absolutePath = f.getAbsolutePath();
		String parent = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		File out = new File(parent + "/35086995.txt");
		FileWriter fw = new FileWriter(out.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		
		// if file doesnt exists, then create it
		if (!out.exists()) {
			out.createNewFile();
		}
		
		PRManager prManager = new PRManager();
		bw.write("Init is running\n");
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println("---------------");
			System.out.println("input: " + line);
			if (line.equals("quit")) {
				System.out.println("Process terminated");
				bw.write("Process terminated\n");
				break;
			}
			// process the line.
			bw.write(prManager.execute(line) + "\n");
		}
		br.close();
		input.close();
		bw.close();
	}

	public static void executeInCommandLine() throws Exception {
		Scanner input = new Scanner(System.in);

		PRManager prManager = new PRManager();

		while (true) {
			System.out.println("Shell> ");
			String line = input.nextLine();
			if (line.equals("quit")) {
				System.out.println("Process terminated");
				break;
			}
			// process the line.
			prManager.execute(line);
		}
		input.close();
	}
}
