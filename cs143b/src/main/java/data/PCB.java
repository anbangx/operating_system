package data;

import java.util.LinkedList;

public class PCB {
	enum Type{
		RUNNING, READY, WAITING
	}
	public class Status{
		Type type = Type.WAITING;
		ReadyList RL = null;
	}
	class CreationTree{
		PCB parent;
		LinkedList<PCB> children;
		
		public CreationTree(){
			parent = null;
			children = new LinkedList<PCB>();
		}
	}
	private String pid;
	private LinkedList<RCB> RList;
	private Status status;
	private CreationTree creationTree;
	private int priority;
	
	public PCB(String pid, int priority){
		this.pid = pid;
		this.priority = priority;
		this.status = new Status();
		this.creationTree = new CreationTree();
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public LinkedList<RCB> getRList() {
		return RList;
	}

	public void setRList(LinkedList<RCB> rList) {
		RList = rList;
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
