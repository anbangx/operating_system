package driver;

import java.util.ArrayList;
import java.util.Random;

import mm.core.MMManager;

public class MMDriver {

	public static Random random = new Random(1000);

	public static void main(String[] args) {
		int totalWordSize = 1000;
		MMManager mmm = new MMManager();
		mmm.init(totalWordSize);
		
		double utilization = 0;
		double search = 0;
		int count = 0;
		
		int a = 3 / 10 * totalWordSize;
		int d = 200;
		int steps = 1000;
		ArrayList<Integer> allocatedBlocks = new ArrayList<Integer>();
		// run simulator with strategy1
		for (int i = 0; i < steps; i++) {
			System.out.println("Step " + i + ":");
			// get size n of next request
			int size = 0;
			Integer allocatedBlock;
			while (true) {
				size = generateNextSize(a, d, totalWordSize);
				allocatedBlock = mmm.request(size);
				if(mmm.getTotalHoles() == 0)
					search = (search * count + 1) / (count + 1);
				else
					search = (search * count + (double)mmm.numHoleExamined/mmm.getTotalHoles()) / (count + 1);
				count++;
				if (allocatedBlock == -1)
					break;
				allocatedBlocks.add(allocatedBlock);
			}

			// record memory utilization
			int occupiedSize = mmm.getSizeOfOccupiedBlock(allocatedBlocks);
			utilization = (utilization * i + (double)occupiedSize/totalWordSize) / (i + 1);
			
			// select block p to be released from 1 to k
			int k = allocatedBlocks.size();
			if (k == 0){
				continue;
			}
			int p = random.nextInt(k);
			mmm.release(allocatedBlocks.get(p));
			mmm.printEmptyHole();

			allocatedBlocks.remove(p);
			mmm.printOccupiedBlock(allocatedBlocks);
			System.out.println("Utilization: " + utilization);
			System.out.println("Search: " + search);
			System.out.println();
		}
	}

	private static int generateNextSize(int a, int d, int totalSize) {
		int size = (int) getGaussian(a, d);
		while (size < 2 || size > totalSize) {
			size = (int) getGaussian(a, d);
		}
		return size;
	}

	private static double getGaussian(double mean, double variance) {
		return mean + random.nextGaussian() * variance;
	}
}
