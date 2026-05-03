package OperatingSystem;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import GUI.DashboardController;

public class Execution {
	public DashboardController x ;
	public Execution(DashboardController x){
		this.x = x;
	}
	
	
	public boolean execute(Process p , String instruction , MutexController mc , Scheduler sched) { //this now executes only one instruction
		String operation = getOperation(instruction);
		switch(operation) {
			case "print" : printVariable(instruction , p); return true;
			case "assign" : return assignVariable(instruction , p , sched);
			case "writeFile" : writeFile(instruction , p);return true;
			case "readFile" : readFile(instruction , p);return true;
			case "printFromTo" : printFromTo(instruction , p);return true;
			case "semWait" : {
			    String resource = instruction.split(" ")[1];
			    boolean accquired = false;
			    if (resource.equals("userInput"))
			        accquired = mc.semWait(mc.userInput, p);

			    else if (resource.equals("userOutput"))
			        accquired = mc.semWait(mc.userOutput, p);

			    else if (resource.equals("file"))
			        accquired = mc.semWait(mc.file, p);

			    return accquired;
			}
			case "semSignal" :{
				String resource = instruction.split(" ")[1];

			    if (resource.equals("userInput"))
			        mc.semSignal(mc.userInput);

			    else if (resource.equals("userOutput"))
			        mc.semSignal(mc.userOutput);

			    else if (resource.equals("file"))
			        mc.semSignal(mc.file);

			    return true;
			}
			default:
				return false;
		}
	}
	
	public String getOperation(String instruction) {
	    return instruction.split(" ")[0];
	}
	
	public void printVariable(String instruction , Process p) {
		String operand = instruction.split(" ")[1];
		String[] memory = Memory.getMemory();
		for(int i = p.pcb.start + 5 + p.instructions.size() ; i < p.pcb.end ; i++) {
			if(memory[i] != null && memory[i].split(" ")[2].equals(operand)) {
				x.printToConsole(memory[i].split("\\|")[1]);
			}
		}
	}
	
	public boolean assignVariable(String instruction, Process p, Scheduler sched) {
	    String[] parts = instruction.split(" ");
	    String variable = parts[1];
	    String input = parts[2];

	    if (input.equals("input")) {
	        if (!sched.waitingForInput) {
	            sched.waitingForInput = true;
	            sched.inputProcess = p;
	            sched.inputVariable = variable;
	            x.printToConsole("Please enter a value");
	        }
	        return false;
	    }

	    if (input.equals("readFile") && parts.length > 3) {
	        List<String> contents = readFile("readFile " + parts[3], p);
	        StringBuilder sb = new StringBuilder();
	        for (String s : contents) {
	            sb.append(s);
	        }
	        input = sb.toString();
	    }

	    Memory.storeVariable(p, variable, input);
	    return true;
	}
	
	public List<String> readFile(String instruction , Process p) {
		String[] string = instruction.split(" ");
		String[] memory = Memory.memory;
		String fileName = "";
		for(int i = p.pcb.start + 5 + p.instructions.size() ; i <= p.pcb.end ; i++) {
			if(memory[i] != null && (memory[i].split(" ")[2]).equals(string[1]) && (memory[i].split(" ")[1]).equals("Variable")) {
				fileName = memory[i].split("\\|")[1];
			}
		}

		List<String> lines = new ArrayList<>();
		try {
			lines = Files.readAllLines(Paths.get(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}
	
	public void writeFile(String instruction , Process p) {
		String[] string = instruction.split(" ");
		try {
			String path = "";
			String content = "";
			String[] memory = Memory.getMemory();
			for(int i = p.pcb.start + 5 + p.instructions.size() ; i <= p.pcb.end ; i++) {
				if(memory[i] != null && (memory[i].split(" ")[2]).equals(string[1]) && (memory[i].split(" ")[1]).equals("Variable")) {
					path = memory[i].split("\\|")[1];
				}
			}
			for(int i = p.pcb.start + 5 + p.instructions.size() ; i <= p.pcb.end ; i++) {
				if(memory[i] != null && (memory[i].split(" ")[2]).equals(string[2]) && (memory[i].split(" ")[1]).equals("Variable")) {
					content = memory[i].split("\\|")[1];
				}
			}
			Files.write(Paths.get(path), content.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void printFromTo(String instruction , Process p) {
		String[] string = instruction.split(" ");
		String operand1 = string[1];
		String operand2 = string[2];
		String value1 = "";
		String value2 = "";
		String[] memory = Memory.getMemory();
		for(int i = p.pcb.start + 5 + p.instructions.size() ; i <= p.pcb.end ; i++) {
			if(memory[i] != null && (memory[i].split(" ")[2]).equals(operand1) && (memory[i].split(" ")[1]).equals("Variable")) {
				value1 = (memory[i].split(" ")[3]).replace("|",""); 
			}
		}
		
		for(int i = p.pcb.start + 5 + p.instructions.size() ; i <= p.pcb.end ; i++) {
			if(memory[i] != null && (memory[i].split(" ")[2]).equals(operand2) && (memory[i].split(" ")[1]).equals("Variable")) {
				value2 = (memory[i].split(" ")[3]).replace("|", ""); 
			}
		}
		int start = Integer.parseInt(value1);
		int end = Integer.parseInt(value2);
		if(start <= end){
			StringBuilder sb = new StringBuilder();
			for (int i = start; i <= end; i++) {
			    sb.append(i).append(" ");
			}
			x.printToConsole(sb.toString().trim());
		}
		else{
			x.printToConsole("Start value cannot be greater than end value");
		}
	}
	
}
