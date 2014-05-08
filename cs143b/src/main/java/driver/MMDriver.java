package driver;

import java.util.ArrayList;
import java.util.Random;

import mm.core.MMManager;
import mm.core.MMManager.Strategy;

public class MMDriver {
	
	public static Random random = new Random(1000);
	public static MMManager mmm = new MMManager();
	
	public final static int simSteps = 10;
	public final static int maxMemorySize = 2000; // word-addressable
			
	public static void main(String[] args) {
		mmm.init(maxMemorySize);
		
		/**
		 * a: [100, 600] 6 steps
		 * d: [40, 200] 5 steps
		 */
		int a = 100; 
		int d = 40; // 0.02 * maxMemorySize
		for(; d <= 200; d += 40){
			for(; a <= 600; a += 100){
				runSimulator(a, d, Strategy.FIRST_FIT);
				runSimulator(a, d, Strategy.BEST_FIT);
			}
		}
		
	}
	
	private static void runSimulator(int a, int d, Strategy strategy){
		mmm.reset(maxMemorySize);
		double utilization = 0;
		double searchRatio = 0;
		int count = 0;
		ArrayList<Integer> allocatedBlocks = new ArrayList<Integer>();
		// run simulator with strategy1
		for (int i = 0; i < simSteps; i++) {
			System.out.println("Step " + i + "(" + strategy + "):");
			// get size n of next request
			int size = 0;
			Integer allocatedBlock;
			while (true) {
				size = generateNextSize(a, d, maxMemorySize);
				int curTotalHoles = mmm.getTotalHoles();
				allocatedBlock = mmm.request(size, strategy);
				if(mmm.getTotalHoles() == 0)
					searchRatio = (searchRatio * count + (double)1) / (count + 1);
				else
					searchRatio = (searchRatio * count + (double)mmm.numHoleExamined/curTotalHoles) / (count + 1);
				count++;
				if (allocatedBlock == -1)
					break;
				allocatedBlocks.add(allocatedBlock);
			}

			// record memory utilization
			int occupiedSize = mmm.getSizeOfOccupiedBlock(allocatedBlocks);
			utilization = (utilization * i + (double)occupiedSize/maxMemorySize) / (i + 1);
			
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
			System.out.println("SearchRatio: " + searchRatio);
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
