package reactive;

import javafx.application.Application;
import javafx.stage.*;
import reactive.controller.AnalysisController;
import reactive.model.ReactiveDependencyAnalyser;
import reactive.view.AnalysisView;

/* Implemented by:
    Giacomo Foschi
    Matricola: 0001179137
    Email: giacomo.foschi3@studio.unibo.it

    Giovanni Pisoni
    Matricola: 0001189814
    Email: giovanni.pisoni@studio.unibo.it

    Giovanni Rinchiuso
    Matricola: 0001195145
     Email: giovanni.rinchiuso@studio.unibo.it

    Gioele Santi
    Matricola: 0001189403
    Email: gioele.santi2@studio.unibo.it
*/
/**
 * Main application class that follows the MVC pattern
 * Responsible only for bootstrapping the application
 */
public class DependencyAnalyser extends Application {

    private AnalysisController controller;

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "javafx");
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