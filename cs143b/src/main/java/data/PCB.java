package data;

import java.util.Iterator;
import java.util.LinkedList;

public class PCB {
	enum Type{
		RUNNING, READY, WAITING
	}
	public class Status{
		Type type = Type.READY;
		ReadyList RL = null;
	}
	class CreationTree{
		PCB parent;
		LinkedList<PCB> children;
		
		public CreationTree(){
			parent = null;
			children = new LinkedList<PCB>();
		}
		
		public String toString(){
			return "Parent: " + parent.pid + ", Children: " + children.toString();
		}
	}
	private String pid;
	private LinkedList<RCB> resourceList;
	private Status status;
	private CreationTree creationTree;
	private int priority;
	
	public PCB(String pid, int priority){
		this.pid = pid;
		this.priority = priority;
		this.status = new Status();
		this.creationTree = new CreationTree();
		this.resourceList = new LinkedList<RCB>();
	}
	
	public RCB getRCB(String rid){
		Iterator<RCB> it = this.resourceList.iterator();
		while(it.hasNext()){
			RCB rcb = it.next();
			if(rcb.getRid().equals(rid))
				return rcb;
		}
		return null;
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

	public LinkedList<RCB> getResourceList() {
		return resourceList;
	}

	public void setRList(LinkedList<RCB> rList) {
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
