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
}
