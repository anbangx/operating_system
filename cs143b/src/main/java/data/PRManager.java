package data;

import java.util.Iterator;
import java.util.LinkedList;

public class PRManager {
	private PCB runningProcess;
	private LinkedList<RCB> allResources;
	private RCB IO;
	private ReadyList RL;
	
	public PRManager(){
		this.runningProcess = new PCB("Init", 0);
//		this.runningProcess.getStatus().type = PCB.Type.RUNNING;
		System.out.println("Init is running");
		allResources = new LinkedList<RCB>();
		allResources.add(new RCB("R1", 1));
		allResources.add(new RCB("R2", 1));
		RL = new ReadyList();
		this.RL.insert(this.runningProcess);
		IO = new RCB("IO", 1);
	}
	
	public void init(){
		this.runningProcess = new PCB("Init", 0);
		System.out.println("Init is running");
		allResources = new LinkedList<RCB>();
		allResources.add(new RCB("R1", 1));
		allResources.add(new RCB("R2", 1));
		RL = new ReadyList();
		this.RL.insert(this.runningProcess);
		IO = new RCB("IO", 1);
	}
	
	public void createProcess(String pid, int priority){
		// 1. create PCB data structure
		PCB pcb = new PCB(pid, priority);
		
		// 2. link PCB to creation tree
		this.runningProcess.getCreationTree().children.add(pcb);
		pcb.getCreationTree().parent = this.runningProcess;
		
		// 3. insert PCB to RL
		this.RL.insert(pcb);
		
		// 4. reschedule
		this.scheduler();
		
//		System.out.println(RL.toString());
	}
	
	public void destroyProcess(String pid){
		PCB pcb = this.RL.get(pid);
		killTree(pcb);
		this.scheduler();
	}
	
	public void killTree(PCB pcb){
		for(PCB child : pcb.getCreationTree().children){
			killTree(child);
		}
		
		// free resources
		for(RCB rcb : pcb.getResourceList())
			releaseResource(rcb.getRid());
		// update pointer
		this.RL.remove(pcb);
	}
	
	public void requestResource(String rid){
		// get the corresponding RCB using rid
		RCB rcb = this.getRCB(rid);
		if(rcb.getAvailable() > 0){
			rcb.request(1);
			this.runningProcess.getResourceList().add(rcb);
		} else{
			this.runningProcess.getStatus().type = PCB.Type.WAITING;
			this.RL.remove(this.runningProcess);
			rcb.getWaitingList().add(this.runningProcess);
			scheduler();
		}
		
//		System.out.println("Current ResourceList: " + this.allResources);
	}
	
	public void releaseResource(String rid){
		RCB rcb = this.runningProcess.getRCB(rid);
		this.runningProcess.getResourceList().remove(rcb);
		LinkedList<PCB> waitingList = rcb.getWaitingList();
		if(waitingList.isEmpty())
			rcb.release(1);
		else{
			PCB pcb = rcb.removeFirstFromWaitingList();
			pcb.getStatus().type = PCB.Type.READY;
			pcb.getStatus().RL = this.RL;
			this.RL.insert(pcb);
			pcb.getResourceList().add(rcb);
			scheduler();
		}
		
//		System.out.println("Current ResourceList: " + this.allResources);
	}
	
	public RCB getRCB(String rid){
		Iterator<RCB> it = this.allResources.iterator();
		while(it.hasNext()){
			RCB rcb = it.next();
			if(rcb.getRid().equals(rid))
				return rcb;
		}
		return null;
	}
	
	public void requestIO(){
		this.runningProcess.getStatus().type = PCB.Type.WAITING;
		this.runningProcess.getStatus().RL = null;
		this.RL.remove(this.runningProcess);
		this.IO.getWaitingList().add(this.runningProcess);
		scheduler();
	}
	
	public void IOCompletion(){
		PCB pcb = this.IO.removeFirstFromWaitingList();
		pcb.getStatus().type = PCB.Type.READY;
		pcb.getStatus().RL = this.RL;
		this.RL.insert(pcb);
		scheduler();
	}
	
	// function call simulate time out
	public void timeOut(){
		// 1. remove q from Ready List
		this.RL.remove(this.runningProcess);
		
		// 2. insert q into Ready List
		this.RL.insert(this.runningProcess);
		
		// 3. reschedule
		this.scheduler();
		
		System.out.println(RL.toString());
	}
	
	public void scheduler(){
		// 1. find highest priority process
		PCB highest = RL.getPCBWithHighPriority();
		
		// 2. under some conditions, do context switch
		if(this.runningProcess.getPriority() < highest.getPriority()	//(1) create/release
				|| this.runningProcess.getStatus().type != PCB.Type.RUNNING	//(2) request/time-out
				|| this.runningProcess == null)	//(3) destroy
			preempt(highest);
	}
	
	public void preempt(PCB highest){
		//(1)
		if(this.runningProcess != null && this.runningProcess.getStatus().type == PCB.Type.RUNNING){
			this.runningProcess.getStatus().type = PCB.Type.READY;
			this.RL.insert(this.runningProcess);
		}
		highest.getStatus().type = PCB.Type.RUNNING;
		this.runningProcess = highest;
		this.RL.remove(highest);
//		System.out.println("Process " + highest.getPid() + " is running");
	}
	
	public void printCurrentRunningProcess(){
		System.out.println("Process " + this.runningProcess.getPid() + " is running");
	}
	
	public void listAllProcessesAndStatus(){
		System.out.println("The ready list is: \n" + this.RL);
		printWaitingList();
	}
	
	public void printWaitingList(){
		System.out.println("The waiting list is :");
		for(RCB rcb : this.allResources){
			if(!rcb.getWaitingList().isEmpty())
				System.out.print(rcb.getWaitingList() + "	");
		}
		System.out.println();
	}
	
	public void listAllResourcesAndStatus(){
		
	}
}
