package OperatingSystem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Memory {
	static int size = 40;
	public static String[] memory;
	int start;
	
	public Memory() {
		memory = new String[size];
		start = 0;
	}
	
	public int recomputeStart() {
	    int lastUsed = -1;
	    for (int i = 0; i < memory.length; i++) {
	        if (memory[i] != null) {
	            lastUsed = i;
	        }
	    }
	    start = lastUsed + 1;
	    return start;
	}

	public int getStart() {
	    return recomputeStart();
	}
	
	public int getUsedSize() {
	    return recomputeStart();
	}

	public int getFreeSize() {
	    return size - getUsedSize();
	}

	public int getProcessSize(Process p) {
	    return 5 + p.instructions.size() + 3;
	}

	public boolean canFit(Process p) {
	    return getFreeSize() >= getProcessSize(p);
	}
	
	public boolean allocate(Process p) {
		if(start + (5 + p.instructions.size() + 3) <= size) {
			p.pcb.start = start;
			p.pcb.end = (start + 5 + p.instructions.size() + 3) - 1;
			memory[start] = "P" + p.pcb.ID + " PCB ID " + p.pcb.ID;
			memory[start + 1] = "P" + p.pcb.ID + " PCB State " + p.pcb.state;
			memory[start + 2] = "P" + p.pcb.ID + " PCB PC " + p.pcb.PC;
			memory[start + 3] = "P" + p.pcb.ID + " PCB Start " + p.pcb.start;
			memory[start + 4] = "P" + p.pcb.ID + " PCB End " + p.pcb.end;
			start = start + 5;
			for(int i = start ; i < p.instructions.size() + start ; i++) {
				memory[i] = "P" + p.pcb.ID + " Instruction " + (i-start) + " |" + p.instructions.get(i - start);
			}
			start += p.instructions.size();
			for(int i = start ; i < start + 3 ; i++) {
				memory[i] = "P" + p.pcb.ID + " Variable NULL | NULL";
			}
			start = p.pcb.end + 1;
			return true;
		}
		else {
			//TODO
			allocateOnDisk(p);
			return false;
		}
	}
	
	public void deAllocate(Process p, List<Process> processesInMemory) {
	    if (p == null || p.pcb.start == -1 || p.pcb.end == -1) {
	        return;
	    }

	    recomputeStart();

	    int removedStart = p.pcb.start;
	    int removedEnd = p.pcb.end;
	    int blockSize = removedEnd - removedStart + 1;

	    for (int i = removedEnd + 1; i < start; i++) {
	        memory[i - blockSize] = memory[i];
	    }

	    for (int i = start - blockSize; i < start; i++) {
	        memory[i] = null;
	    }

	    p.pcb.start = -1;
	    p.pcb.end = -1;

	    recomputeStart();

	    updateMovedProcesses(processesInMemory);
	}

	private void updateMovedProcesses(List<Process> processesInMemory) {
	    for (Process process : processesInMemory) {
	        if (process == null) {
	            continue;
	        }

	        int newStart = findProcessStart(process.pcb.ID);

	        if (newStart == -1) {
	            process.pcb.start = -1;
	            process.pcb.end = -1;
	            continue;
	        }

	        int blockSize = 5 + process.instructions.size() + 3;
	        int newEnd = newStart + blockSize - 1;

	        process.pcb.start = newStart;
	        process.pcb.end = newEnd;

	        memory[newStart] = "P" + process.pcb.ID + " PCB ID " + process.pcb.ID;
	        memory[newStart + 1] = "P" + process.pcb.ID + " PCB State " + process.pcb.state;
	        memory[newStart + 2] = "P" + process.pcb.ID + " PCB PC " + process.pcb.PC;
	        memory[newStart + 3] = "P" + process.pcb.ID + " PCB Start " + process.pcb.start;
	        memory[newStart + 4] = "P" + process.pcb.ID + " PCB End " + process.pcb.end;
	    }
	}

    private int findProcessStart(int processId) {
        for (int i = 0; i < start; i++) {
            if (memory[i] != null && memory[i].equals("P" + processId + " PCB ID " + processId)) {
                return i;
            }
        }
        return -1;
    }
	
	public void allocateOnDisk(Process p) {
	    try {
	        p.pcb.start = -1;
	        p.pcb.end = -1;

	        BufferedWriter writer =
	            new BufferedWriter(new FileWriter("src/OperatingSystem/ExtraSpace.txt", true));

	        writer.write("BEGIN P" + p.pcb.ID);
	        writer.newLine();

	        writer.write("P" + p.pcb.ID + " PCB ID " + p.pcb.ID);
	        writer.newLine();

	        writer.write("P" + p.pcb.ID + " PCB State " + p.pcb.state);
	        writer.newLine();

	        writer.write("P" + p.pcb.ID + " PCB PC " + p.pcb.PC);
	        writer.newLine();

	        writer.write("P" + p.pcb.ID + " PCB Start " + p.pcb.start);
	        writer.newLine();

	        writer.write("P" + p.pcb.ID + " PCB End " + p.pcb.end);
	        writer.newLine();

	        for (int i = 0; i < p.instructions.size(); i++) {
	            writer.write("P" + p.pcb.ID + " Instruction " + i + " |" + p.instructions.get(i));
	            writer.newLine();
	        }

	        for (int i = 0; i < 3; i++) {
	            writer.write("P" + p.pcb.ID + " Variable NULL | NULL");
	            writer.newLine();
	        }

	        writer.write("END P" + p.pcb.ID);
	        writer.newLine();

	        writer.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public static void storeVariable(Process p, String variable, String value) {
	    if (p == null || p.pcb.start == -1 || p.pcb.end == -1) {
	        return;
	    }

	    for (int i = p.pcb.end - 2; i <= p.pcb.end; i++) {
	        if (memory[i] == null) continue;

	        String[] parts = memory[i].split(" ");
	        if (parts.length >= 5) {
	            String varName = parts[2];
	            if (varName.equals(variable)) {
	                memory[i] = "P" + p.pcb.ID + " Variable " + variable + " |" + value;
	                return;
	            }
	        }
	    }

	    for (int i = p.pcb.end - 2; i <= p.pcb.end; i++) {
	        if (memory[i] == null) continue;

	        String[] parts = memory[i].split(" ");
	        if (parts.length >= 5) {
	            String varName = parts[2];
	            if (varName.equals("NULL")) {
	                memory[i] = "P" + p.pcb.ID + " Variable " + variable + " |" + value;
	                return;
	            }
	        }
	    }
	}
	
	public void updatePCB(Process p) {
		if(p.pcb.start != -1) {
			memory[p.pcb.start] = "P" + p.pcb.ID + " PCB ID " + p.pcb.ID;
			memory[p.pcb.start + 1] = "P" + p.pcb.ID + " PCB State " + p.pcb.state;
			memory[p.pcb.start + 2] = "P" + p.pcb.ID + " PCB PC " + p.pcb.PC;
			memory[p.pcb.start + 3] = "P" + p.pcb.ID + " PCB Start " + p.pcb.start;
			memory[p.pcb.start + 4] = "P" + p.pcb.ID + " PCB End " + p.pcb.end;
		}
	}
	
	public static String[] getMemory() {
		return memory;
	}
}
