package data;

import java.util.Iterator;
import java.util.LinkedList;

public class PRManager {
	private PCB runningProcess;
	private LinkedList<RCB> allResources;
	private RCB IO;
	private ReadyList RL;
	
	public PRManager(){
		init();
		System.out.println("Process Init is running");
	}
	
	public void init(){
		this.runningProcess = new PCB("Init", 0);
		this.runningProcess.getStatus().type = PCB.Type.RUNNING;
		allResources = new LinkedList<RCB>();
		allResources.add(new RCB("R1", 1));
		allResources.add(new RCB("R2", 1));
		allResources.add(new RCB("R3", 1));
		allResources.add(new RCB("R4", 1));
		RL = new ReadyList();
		IO = new RCB("IO", 1);
	}
	
	public void execute(String line){
		String[] tokens = line.split(" ");
		String command = tokens[0];
		if (command.equals("")) {
			System.out.println();
			return;
		}
		if (command.equals("init")) {
			init();
		} else if (command.equals("cr")) {
			createProcess(tokens[1], Integer.parseInt(tokens[2]));
		} else if(command.equals("de")){ 
			destroyProcess(tokens[1]);
		} else if(command.equals("to")){ 
			timeOut();
		} else if(command.equals("req")){
			requestResource(tokens[1]);
		} else if(command.equals("rel")){
			releaseResource(tokens[1], true);
		} else if(command.equals("rio")){
			requestIO();
		} else if(command.equals("ioc")){
			IOCompletion();
		} else if(command.equals("lsp")){
			listAllProcessesAndStatus();
		} else if(command.equals("lsr")){
			listAllResourcesAndStatus();
		} else if(command.equals("infop")){
			printProcessInfo(tokens[1]);
		} else if(command.equals("infor")){
			printResourceInfo(tokens[1]);
		}
		printCurrentRunningProcess();
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
	}
	
	public void destroyProcess(String pid){
		PCB pcb = getPCB(pid);
		if(pcb == null){
			System.out.println("Process " + pid + " doesn't exist!");
			return;
		}
		killTree(pcb);
		this.scheduler();
	}
	
	public void killTree(PCB pcb){
		for(PCB child : pcb.getCreationTree().children){
			killTree(child);
		}
		
		// free resources
		for(RCB rcb : pcb.getResourceList())
			releaseResource(rcb.getRid(), false);
		
		// update pointer
		if(this.runningProcess != null && this.runningProcess.equals(pcb))
			this.runningProcess = null;
		else
			this.RL.remove(pcb);
		pcb = null;
	}
	
	public PCB getPCB(String pid){
		PCB pcb = null;
		if(this.runningProcess.getPid().equals(pid))
			pcb = this.runningProcess;
		else{
			pcb = this.RL.get(pid);
		}
		if(pcb == null)
			pcb = getPCBFromWaitingList(pid);
		return pcb;
	}
	
	public PCB getPCBFromWaitingList(String pid){
		for(RCB rcb : this.allResources){
			Iterator<PCB> it = rcb.getWaitingList().iterator();
			while(it.hasNext()){
				PCB pcb = it.next();
				if(pcb.getPid().equals(pid))
					return pcb;
			}
		}
		return null;
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
	}
	
	public void releaseResource(String rid, boolean releaseByRunningProcess){
		RCB rcb = null;
		if(releaseByRunningProcess){
			rcb = this.runningProcess.getRCB(rid);
			if(rcb == null){
				System.out.println("Release " + rid + " failed, the current running process doesn't hold this resource!");
				return;
			}
			this.runningProcess.getResourceList().remove(rcb);
		} else{ // release when destroy the process
			for(RCB resource : this.allResources){
				if(resource.getRid().equals(rid)){
					rcb = resource;
					break;
				}
			}
			if(rcb == null){
				System.out.println("Release " + rid + " failed, this resource doesn't exist!");
				return;
			}
		}
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
		// 1. insert q into Ready List
		this.RL.insert(this.runningProcess);
		
		// 2. change q.status to ready
		this.runningProcess.getStatus().type = PCB.Type.READY;
		
		// 3. reschedule
		this.scheduler();
	}
	
	public void scheduler(){
		// 1. find highest priority process
		PCB highest = RL.getPCBWithHighPriority();
		
		// 2. under some conditions, do context switch
		if(this.runningProcess == null	//(3) destroy
				|| this.runningProcess.getPriority() < highest.getPriority()	//(1) create/release
				|| this.runningProcess.getStatus().type != PCB.Type.RUNNING	//(2) request/time-out
				)	
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
		boolean flag = false;
		for(RCB rcb : this.allResources){
			if(!rcb.getWaitingList().isEmpty()){
				flag = true;
				System.out.print(rcb.getWaitingList() + "	");
			}
		}
		if(!flag)
			System.out.println();
	}
	
	public void listAllResourcesAndStatus(){
		for(RCB rcb : this.allResources)
			System.out.println(rcb.getInfo());
	}
	
	public void printProcessInfo(String pid){
		PCB pcb = getPCB(pid);
		System.out.println(pcb.getInfo());
	}
	
	public void printResourceInfo(String rid){
		RCB rcb = this.getRCB(rid);
		System.out.println(rcb.getInfo());
	}
}
