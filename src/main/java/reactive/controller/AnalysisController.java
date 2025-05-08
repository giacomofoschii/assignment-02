package reactive.controller;

import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.graphstream.graph.*;
import org.graphstream.ui.fx_viewer.*;
import org.graphstream.ui.view.Viewer;
import reactive.model.*;
import reactive.view.AnalysisView;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller class that handles the business logic for dependency analysis
 */
public class AnalysisController {
    private static final int BUFFER_SIZE = 1000;
    private final AnalysisView view;
    private final ReactiveDependencyAnalyser analyser;
    private final Stage primaryStage;
    private final Graph graph;
    private final CompositeDisposable disposables;
    private final AtomicInteger classCount;
    private final AtomicInteger dependencyCount;
    private final Map<String, String> nodeIdMap;
    private String projectFolder;
    private FxViewer viewer;

    public AnalysisController(AnalysisView view, ReactiveDependencyAnalyser analyser, Stage primaryStage) {
        this.view = view;
        this.analyser = analyser;
        this.primaryStage = primaryStage;
        this.graph = this.view.getGraphView().initializeGraph();
        this.disposables = new CompositeDisposable();
        this.classCount = new AtomicInteger(0);
        this.dependencyCount = new AtomicInteger(0);
        this.nodeIdMap = new HashMap<>();

        this.initializeEventHandlers();
    }

    private void initializeEventHandlers() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project Folder");

        // Wire up the folder selection button
        view.getFolderButton().setOnAction(e -> {
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                this.view.getStartButton().setDisable(false);
                this.setProjectFolder(selectedDirectory.getPath());
                this.view.appendLog("Selected folder: " + selectedDirectory.getPath() + "\n");
            }
        });

        // Wire up the start button
        this.view.getStartButton().setOnAction(e -> this.startAnalysis());

        // Wire up the zoom slider
        this.view.getZoomSlider().valueProperty().addListener((observable, oldValue, newValue) -> {
            double zoomFactor = newValue.doubleValue();
            view.updateZoomLabel(zoomFactor);
            view.getGraphView().setZoom(zoomFactor);
        });
    }

    public void setProjectFolder(String path) {
        this.projectFolder = path;
    }

    public void startAnalysis() {
        if (this.projectFolder == null || this.projectFolder.isEmpty()) {
            this.view.appendLog("Error: No project folder selected\n");
            return;
        }

        this.resetAnalysis();

        this.view.appendLog("Starting analysis of: " + this.projectFolder + "\n");
        this.view.getStartButton().setDisable(true);
        this.view.getFolderButton().setDisable(true);

        // Process the Java files reactively
        this.disposables.add(
                this.analyser.getJavaFiles(this.projectFolder)
                        .onBackpressureBuffer(BUFFER_SIZE, () -> {}, BackpressureOverflowStrategy.ERROR)
                        .map(analyser::parseClassDependencies)
                        .observeOn(Schedulers.single())
                        .subscribe(
                                classDep -> {
                                    // Update counters and UI
                                    this.classCount.incrementAndGet();
                                    this.dependencyCount.addAndGet(classDep.getDependencyCount());
                                    Platform.runLater(() -> {
                                        this.view.updateClassesCount(this.classCount.get());
                                        this.view.updateDependenciesCount(this.dependencyCount.get());
                                        this.view.appendLog("Analyzed class: " + classDep.getClassName() +
                                                " - Dependencies: " + classDep.getDependencyCount() + "\n");
                                        updateGraph(classDep);
                                    });
                                },
                                error -> Platform.runLater(() -> {
                                    if (error instanceof MissingBackpressureException) {
                                        this.view.appendLog("Error: Too many classes, buffer full\n");
                                    } else {
                                        this.view.appendLog("Error: " + error.getMessage() + "\n");
                                    }
                                    this.view.getStartButton().setDisable(false);
                                    this.view.getFolderButton().setDisable(false);
                                }),
                                () -> Platform.runLater(() -> {
                                    this.view.appendLog("Analysis completed!\n");
                                    this.view.appendLog("Total classes: " + this.classCount.get() + "\n");
                                    this.view.appendLog("Total dependencies: " + this.dependencyCount.get() + "\n");
                                    this.view.getStartButton().setDisable(false);
                                    this.view.getFolderButton().setDisable(false);
                                })
                        )
        );
    }

    // Reset the analysis state
    private void resetAnalysis() {
        if (this.viewer != null) {
            this.viewer.close();
            this.viewer = null;
        }

        this.classCount.set(0);
        this.dependencyCount.set(0);

        this.view.clearLog();
        this.view.updateClassesCount(0);
        this.view.updateDependenciesCount(0);

        this.graph.clear();
        this.nodeIdMap.clear();

        this.viewer = new FxViewer(this.graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        this.viewer.enableAutoLayout();
        FxViewPanel viewPanel = (FxViewPanel) this.viewer.addDefaultView(false);
        this.view.getZoomSlider().setValue(0.5);
        this.view.updateZoomLabel(0.5);
        if (viewPanel.getCamera() != null) {
            viewPanel.getCamera().setViewPercent(2.0);
        }

        Platform.runLater(() -> this.view.getGraphView().displayGraph(viewPanel));
    }

    // Update the graph with a new class dependency
    private void updateGraph(ClassDependency classDep) {
        String className = simplifyClassName(classDep.getClassName());
        String nodeId = getOrCreateNodeId(className);

        if (this.graph.getNode(nodeId) == null) {
            Node node = this.graph.addNode(nodeId);
            node.setAttribute("ui.label", className);
        }

        // Add dependencies as edges
        for (String dependency : classDep.getDependencies()) {
            String depName = simplifyClassName(dependency);
            String depNodeId = getOrCreateNodeId(depName);

            if (this.graph.getNode(depNodeId) == null) {
                Node depNode = this.graph.addNode(depNodeId);
                depNode.setAttribute("ui.label", depName);
            }

            String edgeId = nodeId + "-" + depNodeId;
            if (this.graph.getEdge(edgeId) == null) {
                this.graph.addEdge(edgeId, nodeId, depNodeId, true);
            }
        }
    }

    //Simplify a fully qualified class name for display
    private String simplifyClassName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private String getOrCreateNodeId(String className) {
        return nodeIdMap.computeIfAbsent(className, k -> "n" + nodeIdMap.size());
    }

    public boolean handleCloseRequest() {
        boolean shouldClose = this.view.showExitConfirmation();
        if (shouldClose) {
            this.shutdown();
        }
        return shouldClose;
    }

    // Shutdown the controller and dispose resources
    public void shutdown() {
        this.disposables.dispose();
        this.closeViewer();

        try {
            Schedulers.shutdown();
        } catch (Exception e) {
            System.err.println("Error shutting down schedulers: " + e.getMessage());
        }

        Platform.runLater(() -> {
            Platform.setImplicitExit(true);
            Platform.exit();
        });
    }

    private void closeViewer() {
        if(this.viewer != null) {
            try {
                this.viewer.disableAutoLayout();
                this.viewer.close();
                this.viewer = null;
            } catch (Exception e) {
                System.err.println("Error closing viewer: " + e.getMessage());
            }
        }
    }
}