package OperatingSystem;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Process {
	String path;
	public ArrayList<String> instructions;
	public PCB pcb;
	public int arrivalTime;
	
	public Process(int ID , String path , int arrivalTime) {
		this.path = path;
		pcb = new PCB(ID);
		instructions = new ArrayList<>();
		this.arrivalTime = arrivalTime;
	}

	public void parseCode() {
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;

			while ((line = br.readLine()) != null) {
				if(!line.trim().isEmpty())
					instructions.add(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

