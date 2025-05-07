package reactive;

import javafx.application.Application;
import javafx.stage.*;
import reactive.controller.AnalysisController;
import reactive.model.ReactiveDependencyAnalyser;
import reactive.view.AnalysisView;

/**
 * Main application class that follows the MVC pattern
 * Responsible only for bootstrapping the application
 */
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
        this.controller = new AnalysisController(view, model, primaryStage);

        view.setupStage(primaryStage);

        // Handle window close - delegate to controller
        primaryStage.setOnCloseRequest(e -> {
            if (!controller.handleCloseRequest()) {
                e.consume();
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