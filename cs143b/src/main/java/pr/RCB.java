package pr;

import java.util.LinkedList;

public class RCB {
	private String rid;
	private int available;
	private int used;
	private LinkedList<Waiting> waitingList;
	
	public RCB(String rid, int available){
		this.rid = rid;
		this.available = available;
		this.used = 0;
		this.waitingList = new LinkedList<Waiting>();
	}
	
	public void request(int num){
		this.available = this.available - num;
		this.used = this.used + num;
	}
	
	public void release(int num){
		this.available = this.available + num;
		this.used = this.used - num;
	}
	
	public PCB removeFirstFromWaitingList(){
		for(Waiting waiting : this.waitingList){
			if(available >= waiting.amount){
				request(waiting.amount);
				this.waitingList.remove(waiting);
				return waiting.pcb;
			}
		}
		return null;
	}
	
	public String getInfo(){
		StringBuilder sb = new StringBuilder();
		sb.append(rid + ": ");
		sb.append("[Av.: " + available + "], ");
		sb.append("[List: {" + waitingList + "}]");
		return sb.toString();
	}
	
	public int getTotal(){
		return this.available + this.used;
	}
	
	public String toString(){
		return rid;
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

	public LinkedList<Waiting> getWaitingList() {
		return waitingList;
	}

	public void setWaitingList(LinkedList<Waiting> waitingList) {
		this.waitingList = waitingList;
	}
	
}
