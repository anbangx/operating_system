package driver;

import java.util.ArrayList;
import java.util.Random;

import plot.XYLineChart;
import mm.core.MMManager;
import mm.core.MMManager.Strategy;
import mm.core.Result;

public class MMDriver {

	public static Random random = new Random(0);
	public static MMManager mmm = new MMManager();

	public final static int simSteps = 10000;
	public final static int maxMemorySize = 2000; // word-addressable

	public static void main(String[] args) {
		mmm.init(maxMemorySize);

		/**
		 * a: [100, 600] 6 steps d: [40, 200] 5 steps
		 */
		int a;
		int d = 40; // 0.02 * maxMemorySize
		for (; d <= 200; d += 40) {
			XYLineChart.means.clear();
			XYLineChart.stats.clear();
			XYLineChart.deviation = d;
			ArrayList<Result> results1 = new ArrayList<Result>();
			ArrayList<Result> results3 = new ArrayList<Result>();
			for (a = 100; a <= 600; a += 100) {
				XYLineChart.means.add(a);
				results1.add(runSimulator(a, d, Strategy.FIRST_FIT));
				// results2.add(runSimulator(a, d, Strategy.NEXT_FIT));
				results3.add(runSimulator(a, d, Strategy.BEST_FIT));
			}
			XYLineChart.stats.put(Strategy.FIRST_FIT, results1);
			// XYLineChart.stats.put(Strategy.NEXT_FIT, results2);
			XYLineChart.stats.put(Strategy.BEST_FIT, results3);
			XYLineChart.createMemoryUtilChartPanel();
			XYLineChart.createSearchRatioChartPanel();
		}

	}

	private static Result runSimulator(int a, int d, Strategy strategy) {
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
				if (mmm.getTotalHoles() == 0)
					searchRatio = (searchRatio * count + (double) 1)
							/ (count + 1);
				else
					searchRatio = (searchRatio * count + (double) mmm.numHoleExamined
							/ curTotalHoles)
							/ (count + 1);
				count++;
				if (allocatedBlock == -1)
					break;
				allocatedBlocks.add(allocatedBlock);
			}

			// record memory utilization
			int occupiedSize = mmm.getSizeOfOccupiedBlock(allocatedBlocks);
			utilization = (utilization * i + (double) occupiedSize
					/ maxMemorySize)
					/ (i + 1);

			// select block p to be released from 1 to k
			int k = allocatedBlocks.size();
			if (k == 0) {
				continue;
			}
			int p = random.nextInt(k);
			mmm.release(allocatedBlocks.get(p));
			mmm.printEmptyHole();

			allocatedBlocks.remove(p);
			mmm.printOccupiedBlock(allocatedBlocks);
			// System.out.println("Utilization: " + utilization);
			// System.out.println("SearchRatio: " + searchRatio);
			// System.out.println();
		}
		return new Result(utilization, searchRatio);
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
