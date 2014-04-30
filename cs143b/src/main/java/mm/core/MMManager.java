package mm.core;

import java.util.ArrayList;

public class MMManager {

	public static final int TAG_SIZE = 1;
	public static final int PREV_INDEX_SIZE = 1;
	public static final int NEXT_INDEX_SIZE = 1;
	public static final int DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE = TAG_SIZE * 2;
	public static final int INTEGER_SIZE = 4;

	public int removeHoleSize = 0;
	private PackableMemory memoryBlock;
	private int totalByteSize;
	private int firstHole;
	private int lastHole;

	public MMManager() {
		memoryBlock = null;
		totalByteSize = 0;
		firstHole = -1;
		lastHole = -1;
	}

	public void init(int totalSize) {
		// 1. create a memory block with a specific size
		totalByteSize = INTEGER_SIZE * totalSize;
		memoryBlock = new PackableMemory(totalByteSize);

		// 2. create and init the hole, then return the block start index
		createAndInitHole(0, false, totalSize
				- DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE, -1, -1);
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
	}

	public int request(int size) {
		// 1. run algorithm to find suitable memory block
		int holeStartIdx = firstFit(size);
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
			memoryBlock.pack(size + remainHoleSize, newHoleEndIdx - INTEGER_SIZE * TAG_SIZE);
			newHoleStartIdx = holeStartIdx;
			
			// remove hole
			if (prevHole != -1)
				setNextHole(prevHole, nextHole);
			if (nextHole != -1)
				setPrevHole(nextHole, prevHole);
			if (holeStartIdx == firstHole){
				firstHole = nextHole;
			}
			if (holeStartIdx == lastHole) {
				lastHole = prevHole;
				setNextHole(lastHole, -1);
			}
//			if (remainHoleSize > 0) {
//				memoryBlock.pack(0, holeStartIdx);
//				memoryBlock.pack(0, newHoleStartIdx - INTEGER_SIZE * TAG_SIZE);
//			}
		} else { // 4.2. otherwise
			memoryBlock.pack(-remainBlockSize, holeStartIdx);
			memoryBlock.pack(-remainBlockSize, newHoleStartIdx - INTEGER_SIZE
					* TAG_SIZE);
		}

		// debug
		boolean debug = true;
		if (debug) {
			System.out.print("Request " + size + " in [" + newHoleStartIdx
					+ "," + newHoleEndIdx + "], ");
			System.out.println("remaining: " + remainBlockSize);
		}

