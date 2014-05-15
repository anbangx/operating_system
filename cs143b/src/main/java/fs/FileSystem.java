package fs;

public class FileSystem {
	
	private static final int DIRECTORY_START_INDEX = 1;
	private static final int DIRECTORY_END_INDEX = 4;
	private static final int FILE_DESCRIPTOR_START_INDEX = 5;
	private static final int FILE_DESCRIPTOR_END_INDEX = 10;
	private static final int MAX_INDEX_WITHIN_BLOCK = 15;
	private static final int SLOT_SIZE = 2;
	private static final int FILE_DESCRIPTOR_SIZE = 4;
	
	private IOSystem io;
	private long[] MASK;
	
	public FileSystem(){
		this.io = new IOSystem();
		MASK = new long[64];
		MASK[63] = 1;
		for(int i = 62; i >= 0; i--){
			MASK[i] = MASK[i+1] << 1;
		}
	}
	
	public void init(){
		// 1. initiate bitmap
		setBitMap(0);
		setBitMap(1);	// file descriptor for directory
		setBitMap(2);	// directory
		setBitMap(3);	// directory
		setBitMap(4);	// directory
		
		// 2. initiate the file descriptor for directory
		initDirectory();
	}
	
	public void initDirectory(){
		int[] block = io.readBlock(1);
		block[0] = 0;
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
		int curSlotIdx = 1; // 1st index {name, index} 
		int[] dirBlock = io.readBlock(curDirIdx);
		while(curDirIdx < DIRECTORY_END_INDEX){
			if(dirBlock[curSlotIdx] < 0){ // find free
				// update name and index
				dirBlock[curSlotIdx - 1] = convertStringToInt(name);
				dirBlock[curSlotIdx] = (curBlockIdx - FILE_DESCRIPTOR_START_INDEX) * 4 + curReference / 4;
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
		block[1] = (int)(bitMap >> 32);
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
}
