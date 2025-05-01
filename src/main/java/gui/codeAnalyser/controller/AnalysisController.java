package gui.codeAnalyser.controller;

import gui.codeAnalyser.view.AnalysisView;
import javafx.stage.DirectoryChooser;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller class that handles the business logic for dependency analysis.
 */
public class AnalysisController {

    private final AnalysisView view;
    private final Graph graph;
    private File selectedFolder;
    private Viewer graphViewer;


    public AnalysisController(AnalysisView view) {
        this.view = view;
        this.graph = this.initializeGraph();

        this.initializeEventHandlers();
    }

    private Graph initializeGraph() {
        Graph graph = new SingleGraph("Dependencies");
        graph.setAttribute("ui.stylesheet",
                "node { size: 30px; text-size: 12px; text-color: #000; fill-color: #B3E5FC; stroke-mode: plain; stroke-color: #0288D1; } " +
                        "edge { arrow-shape: arrow; arrow-size: 12px, 6px; fill-color: #757575; }");
        return graph;
    }

    private void initializeEventHandlers() {
        this.view.getFolderButton().setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Project Root Directory");
            this.selectedFolder = directoryChooser.showDialog(null);

            if (this.selectedFolder != null) {
                this.view.appendLog("Selected folder: " + this.selectedFolder.getAbsolutePath() + "\n");
                this.view.getStartButton().setDisable(false);
            } else {
                this.view.appendLog("No folder selected.\n");
                this.view.getStartButton().setDisable(true);
            }
        });

        // Set action for start analysis button
        this.view.getStartButton().setOnAction(e -> startAnalysis());
    }

    public void shutdown() {
        // Clean up graph viewer resources
        if (this.graphViewer != null) {
            try {
                this.graphViewer.close();
                this.view.appendLog("Graph viewer resources released.\n");
            } catch (Exception e) {
                this.view.appendLog("Error during graph viewer shutdown: " + e.getMessage() + "\n");
            }
        }

        // Clean up graph resources
        if (this.graph != null) {
            this.graph.clear();
        }
    }

    public void startAnalysis() {
        if (this.selectedFolder == null) {
            this.view.appendLog("Please select a project folder first.\n");
            return; // Added return to prevent further execution
        }

        //Reset the Interface
        this.view.clearLog();
        this.view.updateClassesCount(0);
        this.view.updateDependenciesCount(0);
        this.graph.clear();

        //Initialize counters
        AtomicInteger classCounter = new AtomicInteger(0);
        AtomicInteger dependencyCounter = new AtomicInteger(0);

        //Set button state
        this.view.getStartButton().setDisable(true);

        // Log the start of analysis
        this.view.appendLog("Starting analysis of folder: " + this.selectedFolder.getAbsolutePath() + "\n");

        try {
            // Initialize and setup graph viewer
            this.graphViewer = new FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
            this.graphViewer.enableAutoLayout();
            FxViewPanel viewPanel = (FxViewPanel) this.graphViewer.addDefaultView(false);

            // Add the graph view to the UI
            this.view.getGraphView().displayGraph(viewPanel);

            this.view.appendLog("Graph viewer initialized successfully.\n");

            // Add a placeholder node to verify graph rendering works
            this.graph.addNode("test");
            this.graph.getNode("test").setAttribute("ui.label", "Test Node");

            // Re-enable the start button after analysis is done
            this.view.getStartButton().setDisable(false);

        } catch (Exception e) {
            this.view.appendLog("Error initializing graph viewer: " + e.getMessage() + "\n");
            this.view.getStartButton().setDisable(false);
        }

        // Create reactive pipeline to analyze the project
        // TODO: Implement the actual analysis logic
    }

}
