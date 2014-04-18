package driver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

import data.PRManager;

public class OSDriver {

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
		BufferedReader br = new BufferedReader(new FileReader(file));

		PRManager prManager = new PRManager();
		String line;
		while ((line = br.readLine()) != null) {
			if (line.equals("quit")) {
				System.out.println("Process terminated");
				break;
			}
			// process the line.
			prManager.execute(line);
		}
		br.close();
		input.close();
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
