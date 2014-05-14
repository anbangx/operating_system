package fs;

public class test {

	public static void main(String[] args) {
		PackableMemory pm = new PackableMemory(4);
		
		StringBuilder s = new StringBuilder("ABC");
		s.setLength(4);
		byte[] buffer = s.toString().getBytes();
		int a = test.byteArrayToInt(buffer);
		System.out.println(a);
		
		byte[] bytes= test.intToByteArray(a);
		String ss = new String(bytes);
		System.out.println(ss);
	}
	
	public static int byteArrayToInt(byte[] b) 
	{
	    int value = 0;
	    for (int i = 0; i < 4; i++) {
	        int shift = (4 - 1 - i) * 8;
	        value += (b[i] & 0x000000FF) << shift;
	    }
	    return value;
	}

	public static byte[] intToByteArray(int a)
	{
	    byte[] ret = new byte[4];
	    ret[3] = (byte) (a & 0xFF);   
	    ret[2] = (byte) ((a >> 8) & 0xFF);   
	    ret[1] = (byte) ((a >> 16) & 0xFF);   
	    ret[0] = (byte) ((a >> 24) & 0xFF);
	    return ret;
	}

}
