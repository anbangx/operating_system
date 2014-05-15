package fs;

import java.util.ArrayList;
import java.util.Arrays;

public class IOSystem {

	private static final int L = 64;
	private static final int B = 64;

	private ArrayList<PackableMemory> ldisk;

	public IOSystem() {
		ldisk = new ArrayList<PackableMemory>(L);
		for(int i = 0; i < L; i++){
			PackableMemory pm = new PackableMemory(B);
			if(i > 0){
				for(int j = 0; j < 16; j++){
					pm.pack(-1, 4 * j);
				}
			}
			ldisk.add(pm);
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < 10; i++){
			sb.append(i + ": " + Arrays.toString(readBlock(i)) + "\n");
		}
		return sb.toString();
	}
	
	public int[] readBlock(int i) {
		PackableMemory pm = ldisk.get(i);
		int[] block = new int[16];
		for(int x = 0; x < 16; x++){
			block[x] = pm.unpack(4 * x);
		}
		return block;
	}

	public void writeBlock(int i, int[] block) {
		boolean debug = true;
		if(debug){
			System.out.println("Write " + convertIntArrayToBitRepresetation(block) + " into block " + i);
		}
		PackableMemory pm = ldisk.get(i);
		for(int x = 0; x < block.length; x++){
			pm.pack(block[x], 4 * x);
		}
	}
	
	public String convertIntArrayToBitRepresetation(int[] A){
		StringBuilder sb = new StringBuilder("[");
		for(int i = 0; i < A.length; i++){
			sb.append(Integer.toBinaryString(A[i]) + ",");
		}
		sb.append("]");
		return sb.toString();
	}
	
	public int convertStringToInt(String name){
		StringBuilder s = new StringBuilder(name);
		s.setLength(4);
		byte[] buffer = s.toString().getBytes();
		return byteArrayToInt(buffer);
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
	
	public static void test1(){
		IOSystem io = new IOSystem();
		int[] write = new int[16];
		write[0] = io.convertStringToInt("ABC"); 
		io.writeBlock(0, write);
		System.out.println("Write: " + Arrays.toString(write));
		int[] read = io.readBlock(0);
		System.out.println("Read: " + Arrays.toString(read));
	}
	
	public static void main(String[] args){
		test1();
	}
}
