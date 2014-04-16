package data;

import java.util.LinkedList;

public class PRManager {
	private PCB runningProcess;
	private LinkedList<RCB> allResources;
	private ReadyList RL;
	
	public PRManager(){
		this.runningProcess = new PCB("Init", 0);
		this.runningProcess.getStatus().type = PCB.Type.RUNNING;
		System.out.println("Init is running");
		allResources = new LinkedList<RCB>();
		allResources.add(new RCB("R1", 3));
		allResources.add(new RCB("R2", 2));
		RL = new ReadyList();
	}
	
	public void createProcess(String pid, int priority){
		// 1. create PCB data structure
		PCB pcb = new PCB(pid, priority);
		
		// 2. link PCB to creation tree
		this.runningProcess.getCreationTree().children.add(pcb);
		pcb.getCreationTree().parent = this.runningProcess;
		
		// 3. insert PCB to RL
		RL.hM.get(priority).add(pcb);
		
		// 4. reschedule
		this.scheduler();
	}
	
	public void scheduler(){
		// 1. find highest priority process
		PCB highest = RL.getPCBWithHighPriority();
		
		if(this.runningProcess.getPriority() < highest.getPriority()
				|| this.runningProcess.getStatus().type != PCB.Type.RUNNING
				|| this.runningProcess == null)
			preempt(highest);
	}
	
	public void preempt(PCB highest){
		if(this.runningProcess != null && this.runningProcess.getStatus().type == PCB.Type.RUNNING){
			this.runningProcess.getStatus().type = PCB.Type.READY;
		}
		this.runningProcess = highest;
		System.out.println("Process " + highest.getPid() + " is running");
	}
}
