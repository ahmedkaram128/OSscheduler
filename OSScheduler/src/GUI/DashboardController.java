package GUI;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import OperatingSystem.*;
import OperatingSystem.Process;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardController {
		@FXML
		private Label algorithm;
		@FXML
		private Label quantum;
		@FXML
		private Button startButton;
		@FXML
		private Button stepButton;
		@FXML
		private Button resetButton;
		@FXML
		private Button pauseButton;
		@FXML
		private Button runAllButton;
		@FXML
		private ListView readyQueueListView;
		@FXML
		private ListView blockedQueueListView;
		@FXML
		private ListView resourceBlockedListView;
		@FXML
		private ListView finishedProcessesListView;
		@FXML
		private Label clockCycleLabel;
		@FXML
		private Label timeSliceLabel;
		@FXML
		private Label runningProcessLabel;
		@FXML
		private Label processStateLabel;
		@FXML
		private Label swapStatusLabel;
		@FXML
		private Label currentInstructionLabel;
		@FXML
		private Label pcLabel;
		@FXML
		private GridPane memoryGrid;
		@FXML
		private TextArea diskTextArea;
		@FXML
		private TextArea swapLogTextArea;
		@FXML
		private Label runningPidLabel;
		@FXML
		private Label runningStateLabel;
		@FXML
		private Label runningStartLabel;
		@FXML
		private Label runningEndLabel;
		@FXML
		private Label runningArrivalLabel;
		@FXML
		private Label userInputMutexLabel;
		@FXML
		private Label userOutputMutexLabel;
		@FXML
		private Label fileMutexLabel;
		@FXML
		private Label p1ArrivalLabel;
		@FXML
		private Label p2ArrivalLabel;
		@FXML
		private Label p3ArrivalLabel;
		@FXML
		private TextArea consoleOutputTextArea;
		@FXML
		private TextField userInput;
		@FXML
		private Button submitUserInput;
		@FXML
		private ListView eventListView;
		@FXML 
		private ListView<String> q0ListView;
		@FXML 
		private ListView<String> q1ListView;
		@FXML 
		private ListView<String> q2ListView;
		@FXML 
		private ListView<String> q3ListView;
		@FXML 
		private Tab mlfqTab;
		
		private Process P1;
		private Process P2;
		private Process P3;
		private Memory m ;
		private MutexController mc;
		private List<Process> newProcesses;
		private Scheduler sched;
		private Timeline runAllTimeline;
		private boolean isPaused = false;
		
		public void setDashboardData(String quantumValue, String algorithmValue) {
		    quantum.setText(quantumValue);
		    algorithm.setText(algorithmValue);
		    removeDisk();

		    if (!algorithmValue.equals("MLFQ")) {
		        mlfqTab.setDisable(true);
		        readyQueueListView.setVisible(true);
		        readyQueueListView.setManaged(true);
		    } else {
		        mlfqTab.setDisable(false);
		        readyQueueListView.setVisible(false);
		        readyQueueListView.setManaged(false);
		    }
		}
		
		public void removeDisk(){
			try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/OperatingSystem/ExtraSpace.txt", false))) {
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
		
		public void printToConsole(String args){
			consoleOutputTextArea.appendText(args + "\n");
		}
		
		public void printToSwap(String args){
			eventListView.getItems().add(args);
		}
		
		@FXML
        public void submitInput() {
            if (sched == null || !sched.waitingForInput) {
                return;
            }

            String value = userInput.getText().trim();
            if (value.isEmpty()) {
                return;
            }

            sched.submitInputValue(value, newProcesses);
            userInput.clear();

            updateUI();

            if (runAllTimeline != null && !isPaused) {
                runAllTimeline.play();
            }
        }
		
		public void setupProcesses(int P1Arrival , int P2Arrival , int P3Arrival , String P1Dest , String P2Dest , String P3Dest){
			p1ArrivalLabel.setText("Process 1: " + String.valueOf(P1Arrival));
			p2ArrivalLabel.setText("Process 2: " + String.valueOf(P2Arrival));
			p3ArrivalLabel.setText("Process 3: " + String.valueOf(P3Arrival));
			P1 = new Process(1 , P1Dest , P1Arrival);
			P2 = new Process(2 , P2Dest , P2Arrival);
			P3 = new Process(3 , P3Dest , P3Arrival);
			ReadyQueue.ReadyQueue.clear();
			BlockedQueue.BlockedQueue.clear();
			m = new Memory();
			newProcesses = new ArrayList<>();
			int q = Integer.parseInt(quantum.getText().trim());
			sched = new Scheduler(null, m, q, this);
			mc = new MutexController(sched);
			sched.setMutexController(mc);
			newProcesses.add(P1);
			newProcesses.add(P2);
			newProcesses.add(P3);
			if (algorithm.getText().equals("MLFQ")) {
			    sched.mode = SMode.MLFQ;
			    sched.processArrivalsMLFQ(newProcesses);
			} else {
			    sched.processArrivals(newProcesses);
			}

			updateUI();
		}
		
		@FXML
		public void runStep() {
		    if (sched.waitingForInput) {
		        printToConsole("Submit the input value first.");
		        return;
		    }

		    if (algorithm.getText().equals("RR")) {
		    	sched.mode = SMode.RR;
		        sched.stepRR(newProcesses);
		    } else if (algorithm.getText().equals("HRRN")) {
		    	sched.mode = SMode.HRRN;
		        sched.stepHRRN(newProcesses);
		    } else if (algorithm.getText().equals("MLFQ")){
		    	sched.mode = SMode.MLFQ;
		        sched.stepMLFQ(newProcesses);
		    }

		    updateUI();
		}
		
		public void updateDisk() {
		    StringBuilder content = new StringBuilder();

		    try (BufferedReader reader =
		             new BufferedReader(new FileReader("src/OperatingSystem/ExtraSpace.txt"))) {

		        String line;
		        while ((line = reader.readLine()) != null) {

		            line = line.trim();

		            if (line.startsWith("BEGIN") || line.startsWith("END")) {
		                continue;
		            }

		            if (line.equals("null")) {
		                line = "EMPTY";
		            }

		            content.append(line).append("\n");
		        }

		    } catch (IOException e) {
		        content.append("Error reading disk file");
		    }

		    diskTextArea.setText(content.toString());
		}
		
		public void updateMemory(){
			memoryGrid.getChildren().clear();
			int columns = 5;
			for(int i = 0 ; i < m.memory.length ; i++){
				String value = (Memory.memory[i] == null) ? "EMPTY" : Memory.memory[i];

		        Label cell = new Label("[" + i + "]\n" + value);

		        cell.setStyle(
		            "-fx-border-color: black;" +
		            "-fx-padding: 10;" +
		            "-fx-min-width: 100;" +
		            "-fx-min-height: 60;" +
		            "-fx-alignment: center;"
		        );

		        int row = i / columns;
		        int col = i % columns;

		        memoryGrid.add(cell, col, row);
			}
		}
		
		
		public void updateUI() {
		    ArrayList<Process> readyQueue = new ArrayList<>(ReadyQueue.ReadyQueue);
		    readyQueueListView.getItems().clear();
		    for (Process p : readyQueue) {
		        readyQueueListView.getItems().add("Process " + p.pcb.ID);
		    }
		    
		    List<Queue<Process>> mlfq = sched.MLFQ;

		    q0ListView.getItems().clear();
		    q1ListView.getItems().clear();
		    q2ListView.getItems().clear();
		    q3ListView.getItems().clear();

		    for (Process p : mlfq.get(0)) {
		        q0ListView.getItems().add("Process " + p.pcb.ID);
		    }

		    for (Process p : mlfq.get(1)) {
		        q1ListView.getItems().add("Process " + p.pcb.ID);
		    }

		    for (Process p : mlfq.get(2)) {
		        q2ListView.getItems().add("Process " + p.pcb.ID);
		    }

		    for (Process p : mlfq.get(3)) {
		        q3ListView.getItems().add("Process " + p.pcb.ID);
		    }	    
		    
		    ArrayList<Process> blockedQueue = new ArrayList<>(BlockedQueue.BlockedQueue);
		    blockedQueueListView.getItems().clear();
		    for (Process p : blockedQueue) {
		        blockedQueueListView.getItems().add("Process " + p.pcb.ID);
		    }

		    ArrayList<Process> finishedQueue = new ArrayList<>(sched.finishedProcess);
		    finishedProcessesListView.getItems().clear();
		    for (Process p : finishedQueue) {
		    	finishedProcessesListView.getItems().add("Process " + p.pcb.ID);
		    }
		    
		    updateMemory();
		    updateDisk();

		    clockCycleLabel.setText(String.valueOf(sched.time));
		    timeSliceLabel.setText(String.valueOf(sched.currentQuantumUsed));

		    Process running = sched.currentProcess;
		    Process shown = (sched.currentProcess != null) ? sched.currentProcess : sched.lastShownProcess;

		    if (shown != null) {
		        runningProcessLabel.setText(String.valueOf(shown.pcb.ID));
		        processStateLabel.setText(String.valueOf(shown.pcb.state));
		        pcLabel.setText(String.valueOf(shown.pcb.PC));

		        runningPidLabel.setText("PID: " + shown.pcb.ID);
		        runningStateLabel.setText("Process State: " + shown.pcb.state);
		        runningStartLabel.setText("Process Start: " + shown.pcb.start);
		        runningEndLabel.setText("Process End: " + shown.pcb.end);
		        runningArrivalLabel.setText("Process Arrival: " + shown.arrivalTime);
		    } else {
		        runningProcessLabel.setText("None");
		        processStateLabel.setText("---");
		        pcLabel.setText("---");
		        runningPidLabel.setText("PID: ---");
		        runningStateLabel.setText("State: ---");
		        runningStartLabel.setText("Memory Start: ---");
		        runningEndLabel.setText("Memory End: ---");
		        runningArrivalLabel.setText("Arrival Time: ---");
		    }

		    currentInstructionLabel.setText(sched.currentInstructionText);

		    userInputMutexLabel.setText(mc.userInput.available ? "User Input: Free" : "User Input: Occupied");
		    userOutputMutexLabel.setText(mc.userOutput.available ? "User Output: Free" : "User Output: Occupied");
		    fileMutexLabel.setText(mc.file.available ? "File: Free" : "File: Occupied");

		    userInput.setDisable(!sched.waitingForInput);
		    submitUserInput.setDisable(!sched.waitingForInput);
		}
		
		@FXML
        public void runAll() {
            if (sched.waitingForInput) {
                printToConsole("Submit the input value first.");
                return;
            }

            runAllTimeline = new Timeline(new KeyFrame(Duration.millis(800), e -> {
                if (sched.waitingForInput) {
                    runAllTimeline.stop();
                    return;
                }

//                boolean allDone = newProcesses.isEmpty() 
//                    && ReadyQueue.ReadyQueue.isEmpty() 
//                    && sched.currentProcess == null;
                
                boolean allDone;
                
                if (algorithm.getText().equals("MLFQ")) {
                	allDone = newProcesses.isEmpty() && sched.MLFQ.stream().allMatch(Queue::isEmpty) /* make sure all MLFQ queues are empty */&& sched.currentProcess == null;
                } else {
                	allDone = newProcesses.isEmpty() && ReadyQueue.ReadyQueue.isEmpty() && sched.currentProcess == null;
                }
                
                if (allDone) {
                    runAllTimeline.stop();
                    return;
                }

                if (algorithm.getText().equals("RR")) {
                    sched.stepRR(newProcesses);
                } else if (algorithm.getText().equals("HRRN")) {
                    sched.stepHRRN(newProcesses);
                } else if (algorithm.getText().equals("MLFQ")){
                	sched.stepMLFQ(newProcesses);
                }

                updateUI();
            }));

            runAllTimeline.setCycleCount(Timeline.INDEFINITE);
            runAllTimeline.play();
        }
		
        @FXML
        public void pauseResume() {
            if (runAllTimeline == null) return;

            if (isPaused) {
                runAllTimeline.play();
                isPaused = false;
                pauseButton.setText("Pause");
            } else {
                runAllTimeline.stop();
                isPaused = true;
                pauseButton.setText("Resume");
            }
        }
        
        @FXML
		public void resetSimulation() {
		    try {
		       
		        ReadyQueue.ReadyQueue.clear();
		        BlockedQueue.BlockedQueue.clear();

		        Memory.memory = new String[40]; //memory gdeeda
		        removeDisk();

		        sched = null;
		        mc = null;
		        m = null;
		        newProcesses = null;

		        Parent root = FXMLLoader.load(getClass().getResource("/resources/WelcomeScreen.fxml"));

		        Stage stage = (Stage) resetButton.getScene().getWindow();
		        stage.setScene(new Scene(root));

		    } catch (Exception e) {
		        e.printStackTrace();
		    }
        }
}
