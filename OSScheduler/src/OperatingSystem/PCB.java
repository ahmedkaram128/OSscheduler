package OperatingSystem;

public class PCB {
	public int ID;
	public State state;
	public int PC;
	public int start;
	public int end;
	
	public PCB(int ID) {
		this.ID = ID; //is the ID given?
		this.PC = 0;
		this.state = State.Ready;
		this.start = -1;
		this.end = -1;
	}
}
