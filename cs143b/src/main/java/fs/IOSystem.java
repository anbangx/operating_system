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
			block[x] = pm.unpack(x);
		}
		return block;
	}

	public void writeBlock(int i, int[] block) {
		PackableMemory pm = ldisk.get(i);
		for(int x = 0; x < block.length; x++){
			pm.pack(block[0], i);
		}
	}
	
}
