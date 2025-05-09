package reactive.view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;


/**
 * Main view class that composes the UI elements for the dependency analyzer.
 */
public class AnalysisView {

    private final BorderPane root;
    private final GraphView graphView;
    private TextArea logTextArea;
    private Label classesCountLabel;
    private Label dependenciesCountLabel;
    private Button startButton;
    private Button folderButton;
    private Slider zoomSlider;
    private Label zoomLabel;

    public AnalysisView() {
        this.root = new BorderPane();
        this.graphView = new GraphView();

        setupTopPanel();
        setupCenterPanel();
        setupBottomPanel();
    }

    public void setupStage(Stage primaryStage) {
        primaryStage.setTitle("Java Dependency Analyzer");
        primaryStage.setScene(this.createScene());
        primaryStage.getIcons()
                .add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))));
    }

    private Scene createScene() {
        return new Scene(this.root, 1000, 700);
    }

    private void setupTopPanel() {
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));

        this.folderButton = new Button("Select Project Folder");
        this.folderButton.setDisable(false);

        this.startButton = new Button("Start Analysis");
        this.startButton.setDisable(true);

        topPanel.getChildren().addAll(folderButton, startButton);
        this.root.setTop(topPanel);
    }

    private void setupCenterPanel() {
        SplitPane splitPane = new SplitPane();

        // Left panel - Log area
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        Label logLabel = new Label("Analysis Log:");
        this.logTextArea = new TextArea();
        this.logTextArea.setEditable(false);
        leftPanel.getChildren().addAll(logLabel, this.logTextArea);
        VBox.setVgrow(this.logTextArea, Priority.ALWAYS);

        // Right panel - Graph area with zoom control
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        Label graphLabel = new Label("Dependency Graph:");
        HBox zoomControlPanel = this.setupZoomControlPanel();
        ScrollPane scrollPane = this.setupScrollPane();

        // Add components to the panels
        rightPanel.getChildren().addAll(graphLabel, zoomControlPanel, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.3);
        this.root.setCenter(splitPane);
    }

    private void setupBottomPanel() {
        HBox bottomPanel = new HBox(20);
        bottomPanel.setPadding(new Insets(10));

        this.classesCountLabel = new Label("Start to count number of Classes/Interfaces");
        this.dependenciesCountLabel = new Label("Start to count number of Dependencies");

        bottomPanel.getChildren().addAll(classesCountLabel, dependenciesCountLabel);
        this.root.setBottom(bottomPanel);
    }

    public boolean showExitConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText("Are you sure you want to exit?");
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))));

        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noBtn = new ButtonType("No", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yesBtn, noBtn);

        alert.getDialogPane().lookupButton(yesBtn).setStyle("-fx-background-color: green; -fx-text-fill: white;");
        alert.getDialogPane().lookupButton(noBtn).setStyle("-fx-background-color: red; -fx-text-fill: white;");

        return alert.showAndWait().orElse(noBtn) == yesBtn;
    }

    private ScrollPane setupScrollPane() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        Pane graphPane = this.graphView.getGraphPane();
        graphPane.setPrefSize(2000, 2000);
        scrollPane.setContent(graphPane);
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
        return scrollPane;
    }

    private HBox setupZoomControlPanel() {
        HBox zoomControlPanel = new HBox(10);
        this.zoomLabel = new Label("Zoom: 100%");
        this.zoomSlider = new Slider(0.1, 3.0, 1.0);
        this.zoomSlider.setShowTickLabels(true);
        this.zoomSlider.setShowTickMarks(true);
        this.zoomSlider.setMajorTickUnit(0.5);
        zoomControlPanel.getChildren().addAll(this.zoomLabel, this.zoomSlider);
        return zoomControlPanel;
    }

    public Button getStartButton() {
        return this.startButton;
    }

    public Button getFolderButton() {
        return this.folderButton;
    }

    public GraphView getGraphView() {
        return this.graphView;
    }

    public Slider getZoomSlider() {
        return this.zoomSlider;
    }

    public void appendLog(String text) {
        this.logTextArea.appendText(text);
    }

    public void clearLog() {
        this.logTextArea.clear();
    }

    public void updateClassesCount(int count) {
        this.classesCountLabel.setText("Number of Classes/Interfaces: " + count);
    }

    public void updateDependenciesCount(int count) {
        this.dependenciesCountLabel.setText("Number of Dependencies: " + count);
    }

    public void updateZoomLabel(double zoomFactor) {
        int percentage = (int) (zoomFactor * 100);
        this.zoomLabel.setText("Zoom: " + percentage + "%");
    }
}