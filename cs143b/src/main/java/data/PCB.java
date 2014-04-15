package data;

import java.util.LinkedList;

public class PCB {
	enum Type{
		READY, WAITING
	}
	class Status{
		Type type;
		ReadyList RL;
	}
	class CreationTree{
		PCB parent;
		LinkedList<PCB> children;
	}
	private char pid;
	private LinkedList<RCB> RList;
	private Status status;
	private CreationTree creationTree;
	private int priority;
	
	public PCB(char pid, int priority){
		this.pid = pid;
		this.priority = priority;
	}
}
