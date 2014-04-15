package data;

import java.util.LinkedList;

public class RCB {
	private String RName;
	private int available;
	private int used;
	private LinkedList<PCB> waitingList;
	
	public RCB(String RName, int available){
		this.RName = RName;
		this.available = available;
		this.used = 0;
		this.waitingList = new LinkedList<PCB>();
	}
}
