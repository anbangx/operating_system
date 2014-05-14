package pr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class ReadyList {
	HashMap<Integer, LinkedList<PCB>> hM;
	
	public ReadyList(){
		hM = new HashMap<Integer, LinkedList<PCB>>();
		hM.put(0, new LinkedList<PCB>());
		hM.put(1, new LinkedList<PCB>());
		hM.put(2, new LinkedList<PCB>());
	}
	
	public PCB getPCBWithHighPriority(){
		PCB highest = null;
		if(!hM.get(2).isEmpty()){
			highest = hM.get(2).getFirst();
		} else if(!hM.get(1).isEmpty()){
			highest = hM.get(1).getFirst();
		} else if(!hM.get(0).isEmpty()){
			highest = hM.get(0).getFirst();
		}
		return highest;
	}
	
	public PCB get(String pid){
		for(int i = 2; i >= 0; i--){
			Iterator<PCB> it = hM.get(i).iterator();
			while(it.hasNext()){
				PCB pcb = it.next();
				if(pcb.getPid().equals(pid)){
					return pcb;
				}
			}
		}
		return null;
	}
	
	public void insert(PCB pcb){
		int priority = pcb.getPriority();
		this.hM.get(priority).add(pcb);
	}
	
	public void remove(PCB pcb){
		int priority = pcb.getPriority();
		this.hM.get(priority).remove(pcb);
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Priority-2: " + hM.get(2).toString() + "\n");
		sb.append("Priority-1: " + hM.get(1).toString() + "\n");
		sb.append("Priority-0: " + hM.get(0).toString());
		return sb.toString();
	}
}
