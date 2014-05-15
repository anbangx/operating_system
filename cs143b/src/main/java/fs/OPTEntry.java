package fs;

public class OPTEntry {
	
	public int[] buffer;
	public int currentPosition;
	public int index;
	public int length;
	
	public OPTEntry(){
		this.buffer = new int[16];
		this.currentPosition = -1;
		this.index = -1;
		this.length = -1;
	}
	
	public OPTEntry(int[] buffer, int currentPosition, int index, int length){
		this.buffer = buffer;
		this.currentPosition = currentPosition;
		this.index = index;
		this.length = length;
	}
	
	public void writeCharToBuffer(char c, int pos){
		this.buffer[pos / 4] = (byte)c << (4 - pos % 4);
	}
	
	public char readCharFromBuffer(int pos){
		return (char)(this.buffer[pos / 4] >> (4 - pos % 4));
	}
	
}
