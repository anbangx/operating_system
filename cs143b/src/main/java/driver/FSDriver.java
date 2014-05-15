package driver;

import java.util.Scanner;

import fs.FileSystem;

public class FSDriver {

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

	public static void executeFromInputFile() {

	}

	public static void executeInCommandLine() throws Exception {
		Scanner input = new Scanner(System.in);
		
		FileSystem fs = new FileSystem();
		
		while (true) {
			System.out.println("Shell> ");
			String line = input.nextLine();
			if (line.equals("quit")) {
				System.out.println("Terminated");
				break;
			}
			// process the line.
			String output = fs.execute(line);
			System.out.println(output);
		}
		input.close();
	}
	
}
