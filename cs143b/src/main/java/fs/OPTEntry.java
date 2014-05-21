package fs;

public class OPTEntry {

	public int[] buffer;
	public int currentPosition;
	public int index;
	public int length;
	public int whichBlock;

	public OPTEntry() {
		this.buffer = new int[16];
		this.currentPosition = -1;
		this.index = -1;
		this.length = -1;
		whichBlock = -1;
	}

	public void writeCharToBuffer(char c, int pos) {
		int oldInt = this.buffer[pos / 4];
		byte[] bytes = intToByteArray(oldInt);
		bytes[pos % 4] = (byte) c;
		this.buffer[pos / 4] = fromByteArray(bytes);
	}

	public char readCharFromBuffer(int pos) {
		int curInt = this.buffer[pos / 4];
		byte[] bytes = intToByteArray(curInt);
		return (char) bytes[pos % 4];
	}

	public byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	public int fromByteArray(byte[] bytes) {
		return bytes[0] << 24 | (bytes[1] & 0xFF) << 16
				| (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
	}

	public static void test1() {
		OPTEntry entry = new OPTEntry();
		entry.writeCharToBuffer('B', 5);
		char c = entry.readCharFromBuffer(5);
		System.out.println(c);
	}

	public static void test2() {
		OPTEntry entry = new OPTEntry();
		String s = "BCADE";
		for (int i = 0; i < s.length(); i++) {
			entry.writeCharToBuffer(s.charAt(i), i);
			char c = entry.readCharFromBuffer(i);
			System.out.println(c);
		}
	}

	public static void main(String[] args) {
		test2();
	}

}
