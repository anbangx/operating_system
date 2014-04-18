package data;

public class Inventory {
	RCB rcb;
	int amount;
	
	public Inventory(RCB rcb, int amount){
		this.rcb = rcb;
		this.amount = amount;
	}
	
	public String toString(){
		return rcb + ":" + amount;
	}
}
