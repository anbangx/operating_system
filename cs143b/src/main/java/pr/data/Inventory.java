package pr.data;

public class Inventory {
	public RCB rcb;
	public int amount;
	
	public Inventory(RCB rcb, int amount){
		this.rcb = rcb;
		this.amount = amount;
	}
	
	public String toString(){
		return rcb + ":" + amount;
	}
}
