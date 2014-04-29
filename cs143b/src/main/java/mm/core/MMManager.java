package mm.core;

public class MMManager {

	public static final int TAG_SIZE = 1;
	public static final int PREV_INDEX_SIZE = 1;
	public static final int NEXT_INDEX_SIZE = 1;
	public static final int DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE = TAG_SIZE * 2;
	public static final int INTEGER_SIZE = 4;

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
		memoryBlock.pack(next, holeStartIdx + INTEGER_SIZE * (TAG_SIZE + PREV_INDEX_SIZE));
		
		// 3. set firstHole
		firstHole = 0;
	}

	public int request(int size) {
		// 1. run algorithm to find suitable memory block
		int holeStartIdx = firstFit(size);
		if (holeStartIdx == -1) {
			System.err.println("Insufficient memory");
			return -1;
		}

		// keep track of prev/next hole for future update
		int prevHole = getPrevHole(holeStartIdx);
		int nextHole = getNextHole(holeStartIdx);

		int blockSize = getBlockSize(holeStartIdx);
		int holeSize = getHoleSizeFromBlockSize(blockSize);
		// if has sufficient memory, create a memory with input size
		// 2. compute the startIdx of new hole
		int newHoleEndIdx = getHoleEndFromHoleStart(holeStartIdx);
		int newHoleStartIdx = newHoleEndIdx - INTEGER_SIZE * (getHoleSizeFromBlockSize(size));

		// 3. init the new block - blockSize
		memoryBlock.pack(size, newHoleStartIdx);
		memoryBlock.pack(size, newHoleEndIdx - INTEGER_SIZE * TAG_SIZE);
		
		// 4. update the old hole
		int newHoleSize = newHoleEndIdx - newHoleStartIdx;
		int remainHoleSize = holeSize - newHoleSize/4;
		int remainBlockSize = remainHoleSize
				- DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE;
		// 4.1. if hole become too small
		if (remainBlockSize <= 1) {
			setNextHole(prevHole, nextHole);
			setPrevHole(nextHole, prevHole);
		} else { // 4.2. otherwise
			memoryBlock.pack(-remainBlockSize, holeStartIdx);
			memoryBlock.pack(-remainBlockSize, newHoleStartIdx - INTEGER_SIZE * TAG_SIZE);
		}

		// return start index of new block
		return newHoleStartIdx + INTEGER_SIZE * TAG_SIZE;
	}

	/**
	 * Algorithm1: First-fit
	 */
	public int firstFit(int requestSize) {
		int curHole = firstHole;
		while (curHole != -1) {
			if (requestSize < getBlockSize(curHole))
				return curHole;
			curHole = getNextHole(curHole);
		}
		return -1;
	}
	
	public void release(int blockIdx){
		// 1. compute left, right if occupied
		int curHoleStart = blockIdx - INTEGER_SIZE * TAG_SIZE;
		int curHoleEnd = getHoleEndFromHoleStart(curHoleStart);
		int blockSize = getBlockSize(curHoleStart);
		int left = memoryBlock.unpack(curHoleStart - INTEGER_SIZE * TAG_SIZE);
		int right;
		if(curHoleEnd + INTEGER_SIZE * TAG_SIZE > totalByteSize)
			right = 1;
		else
			right = memoryBlock.unpack(curHoleEnd + INTEGER_SIZE * TAG_SIZE);
		
		// 2. check
		if(left > 0 && right > 0){	
			// 2.1. both are occupied
			// (1). update tag
			memoryBlock.pack(-blockSize, curHoleStart);
			memoryBlock.pack(-blockSize, curHoleEnd - INTEGER_SIZE * TAG_SIZE);
			// (2). add it to the lastHole
			setPrevHole(curHoleStart, lastHole);
			setNextHole(lastHole, curHoleStart);
		} else if(left > 0 && right < 0){
			// 2.2. left is occupid but right is not - merge with right hole
			// rightHoleStart == curHoleEnd 
			// (1). update tag - size
			int newBlockSize = blockSize + Math.abs(right) + 2 * TAG_SIZE;
			memoryBlock.pack(-newBlockSize, curHoleStart);
			int rightHoleEnd = getHoleEndFromHoleStart(curHoleEnd);
			memoryBlock.pack(-newBlockSize, rightHoleEnd - INTEGER_SIZE * TAG_SIZE);
			// (2). move right hole reference to cur
			setPrevHole(curHoleStart, getPrevHole(curHoleEnd));
			setNextHole(curHoleStart, getNextHole(curHoleEnd));
		} else if(left < 0 && right > 0){
			// 2.3. left is not occupid but right is - merge with left hole
			// leftHoleEnd == curHoleStart
			// (1). update tag - size
			int newBlockSize = blockSize + Math.abs(left) + 2 * TAG_SIZE;
			int leftHoleStart = getHoleStartFromHoleEnd(curHoleStart);
			memoryBlock.pack(-newBlockSize, leftHoleStart);
			memoryBlock.pack(-newBlockSize, curHoleEnd - INTEGER_SIZE * TAG_SIZE);
		} else{
			// 2.4. both are not occupied - merge with left and right holes
			// (1). update tag - size
			int newBlockSize = blockSize + Math.abs(left) + Math.abs(right) + 4 * TAG_SIZE;
			int leftHoleStart = getHoleStartFromHoleEnd(curHoleStart);
			int rightHoleEnd = getHoleEndFromHoleStart(curHoleEnd);
			memoryBlock.pack(-newBlockSize, leftHoleStart);
			memoryBlock.pack(-newBlockSize, rightHoleEnd - INTEGER_SIZE * TAG_SIZE);
			// (2). remove right hole
			removeHole(curHoleEnd);
		}
	}
	
	public void removeHole(int curHole){
		int prevHole = getPrevHole(curHole);
		int nextHole = getNextHole(curHole);
		if(prevHole != -1)
			setNextHole(prevHole, nextHole);
		if(nextHole != -1)
			setPrevHole(nextHole, prevHole);
		if(curHole == lastHole)
			lastHole = prevHole;
	}
	
	public int getBlockSize(int startIdx) {
		return Math.abs(memoryBlock.unpack(startIdx));
	}
	
	public int getHoleStartFromHoleEnd(int endIdx) {
		int blockSize = Math.abs(memoryBlock.unpack(endIdx - INTEGER_SIZE * TAG_SIZE));
		return endIdx - INTEGER_SIZE * (DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE + blockSize);
	}
	
	public int getHoleEndFromHoleStart(int startIdx) {
		int blockSize = getBlockSize(startIdx);
		return startIdx + INTEGER_SIZE * (blockSize + DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE);
	}

	public int getHoleSizeFromBlockSize(int blockSize) {
		return blockSize + DIFF_BETWEEN_HOLESIZE_AND_BLOCKSIZE;
	}
	
	public int getPrevHole(int curHole) {
		return memoryBlock.unpack(curHole + INTEGER_SIZE * TAG_SIZE);
	}

	public int getNextHole(int curHole) {
		return memoryBlock.unpack(curHole + INTEGER_SIZE * (TAG_SIZE + PREV_INDEX_SIZE));
	}
	
	public void setPrevHole(int curHole, int prevHole) {
		int curPreReferIdx = curHole + INTEGER_SIZE * TAG_SIZE;
		memoryBlock.pack(prevHole, curPreReferIdx);
	}

	public void setNextHole(int curHole, int nextHole) {
		int curNextReferIdx = curHole + INTEGER_SIZE * (TAG_SIZE + PREV_INDEX_SIZE);
		memoryBlock.pack(nextHole, curNextReferIdx);
	}
	
	public int getBlockStartIdx(int startIdx) {
		return startIdx + INTEGER_SIZE * (TAG_SIZE + PREV_INDEX_SIZE + NEXT_INDEX_SIZE);
	}

	public static void main(String[] args) {
		MMManager mmm = new MMManager();
		mmm.init(100);
		int n = 10;
		for(int i = 0; i < n; i++){
			int startIdx = mmm.request(10);
			mmm.release(startIdx);
			if(startIdx == -1)
				System.err.println("Insufficient memory");
			else
				System.out.println("Req succeeds, and the start index is: " + startIdx);
		}
	}
}
