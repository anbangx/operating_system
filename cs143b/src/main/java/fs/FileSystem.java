package fs;

public class FileSystem {
	
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
		
	}
	
	public void create(String name){
		
	}
	
	public long getBitMap(){
		int[] block = io.readBlock(0);
		return convertToLong(block[0], block[1]);
	}
	
	public void setBitMap(int i){
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
}
