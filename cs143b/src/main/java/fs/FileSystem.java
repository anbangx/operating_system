package fs;

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
	
	public FileSystem(){
		this.io = new IOSystem();
		MASK = new long[64];
		MASK[63] = 1;
		for(int i = 62; i >= 0; i--){
			MASK[i] = MASK[i+1] << 1;
		}
		OPT = new OPTEntry[100];
	}
	
	public String execute(String line){
		String[] tokens = line.split(" ");
		String command = tokens[0];
		
		if(command.equals("in")){
			this.init();
			return "disk initialized";
		} else if(command.equals("cr")){
			String name = tokens[1];
			this.create(name);
			return name + " created";
		} else if(command.equals("op")){
			String name = tokens[1];
			int OPTIdx = this.open(name);
			return name + " opened " + OPTIdx;
		} else if(command.equals("lsdisk")){
			return io.toString();
		}
		return "";
	}
	
	public void init(){
		// 1. initiate bitmap
		setBitMap(0);
		setBitMap(1);	// file descriptor for directory
		setBitMap(2);	// directory
		setBitMap(3);	// directory
		setBitMap(4);	// directory
		
		// 2. initiate the directory
		initDirectory();
	}
	
	public void initDirectory(){
		int[] block = io.readBlock(1);
		block[0] = 0; // length of files
		block[1] = 2;
		block[2] = 3;
		block[3] = 4;
	}
	
	public void create(String name){
		// 1. find a free file descriptor
		int curBlockIdx = FILE_DESCRIPTOR_START_INDEX;
		int curReference = 0;
		int[] fdBlock = io.readBlock(curBlockIdx);
		while(curBlockIdx <= FILE_DESCRIPTOR_END_INDEX){
			if(fdBlock[curReference] < 0){ // find free
				// update length
				fdBlock[curReference] = 0;
				break;
			}
			curReference += FILE_DESCRIPTOR_SIZE;
			if(curReference > MAX_INDEX_WITHIN_BLOCK){
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
		while(curDirIdx <= DIRECTORY_END_INDEX){
			if(dirBlock[curSlotIdx + 1] < 0){ // find free
				// update name and index
				dirBlock[curSlotIdx] = convertStringToInt(name);
				dirBlock[curSlotIdx + 1] = (curBlockIdx - FILE_DESCRIPTOR_START_INDEX) * 4 + curReference / 4;
				break;
			}
			curSlotIdx += SLOT_SIZE;
			if(curSlotIdx > MAX_INDEX_WITHIN_BLOCK){
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
	
	public void destroy(String name){
		// 1. search directory to find file descriptor
		int curDirIdx = DIRECTORY_START_INDEX;
		int curSlotIdx = 0; // 1st index {name, index} 
		int[] dirBlock = io.readBlock(curDirIdx);
		int nameToInt = convertStringToInt(name);
		int fdIdx = -1;
		int[] fdBlock = null;
		while(curDirIdx < DIRECTORY_END_INDEX){
			if(dirBlock[curSlotIdx] == nameToInt){ // find!
				// 2. free file descriptor
				fdIdx = curSlotIdx / 4 + 5;
				int fdReference = curSlotIdx % 4;
				fdBlock = io.readBlock(fdIdx);
				fdBlock[fdReference] = -1;
				// 3. update bitmap
				for(int i = 1; i < 4; i++){
					setBitMap(fdBlock[fdReference + i]);
				}
				// 4. remove directory entry
				dirBlock[curSlotIdx + 1] = -1;
			}
			curSlotIdx += SLOT_SIZE;
			if(curSlotIdx > MAX_INDEX_WITHIN_BLOCK){
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
	
	public int open(String name){
		// 1. search directory to find file descriptor
		int slotIdx = getSlotIdx(name);
		if(slotIdx < 0){
			System.out.println(name + " doesn't exist!");
			return -1;
		}
		
		// 2. allocate a free OPT entry
		int freeOPTIdx = getFreeOPTEntryIdx();
		
		// 3. fill in current position and file descriptor index
		OPT[freeOPTIdx].currentPosition = 0;
		OPT[freeOPTIdx].index = slotIdx;
		
		// 4. read block 0 of file into the r/w buffer(read-ahead)
		int[] fdBlock = getBlockFromSlotIdx(slotIdx);
		int fileLength = fdBlock[slotIdx * 4];
		int firstDataBlockIdx = fdBlock[slotIdx * 4 + 1];
		OPT[freeOPTIdx].length = fileLength;
		OPT[freeOPTIdx].buffer = io.readBlock(firstDataBlockIdx);
		
		return freeOPTIdx;
	}
	
	public boolean close(int OPTIdx){
		// 1. write buffer to disk
		int dataBlockIdx = getDataBlockIdxFromOPTEntry(OPT[OPTIdx]);
		io.writeBlock(dataBlockIdx, OPT[OPTIdx].buffer);
		
		// 2. update file length in descriptor
		int slotIdx = OPT[OPTIdx].index;
		int[] fdBlock = getBlockFromSlotIdx(slotIdx);
		fdBlock[slotIdx % 4] = OPT[OPTIdx].length;
		
		// 3. free OPT entry
		OPT[OPTIdx].index = -1;
		  
		// 4. return status
		return true;
	}
	
	public String read(int OPTIdx, int count){
		// 1. compute position in the r/w buffer
		int currentPosition = OPT[OPTIdx].currentPosition;
		int length = OPT[OPTIdx].length;
		StringBuilder sb = new StringBuilder();
		
		if(currentPosition + count < length){ // not reach the end of buffer
			// 2. read buffer
			for(int i = 0; i < count; i++){
				char c = OPT[OPTIdx].readCharFromBuffer(currentPosition + i);
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	public int write(int OPTIdx, String text){
		// 1. compute position in the r/w buffer
		int currentPosition = OPT[OPTIdx].currentPosition;
		
		int len = text.length();
		if(currentPosition + len <= 64){ // not reach the end of buffer
			// if block doesn't exist
			if(OPT[OPTIdx].index == -1){
				// allocate new block
				int newBlockIdx = searchAndUpdateBitMap();
				OPT[OPTIdx].index = newBlockIdx;
				OPT[OPTIdx].buffer = io.readBlock(newBlockIdx);
			}
			// 2. write text to buffer
			for(int i = 0; i < len; i++){
				OPT[OPTIdx].writeCharToBuffer(text.charAt(i), currentPosition + i);
				OPT[OPTIdx].currentPosition++;
				OPT[OPTIdx].length++;
			}
			// 3. write the buffer to disk block
			int dataBlockIdx = getDataBlockIdxFromOPTEntry(OPT[OPTIdx]);
			io.writeBlock(dataBlockIdx, OPT[OPTIdx].buffer);
			// 4. update file length in descriptor
			int slotIdx = OPT[OPTIdx].index;
			int[] fdBlock = getBlockFromSlotIdx(slotIdx);
			fdBlock[0] = OPT[OPTIdx].length;
		}
		return len;
	}
	
	public int getSlotIdx(String name){
		int curDirIdx = DIRECTORY_START_INDEX;
		int curSlotIdx = 0; // 1st index {name, index} 
		int[] dirBlock = io.readBlock(curDirIdx);
		int nameToInt = convertStringToInt(name);
		while(curDirIdx <= DIRECTORY_END_INDEX){
			if(dirBlock[curSlotIdx + 1] == nameToInt){ // find!
				return curSlotIdx;
			}
			curSlotIdx += SLOT_SIZE;
			if(curSlotIdx > MAX_INDEX_WITHIN_BLOCK){
				// check next directory block
				curDirIdx++;
				dirBlock = io.readBlock(curSlotIdx);
				curSlotIdx = 0;
			}
		}
		return -1;
	}
	
	public int getFreeOPTEntryIdx(){
		for(int i = 0; i < OPT.length; i++){
			if(OPT[i].index < 0){
				return i;
			}
		}
		return -1;
	}
	
	public int searchAndUpdateBitMap(){
		long bitmap = getBitMap();
		for(int i = 0; i < 64; i++){
			if((bitmap & MASK[i]) == 0){
				// find! stop search
				setBitMap(i);
				return i;
			}
		}
		return -1;
	}
	
	public int getDataBlockIdxFromOPTEntry(OPTEntry entry){
		int slotIdx = entry.index;
		int[] fdBlock = getBlockFromSlotIdx(slotIdx);
		int firstDataBlockIdx = fdBlock[slotIdx % 4 + 1];
		return firstDataBlockIdx;
	}
	
	public int[] getBlockFromSlotIdx(int curSlotIdx){
		int fdIdx = curSlotIdx / 4 + 5;
		return io.readBlock(fdIdx);
	}
	
	public int getReferenceFromSlotIdx(int curSlotIdx){
		return curSlotIdx % 4;
	}
	
	public long getBitMap(){
		int[] block = io.readBlock(0);
		return convertToLong(block[0], block[1]);
	}
	
	public void setBitMap(int i){
		if(i == -1)
			return;
		long bitMap = getBitMap();
		bitMap = bitMap | MASK[i];
		int[] block = new int[64]; 
		block[0] = (int)(bitMap >> 32);
		block[1] = (int)bitMap;
		io.writeBlock(i, block);
	}
	
	public long convertToLong(int a, int b){
		long c = (long)a << 32 | b & 0xFFFFFFFFL;
		return c;
	}
	
	public int convertStringToInt(String name){
		StringBuilder s = new StringBuilder(name);
		s.setLength(4);
		byte[] buffer = s.toString().getBytes();
		return byteArrayToInt(buffer);
	}
	
	public String convertIntToString(int a){
		byte[] bytes= intToByteArray(a);
		return new String(bytes);
	}
	
	public int byteArrayToInt(byte[] b) 
	{
	    int value = 0;
	    for (int i = 0; i < 4; i++) {
	        int shift = (4 - 1 - i) * 8;
	        value += (b[i] & 0x000000FF) << shift;
	    }
	    return value;
	}

	public byte[] intToByteArray(int a)
	{
	    byte[] ret = new byte[4];
	    ret[3] = (byte) (a & 0xFF);   
	    ret[2] = (byte) ((a >> 8) & 0xFF);   
	    ret[1] = (byte) ((a >> 16) & 0xFF);   
	    ret[0] = (byte) ((a >> 24) & 0xFF);
	    return ret;
	}
	
	public int[] convertStringToIntArray(String s){
		int len = s.length();
		int num = len / 4;
		int[] results = new int[num];
		for(int i = 0; i < num; i++){
			results[i] = convertStringToInt(s.substring(4 * i, 4));
		}
		return results;
	}
}
