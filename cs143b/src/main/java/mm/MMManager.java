package mm;

import java.util.ArrayList;

public class MMManager {
	
	public enum Strategy {
		FIRST_FIT, NEXT_FIT, BEST_FIT
	}
	
	public static final int TAG_SIZE = 1;
	public static final int PREV_INDEX_SIZE = 1;
	public static final int NEXT_INDEX_SIZE = 1;
	public static final int DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE = TAG_SIZE * 2;
	public static final int INTEGER_SIZE = 4;

	private PackableMemory memoryBlock;
	private int totalByteSize;
	private int firstHole;
	private int lastHole;
	private int resumeHole;
	public int numHoleExamined = 0;

	public MMManager() {
		memoryBlock = null;
		totalByteSize = 0;
		firstHole = -1;
		lastHole = -1;
		resumeHole = -1;
	}

	public void init(int totalSize) {
		// 1. create a memory block with a specific size
		totalByteSize = INTEGER_SIZE * totalSize;
		memoryBlock = new PackableMemory(totalByteSize);

		// 2. create and init the hole, then return the block start index
		createAndInitHole(0, false, totalSize
				- DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE, -1, -1);

		System.out.println("Init " + totalSize + " in [0," + totalByteSize
				+ "]");
	}
	
	public void reset(int totalSize){
		memoryBlock = null;
		totalByteSize = 0;
		firstHole = -1;
		lastHole = -1;
		resumeHole = -1;
		init(totalSize);
	}

	public void createAndInitHole(int holeStartIdx, boolean occupied,
			int blockSize, int prev, int next) {
		int tag = occupied ? blockSize : -blockSize;

		// 1. set the tag
		// left tag
		memoryBlock.pack(tag, holeStartIdx);
		// right tag
		int rightTagOffset = INTEGER_SIZE * (TAG_SIZE + blockSize);
		memoryBlock.pack(tag, holeStartIdx + rightTagOffset);

		// 2. set reference
		// prev
		memoryBlock.pack(prev, holeStartIdx + INTEGER_SIZE * TAG_SIZE);
		// next
		memoryBlock.pack(next, holeStartIdx + INTEGER_SIZE
				* (TAG_SIZE + PREV_INDEX_SIZE));

		// 3. set firstHole/lastHole
		firstHole = 0;
		lastHole = 0;
		resumeHole = 0;
	}
	
	/**
	 * The default strategy is FIRST_FIT
	 */
	public int request(int size){
		return request(size, Strategy.FIRST_FIT);
	}
	
	public int request(int size, Strategy strategy) {
		// 1. run algorithm to find suitable memory block
		int holeStartIdx = -1;
		switch(strategy){
			case FIRST_FIT:
				holeStartIdx = firstFit(size);
				break;
			case NEXT_FIT:
				holeStartIdx = nextFit(size);
				break;
			case BEST_FIT:
				holeStartIdx = bestFit(size);
				break;
			default:
				break;
		}
		
		if (holeStartIdx == -1) {
			System.out.println("Request " + size + ", but insufficient memory");
			return -1;
		}

		// keep track of prev/next hole for future update
		int prevHole = getPrevHole(holeStartIdx);
		int nextHole = getNextHole(holeStartIdx);

		// if has sufficient memory, create a memory with input size
		// 2. compute the startIdx of new hole
		int newHoleEndIdx = getHoleEndFromHoleStart(holeStartIdx);
		int newHoleStartIdx = newHoleEndIdx - INTEGER_SIZE
				* (getHoleSizeFromBlockSize(size));

		// 3. init the new block - blockSize
		memoryBlock.pack(size, newHoleStartIdx);
		memoryBlock.pack(size, newHoleEndIdx - INTEGER_SIZE * TAG_SIZE);

		// 4. update the old hole
		int remainHoleSize = (newHoleStartIdx - holeStartIdx) / 4;
		int remainBlockSize = remainHoleSize
				- DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE;
		// 4.1. if hole become too small
		if (remainBlockSize < 2) {
			memoryBlock.pack(size + remainHoleSize, holeStartIdx);
			memoryBlock.pack(size + remainHoleSize, newHoleEndIdx
					- INTEGER_SIZE * TAG_SIZE);
			newHoleStartIdx = holeStartIdx;

			// remove hole
			if (prevHole != -1)
				setNextHole(prevHole, nextHole);
			if (nextHole != -1)
				setPrevHole(nextHole, prevHole);
			if (holeStartIdx == firstHole) {
				firstHole = nextHole;
				resumeHole = firstHole;
			}
			if (holeStartIdx == lastHole) {
				lastHole = prevHole;
				setNextHole(lastHole, -1);
			}
		} else { // 4.2. otherwise
			memoryBlock.pack(-remainBlockSize, holeStartIdx);
			memoryBlock.pack(-remainBlockSize, newHoleStartIdx - INTEGER_SIZE
					* TAG_SIZE);
		}

		// debug
		boolean debug = false;
		if (debug) {
			System.out.print("Request " + size + " in [" + newHoleStartIdx
					+ "," + newHoleEndIdx + "], ");
			System.out.println("and the block start index is: "
					+ (newHoleStartIdx + INTEGER_SIZE * TAG_SIZE));
		}

		// return start index of new block
		return newHoleStartIdx + INTEGER_SIZE * TAG_SIZE;
	}

