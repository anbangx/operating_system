package data;

import java.util.HashMap;
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
}
