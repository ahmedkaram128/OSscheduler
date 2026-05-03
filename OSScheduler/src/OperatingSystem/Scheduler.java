package OperatingSystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import GUI.DashboardController;

public class Scheduler {
	Execution exec;
	MutexController mc;
	public List<Queue<Process>> MLFQ;
	Memory memory;
	public int RRQuantum;
	public int time;
	public SMode mode ;
	List<Process> processesInMemory;
	public int currentQuantumUsed ;
	public Process currentProcess ; 
	public boolean waitingForInput = false;
	public Process inputProcess = null;
	public String inputVariable = null;
	public String pendingInputValue = null;
	public String currentInstructionText = "-";
	public Process lastShownProcess = null;
	public List<Process> finishedProcess;
	public boolean running;
	public int MLFQTime;
	public int runningProcessQueue;
	public Process runningProcess;
	public int currentMLFQLevel = -1;
	
	public Scheduler(MutexController mc , Memory memory, int RRQuantum , DashboardController dc) {
		currentQuantumUsed = 0;
		currentProcess = null;
		MLFQ = new ArrayList<>();
		processesInMemory = new ArrayList<>();
		finishedProcess = new ArrayList<>();
		exec = new Execution(dc);
		this.mc = mc;
		this.memory = memory;
		this.RRQuantum = RRQuantum;

		for(int i = 0 ; i < 4 ; i++) {
			MLFQ.add(new LinkedList<>());
		}
		time = 0;
	}
	
	public void setMutexController(MutexController mc) {
	    this.mc = mc;
	}
	
	public void printQueues() { //used in console only
		ArrayList<Process> readyQueue = new ArrayList<>(ReadyQueue.ReadyQueue);
		ArrayList<Process> blockedQueue = new ArrayList<>(BlockedQueue.BlockedQueue);
		System.out.println("Ready Queue Contents:");
		for(Process p : readyQueue) {
			System.out.println(p.pcb.ID);
		}
		System.out.println("Blocked Queue Contents:");
		for(Process p : blockedQueue) {
			System.out.println(p.pcb.ID);
		}
	}
	
	public void processArrivals(List<Process> newProcesses) {
		for (int i = 0; i < newProcesses.size(); i++) {
            if (newProcesses.get(i).arrivalTime == time) {
            	newProcesses.get(i).parseCode();
            	boolean inMemory = memory.allocate(newProcesses.get(i));
                ReadyQueue.ReadyQueue.add(newProcesses.get(i));
                exec.x.printToSwap("[Time " + time + "] Process " + newProcesses.get(i).pcb.ID + " entered the ready queue");
                if(inMemory){
                	processesInMemory.add(newProcesses.get(i));
                }
                newProcesses.remove(i);
                i--;
            }
        }
	}
	
