package pr;

import java.util.Iterator;
import java.util.LinkedList;

public class PCB {
	public enum Type{
		RUNNING, READY, WAITING
	}
	public class Status{
		public Type type = Type.READY;
		public ReadyList RL = null;
	}
	public class CreationTree{
		public PCB parent;
		public LinkedList<PCB> children;
		
		public CreationTree(){
			parent = null;
			children = new LinkedList<PCB>();
		}
		
		public String toString(){
			return "Parent: " + parent.pid + ", Children: " + children.toString();
		}
	}
	private String pid;
	private LinkedList<Inventory> resourceList;
	private Status status;
	private CreationTree creationTree;
	private int priority;
	
	public PCB(String pid, int priority){
		this.pid = pid;
		this.priority = priority;
		this.status = new Status();
		this.creationTree = new CreationTree();
		this.resourceList = new LinkedList<Inventory>();
	}
	
	public Inventory getInventory(String rid){
		Iterator<Inventory> it = this.resourceList.iterator();
		while(it.hasNext()){
			Inventory inventory = it.next();
			if(inventory.rcb.getRid().equals(rid))
				return inventory;
		}
		return null;
	}
	
	public RCB getRCB(String rid){
		Iterator<Inventory> it = this.resourceList.iterator();
		while(it.hasNext()){
			Inventory inventory = it.next();
			if(inventory.rcb.getRid().equals(rid))
				return inventory.rcb;
		}
		return null;
	}
	
	public static boolean isChild(PCB root, String pid){
		if(root == null)
			return false;
		if(root.getPid().equals(pid))
			return true;
		for(PCB child : root.getCreationTree().children){
			if(isChild(child, pid))
				return true;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object other) {
		PCB pcb = (PCB)other;
		return this.pid.equals(pcb.pid);
	}
	public String toString(){
		return this.pid + " " + this.status.type;
	}
	
	public String getInfo(){
		return this.pid + ": [Status: " + this.status.type + "], [" + this.creationTree 
				+ "], ResourceList: " + this.resourceList + "]";
	}
	
	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public LinkedList<Inventory> getResourceList() {
		return resourceList;
	}

	public void setRList(LinkedList<Inventory> rList) {
		resourceList = rList;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public CreationTree getCreationTree() {
		return creationTree;
	}

	public void setCreationTree(CreationTree creationTree) {
		this.creationTree = creationTree;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	
}