	/**
	 * Algorithm1: First-fit
	 */
	public int firstFit(int requestSize) {
		numHoleExamined = 0;
		int curHole = firstHole;
		while (curHole >= 0) {
			numHoleExamined++;
			if (requestSize < getBlockSize(curHole))
				return curHole;
			curHole = getNextHole(curHole);
		}
		return -1;
	}
	
	/**
	 * Algorithm2: Next-fit
	 */
	public int nextFit(int requestSize) {
		numHoleExamined = 0;
		int curHole = resumeHole;
		if(curHole == -1)	return -1;
		while (curHole >= 0) {
			numHoleExamined++;
			if (requestSize < getBlockSize(curHole)){
				resumeHole = curHole;
				return curHole;
			}
			curHole = getNextHole(curHole);
		}
		resumeHole = firstHole;
		return -1;
	}
	
	/**
	 * Algorithm3: Best-fit
	 */
	public int bestFit(int requestSize) {
		numHoleExamined = 0;
		int curHole = firstHole;
		int minDiff = Integer.MAX_VALUE;
		int returnHole = -1;

		while (curHole >= 0) {
			int blockSize = getBlockSize(curHole);
			numHoleExamined++;
			if (blockSize >= requestSize && (blockSize - requestSize) < minDiff) {
				minDiff = blockSize - requestSize;
				returnHole = curHole;
			}
			curHole = getNextHole(curHole);
		}
		return returnHole >= 0 ? returnHole : -1;
	}

	public int getTotalHoles() {
		int total = 0;
		int startHole = firstHole;
		while (startHole >= 0) {
			total++;
			startHole = getNextHole(startHole);
		}
		return total;
	}

