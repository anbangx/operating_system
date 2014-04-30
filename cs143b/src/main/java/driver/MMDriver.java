package driver;

import java.util.ArrayList;
import java.util.Random;

import mm.core.MMManager;

public class MMDriver {

	public static Random random = new Random(139060);

	public static void main(String[] args) {
		int totalWordSize = 50;
		MMManager mmm = new MMManager();
		mmm.init(totalWordSize);

		int a = 1 / 10 * totalWordSize;
		int d = 10;
		int steps = 10000;
		ArrayList<Integer> allocatedBlocks = new ArrayList<Integer>();
		// run simulator with strategy1
		for (int i = 0; i < steps; i++) {
			System.out.println(i);
			// get size n of next request
			int size = 0;
			Integer allocatedBlock;
			while (true) {
				size = generateNextSize(a, d, totalWordSize);
				allocatedBlock = mmm.request(size);
				if(allocatedBlock == -1)
					break;
				allocatedBlocks.add(allocatedBlock);
			}

			// record memory utilization

			// select block p to be released from 1 to k
			int k = allocatedBlocks.size();
			int p = random.nextInt(k);
			mmm.release(allocatedBlocks.get(p));
			allocatedBlocks.remove(p);
		}
	}
	
	private static int generateNextSize(int a, int d, int totalSize){
		int size = (int) getGaussian(a, d);
		while(size < 2 || size > totalSize){
			size = (int) getGaussian(a, d);
		}
		return size;
	}
	
	private static double getGaussian(double mean, double variance) {
		return mean + random.nextGaussian() * variance;
	}
}
