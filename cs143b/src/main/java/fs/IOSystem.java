package fs;

import java.util.ArrayList;

public class IOSystem {

	private static final int L = 64;
	private static final int B = 64;

	private ArrayList<PackableMemory> ldisk;

	public IOSystem() {
		ldisk = new ArrayList<PackableMemory>(L);
		for(int i = 0; i < L; i++){
			ldisk.add(new PackableMemory(B));
		}
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