		// return start index of new block
		return newHoleStartIdx + INTEGER_SIZE * TAG_SIZE;
	}

	/**
	 * Algorithm1: First-fit
	 */
	public int firstFit(int requestSize) {
		int curHole = firstHole;
		while (curHole >= 0) {
			if (requestSize < getBlockSize(curHole))
				return curHole;
			curHole = getNextHole(curHole);
		}
		return -1;
	}

	public int release(int blockIdx) {
		// 1. compute left, right if occupied
		int curHoleStart = blockIdx - INTEGER_SIZE * TAG_SIZE;
		int curHoleEnd = getHoleEndFromHoleStart(curHoleStart);
		int blockSize = getBlockSize(curHoleStart);
		int left;
		if(curHoleStart - INTEGER_SIZE * TAG_SIZE < 0)
			left = 1;
		else
			left = memoryBlock.unpack(curHoleStart - INTEGER_SIZE * TAG_SIZE);
		int right;
		if (curHoleEnd + INTEGER_SIZE * TAG_SIZE > totalByteSize)
			right = 1;
		else
			right = memoryBlock.unpack(curHoleEnd);

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
			} else {
				setPrevHole(curHoleStart, lastHole); 
				setNextHole(lastHole, curHoleStart);
				lastHole = curHoleStart;
			}
			setNextHole(lastHole, -1);

			System.out.println("Release " + blockSize + " in [" + curHoleStart
					+ "," + curHoleEnd + "]");
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
			
			if(curHoleEnd == firstHole)
				firstHole = curHoleStart;
			if(curHoleEnd == lastHole)
				lastHole = curHoleStart;

			System.out.println("Release " + blockSize + " in [" + curHoleStart
					+ "," + rightHoleEnd + "]");
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

			System.out.println("Release " + blockSize + " in [" + leftHoleStart
					+ "," + curHoleEnd + "]");
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

			System.out.println("Release " + blockSize + " in [" + leftHoleStart
					+ "," + rightHoleEnd + "]");
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
		if (curHole == firstHole){
			firstHole = nextHole;
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
	
	public void printEmptyHole(){
		StringBuilder sb = new StringBuilder();
		int startHole = firstHole;
		while (startHole >= 0) {
			int endHole = getHoleEndFromHoleStart(startHole);
			sb.append("[" + startHole + "," + endHole + ";" + getBlockSize(startHole) +"] ");
			startHole = getNextHole(startHole);
		}
		System.out.println("Holes: " + sb.toString());
	}
	
	public void printOccupiedBlock(ArrayList<Integer> allocatedBlocks){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < allocatedBlocks.size(); i++){
			int blockIdx = allocatedBlocks.get(i);
			int startHole = blockIdx - INTEGER_SIZE * TAG_SIZE;
			int endHole = getHoleEndFromHoleStart(startHole);
			sb.append("[" + startHole + "," + endHole + ";" + getBlockSize(startHole) +"] ");
		}
		System.out.println("OccupiedBlocks: " + sb.toString());
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
		for (int i = 0; i < n; i++) {
			int startIdx = mmm.request(10);
			if (startIdx == -1)
				System.err.println("Insufficient memory");
			else
				System.out.println("Req succeeds, and the start index is: "
						+ startIdx);
		}
		System.out.println();
	}

	public static void test1() {
		System.out.println("test1");
		MMManager mmm = new MMManager();
		mmm.init(100);

		int A = mmm.request(10);
		System.out.println("Req A succeeds, and the start index is: " + A);
		int B = mmm.request(10);
		System.out.println("Req B succeeds, and the start index is: " + B);
		int C = mmm.request(10);
		System.out.println("Req C succeeds, and the start index is: " + C);
		int D = mmm.request(10);
		System.out.println("Req D succeeds, and the start index is: " + D);
		int B2 = mmm.release(B);
		System.out.println("Rel B succeeds, and the start index is: " + B2);
		System.out.println();
	}

	public static void test2() {
		System.out.println("test2");
		MMManager mmm = new MMManager();
		mmm.init(100);

		int A = mmm.request(10);
		System.out.println("Req A succeeds, and the start index is: " + A);
		int B = mmm.request(10);
		System.out.println("Req B succeeds, and the start index is: " + B);
		int C = mmm.request(10);
		System.out.println("Req C succeeds, and the start index is: " + C);
		int D = mmm.request(10);
		System.out.println("Req D succeeds, and the start index is: " + D);
		int A2 = mmm.release(A);
		System.out.println("Rel A succeeds, and the start index is: " + A2);
		A2 = mmm.release(B);
		System.out.println("Rel B succeeds, and the start index is: " + A2);
		System.out.println();
	}

	public static void test3() {
		System.out.println("test3");
		MMManager mmm = new MMManager();
		mmm.init(100);

		int A = mmm.request(10);
		System.out.println("Req A succeeds, and the start index is: " + A);
		int B = mmm.request(10);
		System.out.println("Req B succeeds, and the start index is: " + B);
		int C = mmm.request(10);
		System.out.println("Req C succeeds, and the start index is: " + C);
		int D = mmm.request(10);
		System.out.println("Req D succeeds, and the start index is: " + D);
		int C2 = mmm.release(C);
		System.out.println("Rel C succeeds, and the start index is: " + C2);
		C2 = mmm.release(B);
		System.out.println("Rel B succeeds, and the start index is: " + C2);
		System.out.println();
	}

	public static void test4() {
		System.out.println("test4");
		MMManager mmm = new MMManager();
		mmm.init(100);

		int A = mmm.request(10);
		System.out.println("Req A succeeds, and the start index is: " + A);
		int B = mmm.request(10);
		System.out.println("Req B succeeds, and the start index is: " + B);
		int C = mmm.request(10);
		System.out.println("Req C succeeds, and the start index is: " + C);
		int D = mmm.request(10);
		System.out.println("Req D succeeds, and the start index is: " + D);
		int A2 = mmm.release(A);
		System.out.println("Rel A succeeds, and the start index is: " + A2);
		int C2 = mmm.release(C);
		System.out.println("Rel C succeeds, and the start index is: " + C2);
		A2 = mmm.release(B);
		System.out.println("Rel B succeeds, and the start index is: " + A2);
		System.out.println();
	}
}
