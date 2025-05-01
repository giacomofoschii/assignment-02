package gui.codeAnalyzer;

import gui.codeAnalyzer.controller.AnalysisController;
import gui.codeAnalyzer.view.AnalysisView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class DependencyAnalyser extends Application {

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "javafx"); // Set this before launch
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Dependency Analyzer");

        //Initialize view and controller
        AnalysisView view = new AnalysisView();
        AnalysisController controller = new AnalysisController(view);

        // Set up the main layout
        Scene scene = new Scene(view.getRoot(), 1200, 800);
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            e.consume(); // Prevent default close action

            //Confirmation dialog
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit Confirmation");
            alert.setHeaderText("Are you sure you want to exit?");

            ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);

            alert.getButtonTypes().setAll(yesButton, noButton);

            alert.getDialogPane().lookupButton(yesButton).setStyle("-fx-background-color: green; -fx-text-fill: white;");
            alert.getDialogPane().lookupButton(noButton).setStyle("-fx-background-color: red; -fx-text-fill: white;");

            alert.showAndWait().ifPresent(response -> {
                if (response == yesButton) {
                    controller.shutdown(); // Call shutdown method
                    primaryStage.close(); // Close the application
                }
            });

        });

        primaryStage.show();
    }
}