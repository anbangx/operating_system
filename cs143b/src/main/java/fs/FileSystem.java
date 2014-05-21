package fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class FileSystem {

	private static final int DIRECTORY_START_INDEX = 2;
	private static final int DIRECTORY_END_INDEX = 4;
	private static final int FILE_DESCRIPTOR_START_INDEX = 5;
	private static final int FILE_DESCRIPTOR_END_INDEX = 10;
	private static final int MAX_INDEX_WITHIN_BLOCK = 15;
	private static final int SLOT_SIZE = 2;
	private static final int FILE_DESCRIPTOR_SIZE = 4;

	private IOSystem io;
	private OPTEntry[] OPT;
	private long[] MASK;

	public FileSystem() {
		this.io = new IOSystem();
		MASK = new long[64];
		MASK[63] = 1;
		for (int i = 62; i >= 0; i--) {
			MASK[i] = MASK[i + 1] << 1;
		}
		initOPT();
	}

	public void initOPT() {
		OPT = new OPTEntry[4];
		for (int i = 0; i < 4; i++) {
			OPT[i] = new OPTEntry();
		}
		OPT[0].index = 0;
	}

	public String execute(String line) throws Exception {
		String[] tokens = line.split(" ");
		String command = tokens[0];

		if (command.equals("in") && tokens.length == 1) {
			this.init();
			return "disk initialized";
		} else if (command.equals("in") && tokens.length == 2) {
			String inputPath = tokens[1];
			this.restore(inputPath);
			return "disk restored";
		} else if (command.equals("cr")) {
			String name = tokens[1];
			this.create(name);
			return name + " created";
		} else if (command.equals("de")) {
			String name = tokens[1];
			this.destroy(name);
			return name + " destroyed";
		} else if (command.equals("op")) {
			String name = tokens[1];
			int OPTIdx = this.open(name);
			return name + " opened " + OPTIdx;
		} else if (command.equals("rd")) {
			int OPTIdx = Integer.parseInt(tokens[1]);
			int count = Integer.parseInt(tokens[2]);
			String read = this.read(OPTIdx, count);
			return read;
		} else if (command.equals("wr")) {
			int OPTIdx = Integer.parseInt(tokens[1]);
			char c = tokens[2].charAt(0);
			int count = Integer.parseInt(tokens[3]);
			int num = this.write(OPTIdx, c, count);
			return num + " bytes written";
		} else if (command.equals("sk")) {
			int OPTIdx = Integer.parseInt(tokens[1]);
			int target = Integer.parseInt(tokens[2]);
			this.seek(OPTIdx, target);
			return "position is " + target;
		} else if (command.equals("lsdisk")) {
			return io.toString();
		} else if (command.equals("dr")) {
			ArrayList<String> files = getAllFiles();
			return files.toString();
		} else if (command.equals("sv")) {
			String outputPath = tokens[1];
			this.save(outputPath);
			return "disk saved";
		}

		return "";
	}

	public void init() {
		// 1. initiate bitmap
		// file descriptor for directory
		// directory
		// directory
		// directory
		for(int i = 0; i <= FILE_DESCRIPTOR_END_INDEX; i++){
			setBitMap(i);
		}
		// 2. initiate the directory
		initDirectory();
		System.out.println(Long.toBinaryString(getBitMap()));
		System.out.println();
	}

	public void initDirectory() {
		int[] block = io.readBlock(1);
		block[0] = 0; // length of files
		block[1] = 2;
		block[2] = 3;
		block[3] = 4;
	}

	public void create(String name) {
		// 1. find a free file descriptor
		int curBlockIdx = FILE_DESCRIPTOR_START_INDEX;
		int curReference = 0;
		int[] fdBlock = io.readBlock(curBlockIdx);
		while (curBlockIdx <= FILE_DESCRIPTOR_END_INDEX) {
			if (fdBlock[curReference] < 0) { // find free
				// update length
				fdBlock[curReference] = 0;
				break;
			}
			curReference += FILE_DESCRIPTOR_SIZE;
			if (curReference > MAX_INDEX_WITHIN_BLOCK) {
				// check next file descriptor block
				curBlockIdx++;
				fdBlock = io.readBlock(curBlockIdx);
				curReference = 0;
			}
		}

		// 2. find a free directory entry
		int curDirIdx = DIRECTORY_START_INDEX;
		int curSlotIdx = 0; // 1st index {name, index}
		int[] dirBlock = io.readBlock(curDirIdx);
		while (curDirIdx <= DIRECTORY_END_INDEX) {
			if (dirBlock[curSlotIdx + 1] < 0) { // find free
				// update name and index
				dirBlock[curSlotIdx] = convertStringToInt(name);
				dirBlock[curSlotIdx + 1] = (curBlockIdx - FILE_DESCRIPTOR_START_INDEX)
						* 4 + curReference / 4;
				break;
			}
			curSlotIdx += SLOT_SIZE;
			if (curSlotIdx > MAX_INDEX_WITHIN_BLOCK) {
				// check next directory block
				curDirIdx++;
				dirBlock = io.readBlock(curSlotIdx);
				curSlotIdx = 0;
			}
		}

		// 3. write back the updates to disk
		io.writeBlock(curBlockIdx, fdBlock);
		io.writeBlock(curDirIdx, dirBlock);
	}

	public void destroy(String name) {
		// 1. search directory to find file descriptor
		int curDirIdx = DIRECTORY_START_INDEX;
		int curSlotIdx = 0; // 1st index {name, index}
		int[] dirBlock = io.readBlock(curDirIdx);
		int nameToInt = convertStringToInt(name);
		int fdIdx = -1;
		int[] fdBlock = null;
		while (curDirIdx <= DIRECTORY_END_INDEX) {
			if (dirBlock[curSlotIdx] == nameToInt) { // find!
				// 2. free file descriptor
				fdIdx = curSlotIdx / 4 + 5;
				int fdReference = curSlotIdx % 4;
				fdBlock = io.readBlock(fdIdx);
				fdBlock[fdReference] = -1;
				// 3. update bitmap
				for (int i = 1; i < 4; i++) {
					setBitMap(fdBlock[fdReference + i]);
				}
				// 4. remove directory entry
				dirBlock[curSlotIdx + 1] = -1;
				break;
			}
			curSlotIdx += SLOT_SIZE;
			if (curSlotIdx > MAX_INDEX_WITHIN_BLOCK) {
				// check next directory block
				curDirIdx++;
				dirBlock = io.readBlock(curSlotIdx);
				curSlotIdx = 0;
			}
		}

		// 5. write back the updates to disk
		io.writeBlock(fdIdx, fdBlock);
		io.writeBlock(curDirIdx, dirBlock);
	}

	public int open(String name) {
		// 1. search directory to find file descriptor
		int slotIdx = getSlotIdx(name);
		if (slotIdx < 0) {
			System.out.println(name + " doesn't exist!");
			return -1;
		}

		// 2. allocate a free OPT entry
		int freeOPTIdx = getFreeOPTEntryIdx();

		// 3. fill in current position and file descriptor index
		OPT[freeOPTIdx].currentPosition = 0;
		OPT[freeOPTIdx].index = slotIdx;
		OPT[freeOPTIdx].whichBlock = 0;

		// 4 search first data block - update bitmap, update file descriptor
		int[] fdBlock = getFDBlockFromSlotIdx(slotIdx);
		if (fdBlock[slotIdx * 4 + 1] == -1) {
			int newBlockIdx = searchAndUpdateBitMap();
			fdBlock[slotIdx * 4 + 1] = newBlockIdx;
			int fdIdx = slotIdx / 4 + 5;
			io.writeBlock(fdIdx, fdBlock);
		}

		// 5. read block 0 of file into the r/w buffer(read-ahead)
		int fileLength = fdBlock[slotIdx * 4];
		int firstDataBlockIdx = fdBlock[slotIdx * 4 + 1];
		OPT[freeOPTIdx].length = fileLength;
		if (firstDataBlockIdx != -1)
			OPT[freeOPTIdx].buffer = io.readBlock(firstDataBlockIdx);

		return freeOPTIdx;
	}

	public boolean close(int OPTIdx) {
		// 1. write buffer to disk
		int dataBlockIdx = getDataBlockIdxFromOPTEntry(OPT[OPTIdx], OPT[OPTIdx].whichBlock);
		io.writeBlock(dataBlockIdx, OPT[OPTIdx].buffer);

		// 2. update file length in descriptor
		int slotIdx = OPT[OPTIdx].index;
		int[] fdBlock = getFDBlockFromSlotIdx(slotIdx);
		fdBlock[slotIdx % 4] = OPT[OPTIdx].length;
		int fdIdx = slotIdx / 4 + 5;
		io.writeBlock(fdIdx, fdBlock);
		
		// 3. free OPT entry
		OPT[OPTIdx].index = -1;

		// 4. return status
		return true;
	}

	public String read(int OPTIdx, int count) {
		StringBuilder sb = new StringBuilder();
		while(count > 0 && OPT[OPTIdx].currentPosition < OPT[OPTIdx].length){
			if(OPT[OPTIdx].currentPosition < 64 * (OPT[OPTIdx].whichBlock + 1)){
				// read buffer
				char c = OPT[OPTIdx].readCharFromBuffer(OPT[OPTIdx].currentPosition % 64);
				sb.append(c);
				OPT[OPTIdx].currentPosition++;
				count--;
			} else{
				// switch to next block
				OPT[OPTIdx].whichBlock++;
				int dataBlockIdx = getDataBlockIdxFromOPTEntry(OPT[OPTIdx], OPT[OPTIdx].whichBlock);
				if(dataBlockIdx == -1)
					break;
				OPT[OPTIdx].buffer = io.readBlock(dataBlockIdx);
			}
		}
		
		return sb.toString();
	}

	public int write(int OPTIdx, char c, int count) {
		int oldCount = count;
		while(count > 0){
			if(OPT[OPTIdx].currentPosition < 64 * (OPT[OPTIdx].whichBlock + 1)){
				// 1. write text to buffer
				OPT[OPTIdx].writeCharToBuffer(c, OPT[OPTIdx].currentPosition % 64);
				OPT[OPTIdx].currentPosition++;
				OPT[OPTIdx].length++;
				count--;
			} else{
				// 2. write the buffer to disk block
				int dataBlockIdx = getDataBlockIdxFromOPTEntry(OPT[OPTIdx], OPT[OPTIdx].whichBlock);
				io.writeBlock(dataBlockIdx, OPT[OPTIdx].buffer);

				// 3. update file length in descriptor
				int slotIdx = OPT[OPTIdx].index;
				int[] fdBlock = getFDBlockFromSlotIdx(slotIdx);
				fdBlock[0] = OPT[OPTIdx].length;
				int fdIdx = slotIdx / 4 + 5;
				
				// 4. switch to next block
				OPT[OPTIdx].whichBlock++;
				//    search first data block - update bitmap, update file descriptor
				if (fdBlock[slotIdx * 4 + OPT[OPTIdx].whichBlock + 1] == -1) {
					int newBlockIdx = searchAndUpdateBitMap();
					fdBlock[slotIdx * 4 + OPT[OPTIdx].whichBlock + 1] = newBlockIdx;
				}
				io.writeBlock(fdIdx, fdBlock);
				
				// 5. read block whichBlock of file into the r/w buffer(read-ahead)
				int fileLength = fdBlock[slotIdx * 4];
				int firstDataBlockIdx = fdBlock[slotIdx * 4 + OPT[OPTIdx].whichBlock + 1];
				OPT[OPTIdx].length = fileLength;
				if (firstDataBlockIdx != -1)
					OPT[OPTIdx].buffer = io.readBlock(firstDataBlockIdx);
			}
		}
		
		return oldCount;
	}

	public void seek(int OPTIdx, int target) {
		int curDataBlockNum = OPT[OPTIdx].currentPosition / 64;
		int targetDataBlockNum = target / 64;
		if (curDataBlockNum != targetDataBlockNum) { // if the new position is
														// not within the
														// current block
			int slotIdx = OPT[OPTIdx].index;
			// 1. write the old buffer to disk
			int[] fdBlock = getFDBlockFromSlotIdx(slotIdx);
			int oldDataBlockIdx = fdBlock[slotIdx % 4 + curDataBlockNum + 1];
			io.writeBlock(oldDataBlockIdx, OPT[OPTIdx].buffer);

			// 2. read the new block to OPT
			int newDataBlockIdx = fdBlock[slotIdx % 4 + targetDataBlockNum + 1];
			OPT[OPTIdx].buffer = io.readBlock(newDataBlockIdx);
			OPT[OPTIdx].whichBlock = targetDataBlockNum;
		}
		OPT[OPTIdx].currentPosition = target;
	}

	public void save(String outputPath) throws Exception {
		for (int i = 1; i < 4; i++) {
			if (OPT[i].index != -1) {
				close(i);
			}
		}
		
		// convert array of bytes into file
		FileOutputStream fileOuputStream = new FileOutputStream(outputPath);
		byte[] temp = io.saveDiskToBytes();
		fileOuputStream.write(temp);
		fileOuputStream.close();

		// init OPT
		initOPT();
	}

	public void restore(String inputPath) throws Exception {
		byte[] bytes = new byte[64 * 64];
		FileInputStream fileInputStream = new FileInputStream(new File(
				inputPath));
		fileInputStream.read(bytes);
		fileInputStream.close();
		io.restoreDiskFromBytes(bytes);
		System.out.println();
	}

	public ArrayList<String> getAllFiles() {
		ArrayList<String> files = new ArrayList<String>();

		int curDirIdx = DIRECTORY_START_INDEX;
		int curSlotIdx = 0; // 1st index {name, index}
		int[] dirBlock = io.readBlock(curDirIdx);
		while (curDirIdx <= DIRECTORY_END_INDEX) {
			if (dirBlock[curSlotIdx + 1] != -1) { // find
				String name = convertIntToString(dirBlock[curSlotIdx]);
				files.add(name.substring(0, name.indexOf(' ')));
			}
			curSlotIdx += SLOT_SIZE;
			if (curSlotIdx > MAX_INDEX_WITHIN_BLOCK) {
				// check next directory block
				curDirIdx++;
				dirBlock = io.readBlock(curSlotIdx);
				curSlotIdx = 0;
			}
		}

		return files;
	}

	public int getSlotIdx(String name) {
		int curDirIdx = DIRECTORY_START_INDEX;
		int curSlotIdx = 0; // 1st index {name, index}
		int[] dirBlock = io.readBlock(curDirIdx);
		int nameToInt = convertStringToInt(name);
		while (curDirIdx <= DIRECTORY_END_INDEX) {
			if (dirBlock[curSlotIdx] == nameToInt) { // find!
				return curSlotIdx;
			}
			curSlotIdx += SLOT_SIZE;
			if (curSlotIdx > MAX_INDEX_WITHIN_BLOCK) {
				// check next directory block
				curDirIdx++;
				dirBlock = io.readBlock(curSlotIdx);
				curSlotIdx = 0;
			}
		}
		return -1;
	}

	public int getFreeOPTEntryIdx() {
		for (int i = 0; i < OPT.length; i++) {
			if (OPT[i].index < 0) {
				return i;
			}
		}
		return -1;
	}

	public int searchAndUpdateBitMap() {
		long bitmap = getBitMap();
		for (int i = 0; i < 64; i++) {
			if ((bitmap & MASK[i]) == 0) {
				// find! stop search
				setBitMap(i);
				return i;
			}
		}
		return -1;
	}

	public int getDataBlockIdxFromOPTEntry(OPTEntry entry, int whickBlock) {
		int slotIdx = entry.index;
		int[] fdBlock = getFDBlockFromSlotIdx(slotIdx);
		int firstDataBlockIdx = fdBlock[slotIdx % 4 + whickBlock + 1];
		return firstDataBlockIdx;
	}

	public int[] getFDBlockFromSlotIdx(int curSlotIdx) {
		int fdIdx = curSlotIdx / 4 + 5;
		return io.readBlock(fdIdx);
	}

	public int getReferenceFromSlotIdx(int curSlotIdx) {
		return curSlotIdx % 4;
	}

	public long getBitMap() {
		int[] block = io.readBlock(0);
		return convertToLong(block[0], block[1]);
	}

	public void setBitMap(int i) {
		if (i == -1)
			return;
		long bitMap = getBitMap();
		bitMap = bitMap | MASK[i];
		int[] block = new int[16];
		block[0] = (int) (bitMap >> 32);
		block[1] = (int) bitMap;
		io.writeBlock(0, block);
	}

	public long convertToLong(int a, int b) {
		long c = (long) a << 32 | b & 0xFFFFFFFFL;
		return c;
	}

	public int convertStringToInt(String name) {
		StringBuilder s = new StringBuilder(name);
		for (int i = name.length() - 1; i < 4; i++) {
			s.append(" ");
		}
		byte[] buffer = s.toString().getBytes();
		return byteArrayToInt(buffer);
	}

	public String convertIntToString(int a) {
		byte[] bytes = intToByteArray(a);
		return new String(bytes);
	}

	public int byteArrayToInt(byte[] b) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i] & 0x000000FF) << shift;
		}
		return value;
	}

	public byte[] intToByteArray(int a) {
		byte[] ret = new byte[4];
		ret[3] = (byte) (a & 0xFF);
		ret[2] = (byte) ((a >> 8) & 0xFF);
		ret[1] = (byte) ((a >> 16) & 0xFF);
		ret[0] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	public int[] convertStringToIntArray(String s) {
		int len = s.length();
		int num = len / 4;
		int[] results = new int[num];
		for (int i = 0; i < num; i++) {
			results[i] = convertStringToInt(s.substring(4 * i, 4));
		}
		return results;
	}
}