	public void submitInputValue(String value, List<Process> newProcesses) {
	    if (!waitingForInput || inputProcess == null || inputVariable == null) {
	        return;
	    }

	    Process p = inputProcess;
	    currentInstructionText = "assign " + inputVariable + " input";
	    lastShownProcess = p;

	    Memory.storeVariable(p, inputVariable, value);
	    p.pcb.PC++;
	    memory.updatePCB(p);
	    if(mode == SMode.RR || mode == SMode.MLFQ)
	    	currentQuantumUsed++;
	    time++;
	    
	    if(mode == SMode.RR || mode == SMode.HRRN){
		    processArrivals(newProcesses);
	    }
	    else{
	    	processArrivalsMLFQ(newProcesses);
	    }

	    waitingForInput = false;
	    inputProcess = null;
	    inputVariable = null;
	    pendingInputValue = null;

	    if (p.pcb.PC >= p.instructions.size()) {
	        p.pcb.state = State.Finished;
	        memory.updatePCB(p);
	        finishedProcess.add(p);
//	        System.out.println("[Time " + time + "] P" + p.pcb.ID + " FINISHED");
	        exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " FINISHED");
	        lastShownProcess = p;
	        currentProcess = null;
	        currentQuantumUsed = 0;
	    } else if (p.pcb.state == State.Blocked) {
//	        System.out.println("[Time " + time + "] P" + p.pcb.ID + " BLOCKED");
	        exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " BLOCKED");
	        lastShownProcess = p;
	        currentProcess = null;
	        currentQuantumUsed = 0;
	    } else if (mode == SMode.RR && currentQuantumUsed >= RRQuantum) {
	        p.pcb.state = State.Ready;
	        memory.updatePCB(p);
	        ReadyQueue.ReadyQueue.add(p);
//	        System.out.println("[Time " + time + "] P" + p.pcb.ID + " back to ready queue");
	        exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " back to ready queue");
	        lastShownProcess = p;
	        currentProcess = null;
	        currentQuantumUsed = 0;
	    }
	    else if (mode == SMode.MLFQ && currentQuantumUsed >= getMLFQQuantum(currentMLFQLevel)) {
	        p.pcb.state = State.Ready;
	        memory.updatePCB(p);

	        int nextLevel = currentMLFQLevel;

	        if (currentMLFQLevel < 3) {
	            nextLevel = currentMLFQLevel + 1;
	        }

	        MLFQ.get(nextLevel).add(p);

//	        System.out.println("[Time " + time + "] P" + p.pcb.ID +
//	                " quantum finished after input, moved to Q" + nextLevel);
	        exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID +
	                " quantum finished after input, moved to Q" + nextLevel);
	        lastShownProcess = p;
	        currentProcess = null;
	        currentQuantumUsed = 0;
	        currentMLFQLevel = -1;
	    }
	}
	
	public void loadFromDisk(Process p) {
	    ArrayList<String> data = getDataDisk(p);

	    if (data == null || data.isEmpty()) {
	        throw new RuntimeException("No disk data found for P" + p.pcb.ID);
	    }

	    int loadStart = memory.getStart();

	    if (loadStart + data.size() > Memory.memory.length) {
	        throw new RuntimeException("Not enough space to load P" + p.pcb.ID);
	    }

	    for (int i = 0; i < data.size(); i++) {
	        Memory.memory[loadStart + i] = data.get(i);
	    }

	    p.pcb.start = loadStart;
	    p.pcb.end = loadStart + data.size() - 1;

	    memory.updatePCB(p);
	    processesInMemory.add(p);
	    removeProcessFromDisk(p.pcb.ID);
	    memory.recomputeStart();

//	    System.out.println("Loaded P" + p.pcb.ID + " from disk");
	    exec.x.printToSwap("Loaded P" + p.pcb.ID + " from disk");
	}
	
    public void stepRR(List<Process> newProcesses) {
    	if(waitingForInput){
    		return;
    	}
    	processArrivals(newProcesses);
 
        if (currentProcess == null) {
            if (ReadyQueue.ReadyQueue.isEmpty()) {
//                System.out.println("[Time " + time + "] CPU IDLE");
                exec.x.printToSwap("[Time " + time + "] CPU IDLE");
                time++;
                processArrivals(newProcesses);
                return;
            }

            Process temp = ReadyQueue.ReadyQueue.peek();

            if (temp.pcb.start == -1) {
                if (memory.canFit(temp)) {
                    loadFromDisk(temp);
                    exec.x.printToSwap("Loaded P" + temp.pcb.ID + " from memory");
//                    System.out.println("[Time " + time + "] Loaded P" + temp.pcb.ID + " without swap");
                    return;
                } else {
                    ArrayList<Process> list = new ArrayList<>(processesInMemory);
                    Process out = getProcessByID(list, getLastMemoryProcess());

                    if (out == null) {
                        throw new RuntimeException("No resident process available to swap out for P" + temp.pcb.ID);
                    }

                    swap(out, temp);
//                    System.out.println("[Time " + time + "] Swap done for P" + temp.pcb.ID);
                    return;
                }
            }

            currentProcess = ReadyQueue.ReadyQueue.remove();
            currentProcess.pcb.state = State.Running;
            memory.updatePCB(currentProcess);
            currentQuantumUsed = 0;

//            System.out.println("[Time " + time + "] Running P" + currentProcess.pcb.ID);
            exec.x.printToSwap("[Time " + time + "] Running P" + currentProcess.pcb.ID);
        }

        Process p = currentProcess;

        if (p.pcb.PC < p.instructions.size()) {
        	String instruction = p.instructions.get(p.pcb.PC);
        	currentInstructionText = instruction;
        	lastShownProcess = p;
        	System.out.println("  [P" + p.pcb.ID + " PC=" + p.pcb.PC + "] " + instruction);

            boolean completed = exec.execute(p, instruction, mc , this);
            
            if(waitingForInput){
            	memory.updatePCB(p);
                return;
            }
            if (completed) {
                p.pcb.PC++;
            }
            
            memory.updatePCB(p);
            currentQuantumUsed++;
            time++;

            processArrivals(newProcesses);
        }

        if (p.pcb.PC >= p.instructions.size()) {
            p.pcb.state = State.Finished;
            processesInMemory.remove(p);
	        finishedProcess.add(p);
            memory.deAllocate(p, processesInMemory);
            lastShownProcess = p;
//            System.out.println("[Time " + time + "] P" + p.pcb.ID + " FINISHED");
            exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " FINISHED");
            currentProcess = null;
            currentQuantumUsed = 0;
        }
        else if (p.pcb.state == State.Blocked) {
            lastShownProcess = p;
//            System.out.println("[Time " + time + "] P" + p.pcb.ID + " BLOCKED");
            exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " BLOCKED");
            currentProcess = null;
            currentQuantumUsed = 0;
        }else if (currentQuantumUsed >= RRQuantum) {
            p.pcb.state = State.Ready;
            memory.updatePCB(p);
            ReadyQueue.ReadyQueue.add(p);
//            System.out.println("[Time " + time + "] P" + p.pcb.ID + " back to ready queue");
            exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " back to ready queue");
            lastShownProcess = p;
            currentProcess = null;
            currentQuantumUsed = 0;
        }
    }
    public void stepHRRN(List<Process> newProcesses) {
        if (waitingForInput) return;

        processArrivals(newProcesses);

        if (currentProcess == null) {
            if (ReadyQueue.ReadyQueue.isEmpty()) {
//                System.out.println("[Time " + time + "] CPU IDLE");
                exec.x.printToSwap("[Time " + time + "] CPU IDLE");
                time++;
                processArrivals(newProcesses);
                return;
            }

            Process chosen = calculateHRR();
            if (chosen == null) { time++; return; }

            if (chosen.pcb.start == -1) {
                if (memory.canFit(chosen)) {
                    loadFromDisk(chosen);
                } else {
                    Process out = getProcessByID(
                        new ArrayList<>(processesInMemory), getLastMemoryProcess());
                    swap(out, chosen);
                }
                return;
            }

            ReadyQueue.ReadyQueue.remove(chosen);
            currentProcess = chosen;
            currentProcess.pcb.state = State.Running;
            memory.updatePCB(currentProcess);
//            System.out.println("[Time " + time + "] Running P" + currentProcess.pcb.ID);
            exec.x.printToSwap("[Time " + time + "] Running P" + currentProcess.pcb.ID);
            printQueues();
        }

        Process p = currentProcess;

        if (p.pcb.PC < p.instructions.size()) {
            String instruction = p.instructions.get(p.pcb.PC);
            currentInstructionText = instruction;
            lastShownProcess = p;
            System.out.println("  [P" + p.pcb.ID + " PC=" + p.pcb.PC + "] " + instruction);

            boolean completed = exec.execute(p, instruction, mc, this);
            if (waitingForInput) { memory.updatePCB(p); return; }
            if (completed) p.pcb.PC++;

            memory.updatePCB(p);
            time++;
            processArrivals(newProcesses);
        }

        if (p.pcb.PC >= p.instructions.size()) {
            p.pcb.state = State.Finished;
            processesInMemory.remove(p);
            finishedProcess.add(p);
            memory.deAllocate(p, processesInMemory);
//            System.out.println("[Time " + time + "] P" + p.pcb.ID + " FINISHED");
            exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " FINISHED");
            printQueues();
            currentProcess = null;
        } else if (p.pcb.state == State.Blocked) {
//            System.out.println("[Time " + time + "] P" + p.pcb.ID + " BLOCKED");
            exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " BLOCKED");
            printQueues();
            currentProcess = null;
        }
    }	
	
	public int getLastMemoryProcess() {
		int x = -1;
		for(int i = Memory.memory.length - 1 ; i >= 0 ; i--) {
			if(Memory.memory[i] != null) {
				x = Integer.parseInt(((Memory.memory[i]).split(" ")[0]).replace("P", ""));
				return x;
			}
		}
		return x;
	}
	
	public Process getProcessByID(ArrayList<Process> list, int id) {
	    for (Process p : list) {
	        if (p.pcb.ID == id) {
	            return p;
	        }
	    }
	    return null; 
	}
	
	public void swap(Process swapOut, Process swapIn) {
	    if (swapOut == null || swapIn == null) {
	        throw new RuntimeException("swapOut or swapIn is null");
	    }

	    ArrayList<String> swapOutData = getDataMemory(swapOut);
	    ArrayList<String> swapInData = getDataDisk(swapIn);

	    if (swapInData == null || swapInData.isEmpty()) {
	        throw new RuntimeException("No disk data found for P" + swapIn.pcb.ID);
	    }

	    removeProcessFromDisk(swapOut.pcb.ID);
	    writeProcessToDisk(swapOutData);

	    processesInMemory.remove(swapOut);

	    memory.deAllocate(swapOut, processesInMemory);

	    int loadStart = memory.getStart();

	    if (loadStart + swapInData.size() > Memory.memory.length) {
	        throw new RuntimeException("Not enough space to swap in P" + swapIn.pcb.ID);
	    }

	    for (int i = 0; i < swapInData.size(); i++) {
	        Memory.memory[loadStart + i] = swapInData.get(i);
	    }

	    swapIn.pcb.start = loadStart;
	    swapIn.pcb.end = loadStart + swapInData.size() - 1;

	    memory.updatePCB(swapIn);

	    processesInMemory.add(swapIn);

	    removeProcessFromDisk(swapIn.pcb.ID);

	    memory.recomputeStart();
	    exec.x.printToSwap("[Time: " + time + "] Swapped Process " + swapOut.pcb.ID + " with Process " + swapIn.pcb.ID);
	    
//	    System.out.println("Swapped out P" + swapOut.pcb.ID);
//	    System.out.println("Swapped in P" + swapIn.pcb.ID);
	}
	
	public void removeProcessFromDisk(int processID) {
	    try {
	        BufferedReader reader =
	            new BufferedReader(new FileReader("src/OperatingSystem/ExtraSpace.txt"));

	        ArrayList<String> kept = new ArrayList<>();
	        String line;
	        boolean skipping = false;

	        while ((line = reader.readLine()) != null) {
	            if (line.equals("BEGIN P" + processID)) {
	                skipping = true;
	                continue;
	            }

	            if (line.equals("END P" + processID)) {
	                skipping = false;
	                continue;
	            }

	            if (!skipping) {
	                kept.add(line);
	            }
	        }

	        reader.close();

	        BufferedWriter writer =
	            new BufferedWriter(new FileWriter("src/OperatingSystem/ExtraSpace.txt", false));

	        for (String s : kept) {
	            writer.write(s);
	            writer.newLine();
	        }

	        writer.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public void writeProcessToDisk(ArrayList<String> data) {
	    try {
	        if (data == null || data.isEmpty()) return;

	        BufferedWriter writer =
	            new BufferedWriter(new FileWriter("src/OperatingSystem/ExtraSpace.txt", true));

	        String processId = data.get(0).split(" ")[0]; 

	        writer.write("BEGIN " + processId);
	        writer.newLine();

	        for (String line : data) {
	            writer.write(line == null ? "null" : line);
	            writer.newLine();
	        }

	        writer.write("END " + processId);
	        writer.newLine();

	        writer.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public ArrayList<String> getDataMemory(Process p){
		ArrayList<String> data = new ArrayList<>();
		for(int i = p.pcb.start ; i <= p.pcb.end ; i++) {
			data.add(Memory.memory[i]);
		}
		return data;
	}
	
	public ArrayList<String> getDataDisk(Process p) {
	    ArrayList<String> data = new ArrayList<>();
	    int processID = p.pcb.ID;

	    try {
	        BufferedReader reader =
	            new BufferedReader(new FileReader("src/OperatingSystem/ExtraSpace.txt"));

	        String line;
	        boolean collecting = false;

	        while ((line = reader.readLine()) != null) {
	            if (line.equals("BEGIN P" + processID)) {
	                collecting = true;
	                continue;
	            }

	            if (line.equals("END P" + processID)) {
	                break;
	            }

	            if (collecting) {
	                data.add(line);
	            }
	        }

	        reader.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    return data;
	}
	
	public Process calculateHRR() {
		ArrayList<Process> list = new ArrayList<>(ReadyQueue.ReadyQueue);
		Process highest = null;
		if(!list.isEmpty()) {
			highest = list.get(0);
			Process p;
			double burstTime; 
			double waitingTime;
			double RRHighest;
			double RRNext;
			for(int i = 1 ; i < list.size() ; i++) {
				p = list.get(i);
				burstTime = p.instructions.size();
				waitingTime = time - p.arrivalTime;
				RRNext = (double) ((burstTime + waitingTime) / burstTime) ;
				burstTime = highest.instructions.size();
				waitingTime = time - highest.arrivalTime;
				RRHighest = (double) ((burstTime + waitingTime) / burstTime);
				if(RRHighest < RRNext) {
					highest = p;
				}
//				System.out.println(Math.max(RRHighest,RRNext));
			}
		}
		return highest;
	}
	
	
	public void stepMLFQ(List<Process> newProcesses) {
	    if (waitingForInput) {
	        return;
	    }


	    processArrivalsMLFQ(newProcesses);

	    if (currentProcess == null) {
	        Process next = null;
	        int level = -1;

	        for (int i = 0; i < 4; i++) {
	            if (!MLFQ.get(i).isEmpty()) {
	                next = MLFQ.get(i).peek();
	                level = i;
	                break;
	            }
	        }

	        if (next == null) {
	            System.out.println("[Time " + time + "] CPU IDLE");
	            exec.x.printToSwap("[Time " + time + "] CPU IDLE");
	            time++;
	            processArrivalsMLFQ(newProcesses);
	            return;
	        }

	        if (next.pcb.start == -1) {
	            if (memory.canFit(next)) {
	                loadFromDisk(next);
	                exec.x.printToSwap("[Time: " + time + "] Loaded P" + next.pcb.ID + " without swap");
//	                System.out.println("[Time " + time + "] Loaded P" + next.pcb.ID + " without swap");
	                return;
	            } else {
	                ArrayList<Process> list = new ArrayList<>(processesInMemory);
	                Process out = getProcessByID(list, getLastMemoryProcess());

	                if (out == null) {
	                    throw new RuntimeException("No resident process available to swap out for P" + next.pcb.ID);
	                }

	                swap(out, next);
//	                System.out.println("[Time " + time + "] Swap done for P" + next.pcb.ID);
	                return;
	            }
	        }

	        currentProcess = MLFQ.get(level).remove();
	        currentMLFQLevel = level;
	        currentQuantumUsed = 0;

	        currentProcess.pcb.state = State.Running;
	        memory.updatePCB(currentProcess);

//	        System.out.println("[Time " + time + "] Running P" + currentProcess.pcb.ID + " from Q" + currentMLFQLevel);
	        exec.x.printToSwap("[Time " + time + "] Running P" + currentProcess.pcb.ID + " from Q" + currentMLFQLevel);
	    }

	    Process p = currentProcess;

	    if (p.pcb.PC < p.instructions.size()) {
	        String instruction = p.instructions.get(p.pcb.PC);

	        currentInstructionText = instruction;
	        lastShownProcess = p;

	        System.out.println("  [P" + p.pcb.ID + " PC=" + p.pcb.PC + "] " + instruction);

	        boolean completed = exec.execute(p, instruction, mc, this);

	        if (waitingForInput) {
	            memory.updatePCB(p);
	            return;
	        }

	        if (completed) {
	            p.pcb.PC++;
	        }

	        memory.updatePCB(p);
	        currentQuantumUsed++;
	        time++;

	        processArrivalsMLFQ(newProcesses);
	    }

	    if (p.pcb.PC >= p.instructions.size()) {
	        p.pcb.state = State.Finished;
	        processesInMemory.remove(p);
	        finishedProcess.add(p);
	        memory.deAllocate(p, processesInMemory);
	        lastShownProcess = p;

//	        System.out.println("[Time " + time + "] P" + p.pcb.ID + " FINISHED");
	        exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " FINISHED");
	        currentProcess = null;
	        currentQuantumUsed = 0;
	        currentMLFQLevel = -1;
	    }

	    else if (p.pcb.state == State.Blocked) {
	        lastShownProcess = p;
//	        System.out.println("[Time " + time + "] P" + p.pcb.ID + " BLOCKED");
	        exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID + " BLOCKED");

	        currentProcess = null;
	        currentQuantumUsed = 0;
	        currentMLFQLevel = -1;
	    }

	    else if (currentQuantumUsed >= getMLFQQuantum(currentMLFQLevel)) {
	        p.pcb.state = State.Ready;
	        memory.updatePCB(p);

	        int nextLevel = currentMLFQLevel;

	        if (currentMLFQLevel < 3) {
	            nextLevel = currentMLFQLevel + 1;
	        }

	        MLFQ.get(nextLevel).add(p);

//	        System.out.println("[Time " + time + "] P" + p.pcb.ID +
//	                " quantum finished, moved to Q" + nextLevel);
	        exec.x.printToSwap("[Time " + time + "] P" + p.pcb.ID +
	                " quantum finished, moved to Q" + nextLevel);
	        lastShownProcess = p;
	        currentProcess = null;
	        currentQuantumUsed = 0;
	        currentMLFQLevel = -1;
	    }

	}
	
	public int getMLFQQuantum(int level) {
	    if (level == 0) return 1;
	    if (level == 1) return 2;
	    if (level == 2) return 4;
	    return 8; 
	}
	
	public void processArrivalsMLFQ(List<Process> newProcesses) {
		for (int i = 0; i < newProcesses.size(); i++) {
            if (newProcesses.get(i).arrivalTime == time) {
            	newProcesses.get(i).parseCode();
            	boolean inMemory = memory.allocate(newProcesses.get(i));
                MLFQ.get(0).add(newProcesses.get(i));
                exec.x.printToSwap("[Time " + time + "] Process " + newProcesses.get(i).pcb.ID + " entered Q0");
                if(inMemory){
                	processesInMemory.add(newProcesses.get(i));
                }
                newProcesses.remove(i);
                i--;
            }
        }
	}
	
}
