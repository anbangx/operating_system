package fs;

public class IOSystem {

	private static final int L = 64;
	private static final int B = 64;

	private byte[][] ldisk;

	public IOSystem() {
		ldisk = new byte[L][B];
	}

	public byte[] readBlock(int i) {
		return ldisk[i];
	}

	public void writeBlock(int i, byte[] block) {
		ldisk[i] = block;
	}
}
