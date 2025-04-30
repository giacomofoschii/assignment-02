package gui.codeAnalyzer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class DependencyAnalyser extends Application {

    private TextArea logTextArea;
    private Label classesCountLabel;
    private Label dependenciesCountLabel;
    private Button startButton;
    private Pane graphPane;
    private Graph graph;

    private File selectedFolder;

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "javafx"); // Set this before launch
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Dependency Analyzer");

        //Create UI components
        BorderPane root = new BorderPane();
        setupTopPanel(root);
        setupCenterPanel(root);
        setupBottomPanel(root);

        //Create the graph
        // Updated GraphStream initialization
        graph = new SingleGraph("Dependencies");
        graph.setAttribute("ui.stylesheet",
                "node { size: 30px; text-size: 12px; text-color: #000; fill-color: #B3E5FC; stroke-mode: plain; stroke-color: #0288D1; } " +
                        "edge { arrow-shape: arrow; arrow-size: 12px, 6px; fill-color: #757575; }");
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupTopPanel(BorderPane root) {
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        Button folderButton =  new Button("Select Project Folder");
        folderButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Project Root Directory");
            this.selectedFolder = directoryChooser.showDialog(null);

            if (this.selectedFolder != null) {
                this.logTextArea.appendText("Selected folder: " + this.selectedFolder.getAbsolutePath() + "\n");
                this.startButton.setDisable(false);
            }
        });

        this.startButton = new Button("Start Analysis");
        this.startButton.setDisable(true);
        this.startButton.setOnAction(e -> startAnalysis());

        topPanel.getChildren().addAll(folderButton, this.startButton);
        root.setTop(topPanel);
    }

    private void setupCenterPanel(BorderPane root) {
        SplitPane splitPane = new SplitPane();

        //Right panel
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));

        Label graphLabel = new Label("Dependency Graph:");
        graphPane = new Pane();

        rightPanel.getChildren().addAll(graphLabel, graphPane);
        VBox.setVgrow(graphPane, Priority.ALWAYS);

        //Left panel
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));

        Label logLabel = new Label("Analysis Log:");
        logTextArea = new TextArea();
        logTextArea.setEditable(false);

        leftPanel.getChildren().addAll(logLabel, logTextArea);
        VBox.setVgrow(logTextArea, Priority.ALWAYS);

        //Add components to the top panel
        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.5);
        root.setCenter(splitPane);
    }

    private void setupBottomPanel(BorderPane root) {
        HBox bottomPanel = new HBox(20);
        bottomPanel.setPadding(new Insets(10));

        this.classesCountLabel = new Label("Number of Classes/Interfaces: 0");
        this.dependenciesCountLabel = new Label("Number of Dependencies: 0");

        bottomPanel.getChildren().addAll(classesCountLabel, dependenciesCountLabel);
        root.setBottom(bottomPanel);
    }

    private void startAnalysis() {
        if (this.selectedFolder == null) {
            this.logTextArea.appendText("Please select a project folder first.\n");
            return; // Added return to prevent further execution
        }

        //Reset the Interface
        this.logTextArea.clear();
        this.classesCountLabel.setText("Classes/Interface: 0");
        this.dependenciesCountLabel.setText("Dependencies: 0");
        this.graph.clear();

        //Initialize counters
        AtomicInteger classCounter = new AtomicInteger(0);
        AtomicInteger dependencyCounter = new AtomicInteger(0);

        //Set button state
        this.startButton.setDisable(true);

        // Log the start of analysis
        this.logTextArea.appendText("Starting analysis of folder: " + this.selectedFolder.getAbsolutePath() + "\n");

        try {
            // Initialize and setup graph viewer
            Viewer viewer = new FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
            viewer.enableAutoLayout();
            FxViewPanel viewPanel = (FxViewPanel) viewer.addDefaultView(false);
            graphPane.getChildren().clear();
            graphPane.getChildren().add(viewPanel);
            viewPanel.prefWidthProperty().bind(graphPane.widthProperty());
            viewPanel.prefHeightProperty().bind(graphPane.heightProperty());

            this.logTextArea.appendText("Graph viewer initialized successfully.\n");

            // Add a placeholder node to verify graph rendering works
            graph.addNode("test");
            graph.getNode("test").setAttribute("ui.label", "Test Node");

            // Re-enable the start button after analysis is done
            this.startButton.setDisable(false);

        } catch (Exception e) {
            this.logTextArea.appendText("Error initializing graph viewer: " + e.getMessage() + "\n");
            e.printStackTrace();
            this.startButton.setDisable(false);
        }

        // Create reactive pipeline to analyze the project
        // TODO: Implement the actual analysis logic
    }
}