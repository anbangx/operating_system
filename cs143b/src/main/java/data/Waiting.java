package data;

public class Waiting {
	PCB pcb;
	int amount;
	
	public Waiting(PCB pcb, int amount){
		this.pcb = pcb;
		this.amount = amount;
	}
	
	public String toString(){
		return pcb + ":" + amount;
	}
}
