package driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

	public static void executeFromInputFile() throws Exception {
		Scanner input = new Scanner(System.in);
		System.out.println("Provide the input path: ");
		String file = input.nextLine();
		File f = new File(file);
		String absolutePath = f.getAbsolutePath();
		String parent = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		File out = new File(parent + "/sid.txt");
		FileWriter fw = new FileWriter(out.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		
		// if file doesnt exists, then create it
		if (!out.exists()) {
			out.createNewFile();
		}
		
		FileSystem fs = new FileSystem();
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println("---------------");
			// process the line.
			bw.write(fs.execute(line) + "\n");
		}
		br.close();
		input.close();
		bw.close();
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
