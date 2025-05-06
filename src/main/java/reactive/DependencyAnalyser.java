package reactive;

import javafx.application.Application;
import javafx.stage.*;
import reactive.controller.AnalysisController;
import reactive.model.ReactiveDependencyAnalyser;
import reactive.view.AnalysisView;

import java.io.File;

public class DependencyAnalyser extends Application {

    private AnalysisController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Set up MVC components
        AnalysisView view = new AnalysisView();
        ReactiveDependencyAnalyser model = new ReactiveDependencyAnalyser();
        this.controller = new AnalysisController(view, model);

        // Configure stage
        primaryStage.setTitle("Java Dependency Analyzer");
        primaryStage.setScene(view.createScene());

        // Set up folder selection dialog
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project Folder");

        // Wire up the folder selection button
        view.getFolderButton().setOnAction(e -> {
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                view.getStartButton().setDisable(false);
                this.controller.setProjectFolder(selectedDirectory.getPath());
                view.appendLog("Selected folder: " + selectedDirectory.getPath() + "\n");
            }
        });

        // Wire up the start button
        view.getStartButton().setOnAction(e -> this.controller.startAnalysis());

        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            if (!view.showExitConfirmation()) {
                e.consume();
            } else {
                this.controller.shutdown();
            }
        });

        primaryStage.show();
    }

    @Override
    public void stop() {
        if (this.controller != null) {
            this.controller.shutdown();
        }
    }

}