package data;

import java.util.LinkedList;

public class RCB {
	private String rid;
	private int available;
	private int used;
	private LinkedList<PCB> waitingList;
	
	public RCB(String rid, int available){
		this.rid = rid;
		this.available = available;
		this.used = 0;
		this.waitingList = new LinkedList<PCB>();
	}
	
	public void request(int num){
		this.available--;
		this.used++;
	}
	
	public void release(int num){
		this.available++;
		this.used--;
	}
	
	public PCB removeFirstFromWaitingList(){
		return this.waitingList.removeFirst();
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(rid + ": ");
		sb.append(available);
		return sb.toString();
	}
	
	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public int getAvailable() {
		return available;
	}

	public void setAvailable(int available) {
		this.available = available;
	}

	public int getUsed() {
		return used;
	}

	public void setUsed(int used) {
		this.used = used;
	}

	public LinkedList<PCB> getWaitingList() {
		return waitingList;
	}

	public void setWaitingList(LinkedList<PCB> waitingList) {
		this.waitingList = waitingList;
	}
	
}
