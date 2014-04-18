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
	
	@Override
	public boolean equals(Object other) {
		Waiting waiting = (Waiting)other;
		return this.pcb.getPid().equals(waiting.pcb.getPid());
	}
}
