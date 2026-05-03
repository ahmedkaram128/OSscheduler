package GUI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.input.KeyCode;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;



public class WelcomeController {
	//Welcome Screen
	@FXML
	private ComboBox algorithmComboBox;
	@FXML
	private TextField quantumField;
	@FXML
	private TextField p1File;
	@FXML
	private TextField p2File;
	@FXML
	private TextField p3File;
	@FXML
	private TextField p1Arrival;
	@FXML
	private TextField p2Arrival;
	@FXML
	private TextField p3Arrival;
	@FXML
	private Label errorState;
	@FXML
	private Button startButton;
	
	
	public void initialize(){
		algorithmComboBox.getItems().addAll("HRRN" , "RR" , "MLFQ");
	}
	
	@FXML
	public void startSimulation() throws IOException {
	    String quantumFieldContent = quantumField.getText();
	    String p1FileContent = p1File.getText();
	    String p2FileContent = p2File.getText();
	    String p3FileContent = p3File.getText();
	    String p1ArrivalContent = p1Arrival.getText();
	    String p2ArrivalContent = p2Arrival.getText();
	    String p3ArrivalContent = p3Arrival.getText();
	    
	    if (algorithmComboBox.getValue() == null) {
	        errorState.setText("Error: Choose an Algorithm!");
	        return;
	    }
	    
	    String selectedAlgorithm = (String) algorithmComboBox.getValue();
	    boolean needsQuantum = selectedAlgorithm.equals("RR");

	    if ((needsQuantum && quantumFieldContent.trim().isEmpty()) ||
	        p1FileContent.trim().isEmpty() ||
	        p2FileContent.trim().isEmpty() ||
	        p3FileContent.trim().isEmpty() ||
	        p1ArrivalContent.trim().isEmpty() ||
	        p2ArrivalContent.trim().isEmpty() ||
	        p3ArrivalContent.trim().isEmpty()) {
	        errorState.setText("Error: All fields must be filled!");
	        return;
	    }

	    int quantum = 1; 
	    int p1Arrival, p2Arrival, p3Arrival;
	    
	    try {
	        if (needsQuantum) {
	            quantum = Integer.parseInt(quantumFieldContent);
	        }
	        p1Arrival = Integer.parseInt(p1ArrivalContent);
	        p2Arrival = Integer.parseInt(p2ArrivalContent);
	        p3Arrival = Integer.parseInt(p3ArrivalContent);
	    } catch (NumberFormatException e) {
	        errorState.setText("Error: Quantum and arrival times must be valid numbers!");
	        return;
	    }

	    if (needsQuantum && quantum <= 0) {
	        errorState.setText("Error: Quantum must be > 0!");
	        return;
	    }

	    if (p1Arrival < 0 || p2Arrival < 0 || p3Arrival < 0) {
	        errorState.setText("Error: Arrival times must be >= 0!");
	        return;
	    }

	    Stage stage = (Stage) startButton.getScene().getWindow();
	    FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/OSScheduler.fxml"));
	    Parent root = loader.load();
	    DashboardController controller = loader.getController();

	    controller.setDashboardData(String.valueOf(quantum), selectedAlgorithm);
	    controller.setupProcesses(p1Arrival, p2Arrival, p3Arrival,
	            p1FileContent, p2FileContent, p3FileContent);
	    controller.updateUI();

	    Scene scene = new Scene(root);
	    stage.setScene(scene);
	    stage.show();
	}
	
	
}


