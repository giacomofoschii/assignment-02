package gui.codeAnalyser;

import gui.codeAnalyser.controller.AnalysisController;
import gui.codeAnalyser.view.AnalysisView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
        primaryStage.setScene(view.createScene());

        // Set up the close request handler to clean up resources
        controller.setupCloseHandler(primaryStage);

        primaryStage.show();
    }
}