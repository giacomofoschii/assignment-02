package reactive.controller;

import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.schedulers.Schedulers;
import javafx.application.Platform;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.*;
import org.graphstream.ui.view.Viewer;
import reactive.model.*;
import reactive.view.AnalysisView;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller class for the dependency analysis application
 */
public class AnalysisController {
    private static final int BUFFER_SIZE = 1000;
    private final AnalysisView view;
    private final ReactiveDependencyAnalyser model;
    private final CompositeDisposable disposables;
    private String projectFolder;
    private final AtomicInteger classCount = new AtomicInteger(0);
    private final AtomicInteger dependencyCount = new AtomicInteger(0);
    private final Graph graph;
    private final Map<String, String> nodeIdMap = new HashMap<>();

    public AnalysisController(AnalysisView view, ReactiveDependencyAnalyser model) {
        this.view = view;
        this.model = model;
        this.disposables = new CompositeDisposable();

        // Initialize the graph
        System.setProperty("org.graphstream.ui", "javafx");
        this.graph = new SingleGraph("dependencies");

        // Set up the graph styling
        graph.setAttribute("ui.stylesheet",
                "node { size: 10px; fill-color: #66B2FF; text-size: 12; } " +
                        "edge { arrow-size: 5px, 3px; }");
    }

    /**
     * Set the project folder to analyze
     * @param path the path to the project folder
     */
    public void setProjectFolder(String path) {
        this.projectFolder = path;
    }

    /**
     * Start the dependency analysis process
     */
    public void startAnalysis() {
        if (projectFolder == null || projectFolder.isEmpty()) {
            view.appendLog("Error: No project folder selected\n");
            return;
        }

        // Reset state
        resetAnalysis();

        view.appendLog("Starting analysis of: " + projectFolder + "\n");
        view.getStartButton().setDisable(true);
        view.getFolderButton().setDisable(true);

        // Process the Java files reactively
        disposables.add(
                model.getJavaFiles(projectFolder)
                        .onBackpressureBuffer(BUFFER_SIZE, () -> {}, BackpressureOverflowStrategy.ERROR)
                        .map(model::parseClassDependencies)
                        .observeOn(Schedulers.single())
                        .subscribe(
                                classDep -> {
                                    // Update counters
                                    classCount.incrementAndGet();
                                    dependencyCount.addAndGet(classDep.getDependencyCount());

                                    // Update UI
                                    Platform.runLater(() -> {
                                        view.updateClassesCount(classCount.get());
                                        view.updateDependenciesCount(dependencyCount.get());
                                        view.appendLog("Analyzed class: " + classDep.getClassName() +
                                                " - Dependencies: " + classDep.getDependencyCount() + "\n");

                                        // Update graph
                                        updateGraph(classDep);
                                    });
                                },
                                error -> {
                                    Platform.runLater(() -> {
                                        if (error instanceof MissingBackpressureException) {
                                            view.appendLog("Error: Too many classes, buffer full\n");
                                        } else {
                                            view.appendLog("Error: " + error.getMessage() + "\n");
                                        }
                                        view.getStartButton().setDisable(false);
                                        view.getFolderButton().setDisable(false);
                                    });
                                },
                                () -> {
                                    Platform.runLater(() -> {
                                        view.appendLog("Analysis completed!\n");
                                        view.appendLog("Total classes: " + classCount.get() + "\n");
                                        view.appendLog("Total dependencies: " + dependencyCount.get() + "\n");
                                        view.getStartButton().setDisable(false);
                                        view.getFolderButton().setDisable(false);
                                    });
                                }
                        )
        );
    }

    /**
     * Reset the analysis state
     */
    private void resetAnalysis() {
        // Clear counters
        classCount.set(0);
        dependencyCount.set(0);

        // Update UI
        view.clearLog();
        view.updateClassesCount(0);
        view.updateDependenciesCount(0);

        // Clear graph
        graph.clear();
        nodeIdMap.clear();

        // Create a new viewer for the graph
        FxViewer viewer = new FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        viewer.enableAutoLayout();
        FxViewPanel viewPanel = (FxViewPanel) viewer.addDefaultView(false);

        // Display the graph
        Platform.runLater(() -> view.getGraphView().displayGraph(viewPanel));
    }

    /**
     * Update the graph with a new class dependency
     * @param classDep the class dependency to add to the graph
     */
    private void updateGraph(ClassDependency classDep) {
        // Simplify class names for display
        String className = simplifyClassName(classDep.getClassName());
        String nodeId = getOrCreateNodeId(className);

        // Add the node if it doesn't exist
        if (graph.getNode(nodeId) == null) {
            Node node = graph.addNode(nodeId);
            node.setAttribute("ui.label", className);
        }

        // Add dependencies as edges
        for (String dependency : classDep.getDependencies()) {
            String depName = simplifyClassName(dependency);
            String depNodeId = getOrCreateNodeId(depName);

            // Add the dependency node if it doesn't exist
            if (graph.getNode(depNodeId) == null) {
                Node depNode = graph.addNode(depNodeId);
                depNode.setAttribute("ui.label", depName);
            }

            // Add an edge if it doesn't exist
            String edgeId = nodeId + "-" + depNodeId;
            if (graph.getEdge(edgeId) == null) {
                graph.addEdge(edgeId, nodeId, depNodeId, true);
            }
        }
    }

    /**
     * Simplify a fully qualified class name for display
     * @param fullName the fully qualified class name
     * @return the simplified class name
     */
    private String simplifyClassName(String fullName) {
        // Get the simple class name (without package)
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    /**
     * Get or create a node ID for a class name
     * @param className the class name
     * @return the node ID
     */
    private String getOrCreateNodeId(String className) {
        return nodeIdMap.computeIfAbsent(className, k -> "n" + nodeIdMap.size());
    }

    /**
     * Shutdown the controller and dispose resources
     */
    public void shutdown() {
        disposables.dispose();
    }
}