	public int release(int blockIdx) {
		// 1. compute left, right if occupied
		int curHoleStart = blockIdx - INTEGER_SIZE * TAG_SIZE;
		int curHoleEnd = getHoleEndFromHoleStart(curHoleStart);
		int blockSize = getBlockSize(curHoleStart);
		int left;
		if (curHoleStart - INTEGER_SIZE * TAG_SIZE < 0)
			left = 1;
		else
			left = memoryBlock.unpack(curHoleStart - INTEGER_SIZE * TAG_SIZE);
		int right;
		if (curHoleEnd + INTEGER_SIZE * TAG_SIZE > totalByteSize)
			right = 1;
		else
			right = memoryBlock.unpack(curHoleEnd);
		
		boolean debug = false;
		// 2. check
		if (left >= 0 && right >= 0) {
			// 2.1. both are occupied
			// (1). update tag
			memoryBlock.pack(-blockSize, curHoleStart);
			memoryBlock.pack(-blockSize, curHoleEnd - INTEGER_SIZE * TAG_SIZE);
			// (2). add it to the lastHole
			if (lastHole == -1) {
				setPrevHole(curHoleStart, -1);
				setNextHole(curHoleStart, -1);
				lastHole = curHoleStart;
				firstHole = lastHole;
				resumeHole = lastHole;
			} else {
				setPrevHole(curHoleStart, lastHole);
				setNextHole(lastHole, curHoleStart);
				lastHole = curHoleStart;
			}
			setNextHole(lastHole, -1);
			
			if(debug){
				System.out.println("Release " + blockSize + " in [" + curHoleStart
					+ "," + curHoleEnd + "]");
			}
			
			return curHoleStart;
		} else if (left >= 0 && right < 0) {
			// 2.2. left is occupid but right is not - merge with right hole
			// rightHoleStart == curHoleEnd
			// (1). update tag - size
			int newBlockSize = blockSize + Math.abs(right) + 2 * TAG_SIZE;
			memoryBlock.pack(-newBlockSize, curHoleStart);
			int rightHoleEnd = getHoleEndFromHoleStart(curHoleEnd);
			memoryBlock.pack(-newBlockSize, rightHoleEnd - INTEGER_SIZE
					* TAG_SIZE);
			// (2). move right hole reference to cur
			// update reference from cur perspective
			setPrevHole(curHoleStart, getPrevHole(curHoleEnd));
			setNextHole(curHoleStart, getNextHole(curHoleEnd));
			// update reference from prev/next perspective
			setNextHole(getPrevHole(curHoleEnd), curHoleStart);
			setPrevHole(getNextHole(curHoleEnd), curHoleStart);

			if (curHoleEnd == firstHole){
				firstHole = curHoleStart;
				resumeHole = firstHole;
			}
			if (curHoleEnd == lastHole)
				lastHole = curHoleStart;
			
			if(debug){
				System.out.println("Release " + blockSize + " in [" + curHoleStart
						+ "," + rightHoleEnd + "]");
			}
			
			return curHoleStart;
		} else if (left < 0 && right >= 0) {
			// 2.3. left is not occupid but right is - merge with left hole
			// leftHoleEnd == curHoleStart
			// (1). update tag - size
			int newBlockSize = blockSize + Math.abs(left) + 2 * TAG_SIZE;
			int leftHoleStart = getHoleStartFromHoleEnd(curHoleStart);
			memoryBlock.pack(-newBlockSize, leftHoleStart);
			memoryBlock.pack(-newBlockSize, curHoleEnd - INTEGER_SIZE
					* TAG_SIZE);
			
			if(debug){
				System.out.println("Release " + blockSize + " in [" + leftHoleStart
						+ "," + curHoleEnd + "]");
			}
			
			return leftHoleStart;
		} else {
			// 2.4. both are not occupied - merge with left and right holes
			// (1). update tag - size
			int newBlockSize = blockSize + Math.abs(left) + Math.abs(right) + 4
					* TAG_SIZE;
			int leftHoleStart = getHoleStartFromHoleEnd(curHoleStart);
			int rightHoleEnd = getHoleEndFromHoleStart(curHoleEnd);
			memoryBlock.pack(-newBlockSize, leftHoleStart);
			memoryBlock.pack(-newBlockSize, rightHoleEnd - INTEGER_SIZE
					* TAG_SIZE);
			// (2). remove right hole
			removeHole(curHoleEnd);
			
			if(debug){
				System.out.println("Release " + blockSize + " in [" + leftHoleStart
						+ "," + rightHoleEnd + "]");
			}
			return leftHoleStart;
		}
	}

	public void removeHole(int curHole) {
		int prevHole = getPrevHole(curHole);
		int nextHole = getNextHole(curHole);
		if (prevHole != -1)
			setNextHole(prevHole, nextHole);
		if (nextHole != -1)
			setPrevHole(nextHole, prevHole);
		if (curHole == firstHole) {
			firstHole = nextHole;
			resumeHole = firstHole;
		}
		if (curHole == lastHole) {
			lastHole = prevHole;
			setNextHole(lastHole, -1);
		}
	}

	public int getBlockSize(int startIdx) {
		return Math.abs(memoryBlock.unpack(startIdx));
	}

	public int getHoleStartFromHoleEnd(int endIdx) {
		int blockSize = Math.abs(memoryBlock.unpack(endIdx - INTEGER_SIZE
				* TAG_SIZE));
		return endIdx - INTEGER_SIZE
				* (DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE + blockSize);
	}

	public int getHoleEndFromHoleStart(int startIdx) {
		int blockSize = getBlockSize(startIdx);
		return startIdx + INTEGER_SIZE
				* (blockSize + DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE);
	}

