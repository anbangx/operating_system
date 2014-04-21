package data;

import java.util.Iterator;
import java.util.LinkedList;

public class PRManager {
	private PCB runningProcess;
	private LinkedList<RCB> allResources;
	private RCB IO;
	private ReadyList RL;

	public PRManager() {
		init();
		System.out.println("Init is running");
	}

	public void init() {
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

	public String execute(String line) {
		boolean flag = true;
		String[] tokens = line.split(" ");
		String command = tokens[0];
		if (command.equals("")) {
			System.out.println();
			return "";
		}
		if (command.equals("init")) {
			init();
		} else if (command.equals("cr")) {
			if (tokens.length != 3) {
				System.out.println("Please provide correct format, ex. cr A 1");
				return "error";
			}
			flag = createProcess(tokens[1], Integer.parseInt(tokens[2]));
		} else if (command.equals("de")) {
			if (tokens.length != 2) {
				System.out.println("Please provide correct format, ex. de B");
				return "error";
			}
			flag = destroyProcess(tokens[1]);
		} else if (command.equals("to")) {
			timeOut();
		} else if (command.equals("req")) {
			if (tokens.length != 2 && tokens.length != 3) {
				System.out
						.println("Please provide correct format, ex. req R1 or req R1 2");
				return "error";
			}
			if (tokens.length == 2)
				flag = requestResource(tokens[1], 1);
			else
				flag = requestResource(tokens[1], Integer.parseInt(tokens[2]));
		} else if (command.equals("rel")) {
			if (tokens.length != 2 && tokens.length != 3) {
				System.out
						.println("Please provide correct format, ex. rel R1 or rel R1 2");
				return "error";
			}
			if (tokens.length == 2)
				flag = releaseResource(tokens[1], 1, true);
			else
				flag = releaseResource(tokens[1], Integer.parseInt(tokens[2]), true);
		} else if (command.equals("rio")) {
			if (tokens.length != 1) {
				System.out.println("Please provide correct format, ex. rio");
				return "error";
			}
			requestIO();
		} else if (command.equals("ioc")) {
			if (tokens.length != 1) {
				System.out.println("Please provide correct format, ex. ioc");
				return "error";
			}
			IOCompletion();
		} else if (command.equals("lsp")) {
			listAllProcessesAndStatus();
		} else if (command.equals("lsr")) {
			listAllResourcesAndStatus();
		} else if (command.equals("infop")) {
			printProcessInfo(tokens[1]);
		} else if (command.equals("infor")) {
			printResourceInfo(tokens[1]);
		}
		return flag ? printCurrentRunningProcess() : "error";
	}

	public boolean createProcess(String pid, int priority) {
		if (getPCB(pid) != null) {
			System.out.println("Error. Process " + pid + " already existed!");
			return false;
		}
		// 1. create PCB data structure
		PCB pcb = new PCB(pid, priority);

		// 2. link PCB to creation tree
		this.runningProcess.getCreationTree().children.add(pcb);
		pcb.getCreationTree().parent = this.runningProcess;

		// 3. insert PCB to RL
		this.RL.insert(pcb);

		// 4. reschedule
		this.scheduler();

		return true;
	}

	public boolean destroyProcess(String pid) {
		PCB pcb = getPCB(pid);
		if (pcb == null) {
			System.out.println("Error. Process " + pid + " doesn't exist or it's not subprocess!");
			return false;
		}
		killTree(pcb);
		this.scheduler();

		return true;
	}

	public void killTree(PCB pcb) {
		for (PCB child : pcb.getCreationTree().children) {
			killTree(child);
		}
		pcb.getCreationTree().children.clear();
//		pcb.getCreationTree().parent.getCreationTree().children.remove(pcb);

		// update pointer
		if (this.runningProcess != null && this.runningProcess.equals(pcb))
			this.runningProcess = null;
		else
			this.RL.remove(pcb);
		// remove from waiting list
		for (RCB rcb : this.allResources) {
			rcb.getWaitingList().remove(new Waiting(pcb, 1));
		}

		// free resources
		for (Inventory inventory : pcb.getResourceList())
			releaseResource(inventory.rcb.getRid(), inventory.amount, false);

		pcb = null;
	}

	public PCB getPCB(String pid) {
		if(!PCB.isChild(this.runningProcess, pid)){
			return null;
		}
		PCB pcb = null;
		if (this.runningProcess.getPid().equals(pid))
			pcb = this.runningProcess;
		else {
			pcb = this.RL.get(pid);
		}
		if (pcb == null)
			pcb = getPCBFromWaitingList(pid);
		return pcb;
	}

	public PCB getPCBFromWaitingList(String pid) {
		for (RCB rcb : this.allResources) {
			Iterator<Waiting> it = rcb.getWaitingList().iterator();
			while (it.hasNext()) {
				Waiting waiting = it.next();
				if (waiting.pcb.getPid().equals(pid))
					return waiting.pcb;
			}
		}
		return null;
	}

	public boolean requestResource(String rid, int amount) {
		// 1. get the corresponding RCB using rid
		RCB rcb = this.getRCB(rid);
		if(rcb == null){
			System.out.println("Resource " + rid + " doesn't exist!");
			return false;
		}
		Inventory inventory = this.runningProcess.getInventory(rid);
		int oldAmount = inventory != null ? inventory.amount : 0;
		if (rcb.getTotal() < amount + oldAmount) {
			System.out.println("Error. Larger than total resource.");
			return false;
		}
		// 2. check if there is enough available resource for this resource
		if (rcb.getAvailable() >= amount) { // r has enough available resources
			// 3.1. rcb update available resource
			rcb.request(amount);
			
			// 3.2 insert into running->RList
			this.runningProcess.getResourceList().add(
					new Inventory(rcb, amount));
		} else { // otherwise
			// 3.1 running->status = waiting
			this.runningProcess.getStatus().type = PCB.Type.WAITING;
			
			// 3.2 add running to waiting list 
			this.RL.remove(this.runningProcess);
			rcb.getWaitingList().add(new Waiting(this.runningProcess, amount));
			scheduler();
		}
		return true;
	}

	public boolean releaseResource(String rid, int amount,
			boolean releaseByRunningProcess) {
		// 1. get the corresponding RCB using rid
		RCB rcb = null;
		if (releaseByRunningProcess) {
			Inventory inventory = this.runningProcess.getInventory(rid);
			if (inventory == null || inventory.rcb == null) {
				System.out
						.println("Error. Release "
								+ rid
								+ " failed, the current running process doesn't hold this resource!");
				return false;
			}
			rcb = inventory.rcb;
			if (amount > inventory.amount) {
				System.out
						.println("Error. The current running process only holds "
								+ inventory.amount + " " + rid + "!");
				return false;
			} else if (amount == inventory.amount) {
				this.runningProcess.getResourceList().remove(rcb);
			} else {
				inventory.amount -= amount;
			}
		} else { // release when destroy the process
			for (RCB resource : this.allResources) {
				if (resource.getRid().equals(rid)) {
					rcb = resource;
					break;
				}
			}
			if (rcb == null) {
				System.out.println("Error.Release " + rid
						+ " failed, this resource doesn't exist!");
				return false;
			}
		}
		LinkedList<Waiting> waitingList = rcb.getWaitingList();
		rcb.release(amount);
		// 2. check if any PCB is waiting for this resource
		if (!waitingList.isEmpty()) { // if yes,
			// 3. get the first one from waiting list
			int total = rcb.getAvailable();
			PCB pcb = rcb.removeFirstFromWaitingList();
			if (pcb != null) {
				// 4. change PCB status
				pcb.getStatus().type = PCB.Type.READY;
				pcb.getStatus().RL = this.RL;
				
				// 5. insert into Ready List
				this.RL.insert(pcb);
				
				// 6. add resource to PCB->RList
				pcb.getResourceList().add(
						new Inventory(rcb, total - rcb.getAvailable()));
			}
			scheduler();
		}
		return true;
	}

	public RCB getRCB(String rid) {
		Iterator<RCB> it = this.allResources.iterator();
		while (it.hasNext()) {
			RCB rcb = it.next();
			if (rcb.getRid().equals(rid))
				return rcb;
		}
		return null;
	}

	public void requestIO() {
		this.runningProcess.getStatus().type = PCB.Type.WAITING;
		this.runningProcess.getStatus().RL = null;
		this.RL.remove(this.runningProcess);
		this.IO.getWaitingList().add(new Waiting(this.runningProcess, 1));
		scheduler();
	}

	public void IOCompletion() {
		PCB pcb = this.IO.removeFirstFromWaitingList();
		pcb.getStatus().type = PCB.Type.READY;
		pcb.getStatus().RL = this.RL;
		this.RL.insert(pcb);
		this.IO.release(1);
		scheduler();
	}

	// function call simulate time out
	public void timeOut() {
		// 1. insert q into Ready List
		this.RL.insert(this.runningProcess);

		// 2. change q.status to ready
		this.runningProcess.getStatus().type = PCB.Type.READY;

		// 3. reschedule
		this.scheduler();
	}

	public void scheduler() {
		// 1. find highest priority process
		PCB highest = RL.getPCBWithHighPriority();

		// 2. under some conditions, do context switch
		if (this.runningProcess == null // (3) destroy
				|| this.runningProcess.getPriority() < highest.getPriority() // (1)
																				// create/release
				|| this.runningProcess.getStatus().type != PCB.Type.RUNNING // (2)
																			// request/time-out
		)
			preempt(highest);
	}

	public void preempt(PCB highest) {
		// (1)
		if (this.runningProcess != null
				&& this.runningProcess.getStatus().type == PCB.Type.RUNNING) {
			this.runningProcess.getStatus().type = PCB.Type.READY;
			this.RL.insert(this.runningProcess);
		}
		highest.getStatus().type = PCB.Type.RUNNING;
		this.runningProcess = highest;
		this.RL.remove(highest);
	}

	public String printCurrentRunningProcess() {
		String ans = this.runningProcess.getPid() + " is running";
		System.out.println(ans);
		return ans;
	}

	public void listAllProcessesAndStatus() {
		System.out.println("The ready list is: \n" + this.RL);
		printWaitingList();
	}

	public void printWaitingList() {
		System.out.println("The waiting list is :");
		boolean flag = false;
		for (RCB rcb : this.allResources) {
			if (!rcb.getWaitingList().isEmpty()) {
				flag = true;
				System.out.print(rcb.getWaitingList() + "	");
			}
		}
		if (!flag)
			System.out.println();
	}

	public void listAllResourcesAndStatus() {
		for (RCB rcb : this.allResources)
			System.out.println(rcb.getInfo());
		System.out.println(IO.getInfo());
	}

	public void printProcessInfo(String pid) {
		PCB pcb = getPCB(pid);
		System.out.println(pcb.getInfo());
	}

	public void printResourceInfo(String rid) {
		RCB rcb = this.getRCB(rid);
		System.out.println(rcb.getInfo());
	}
}