	public int getHoleSizeFromBlockSize(int blockSize) {
		return blockSize + DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE;
	}

	public int getPrevHole(int curHole) {
		return memoryBlock.unpack(curHole + INTEGER_SIZE * TAG_SIZE);
	}

	public int getNextHole(int curHole) {
		return memoryBlock.unpack(curHole + INTEGER_SIZE
				* (TAG_SIZE + PREV_INDEX_SIZE));
	}

	public void setPrevHole(int curHole, int prevHole) {
		if (curHole < 0)
			return;
		int curPreReferIdx = curHole + INTEGER_SIZE * TAG_SIZE;
		memoryBlock.pack(prevHole, curPreReferIdx);
	}

	public void setNextHole(int curHole, int nextHole) {
		if (curHole < 0)
			return;
		int curNextReferIdx = curHole + INTEGER_SIZE
				* (TAG_SIZE + PREV_INDEX_SIZE);
		memoryBlock.pack(nextHole, curNextReferIdx);
	}

	public int getBlockStartIdx(int startIdx) {
		return startIdx + INTEGER_SIZE
				* (TAG_SIZE + PREV_INDEX_SIZE + NEXT_INDEX_SIZE);
	}

	public void printEmptyHole() {
		StringBuilder sb = new StringBuilder();
		int startHole = firstHole;
		while (startHole >= 0) {
			int endHole = getHoleEndFromHoleStart(startHole);
			sb.append("[" + startHole + "," + endHole + ";"
					+ getBlockSize(startHole) + "] ");
			startHole = getNextHole(startHole);
		}
		System.out.println("Holes: " + sb.toString());
	}

	public void printOccupiedBlock(ArrayList<Integer> allocatedBlocks) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < allocatedBlocks.size(); i++) {
			int blockIdx = allocatedBlocks.get(i);
			int startHole = blockIdx - INTEGER_SIZE * TAG_SIZE;
			int endHole = getHoleEndFromHoleStart(startHole);
			sb.append("[" + startHole + "," + endHole + ";"
					+ getBlockSize(startHole) + "] ");
		}
		System.out.println("OccupiedBlocks: " + sb.toString());
	}

	public int getSizeOfOccupiedBlock(ArrayList<Integer> allocatedBlocks) {
		int totalSize = 0;
		for (int i = 0; i < allocatedBlocks.size(); i++) {
			int blockIdx = allocatedBlocks.get(i);
			int startHole = blockIdx - INTEGER_SIZE * TAG_SIZE;
			totalSize += getBlockSize(startHole);
		}
		return totalSize;
	}

	public static void main(String[] args) {
		test();
		test1();
		test2();
		test3();
		test4();
	}

	public static void test() {
		System.out.println("test");
		MMManager mmm = new MMManager();
		mmm.init(100);

		int n = 10;
		for (int i = 0; i < n; i++)
			mmm.request(10);
		System.out.println();
	}

	public static void test1() {
		System.out.println("test1");
		MMManager mmm = new MMManager();
		mmm.init(100);

		mmm.request(10);
		int B = mmm.request(10);
		mmm.request(10);
		mmm.request(10);
		mmm.release(B);
		System.out.println();
	}

	public static void test2() {
		System.out.println("test2");
		MMManager mmm = new MMManager();
		mmm.init(100);

		int A = mmm.request(10);
		int B = mmm.request(10);
		mmm.request(10);
		mmm.request(10);
		mmm.release(A);
		mmm.release(B);
		System.out.println();
	}

	public static void test3() {
		System.out.println("test3");
		MMManager mmm = new MMManager();
		mmm.init(100);

		mmm.request(10);
		int B = mmm.request(10);
		int C = mmm.request(10);
		mmm.request(10);
		mmm.release(C);
		mmm.release(B);
		System.out.println();
	}

	public static void test4() {
		System.out.println("test4");
		MMManager mmm = new MMManager();
		mmm.init(100);

		int A = mmm.request(10);
		int B = mmm.request(10);
		int C = mmm.request(10);
		mmm.request(10);
		mmm.release(A);
		mmm.release(C);
		mmm.release(B);
		System.out.println();
	}
}
