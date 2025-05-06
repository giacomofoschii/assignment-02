package reactive.controller;

import reactive.view.AnalysisView;
import reactive.model.ReactiveDependencyAnalyser;
import common.util.TypeDependency;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.core.Observable;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Edge;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller class that handles the business logic for dependency analysis.
 */
public class AnalysisController {
    private final AnalysisView view;
    private final Graph graph;
    private final ReactiveDependencyAnalyser analyser;
    private final CompositeDisposable disposables;
    private File selectedFolder;
    private Viewer graphViewer;

    public AnalysisController(AnalysisView view) {
        this.view = view;
        this.graph = this.initializeGraph();
        this.analyser = new ReactiveDependencyAnalyser();
        this.disposables = new CompositeDisposable();

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

    public void setupCloseHandler(Stage stage) {
        stage.setOnCloseRequest(e -> {
            e.consume();
            if (this.view.showExitConfirmation()) {
                this.shutdown();
                stage.close();
            }
        });
    }

    private void shutdown() {
        // Clean up reactive disposables
        if (!disposables.isDisposed()) {
            disposables.dispose();
        }

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
            return;
        }

        // Reset the Interface
        this.view.clearLog();
        this.view.updateClassesCount(0);
        this.view.updateDependenciesCount(0);
        this.graph.clear();

        // Set button state
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

            // Set of processed class names to avoid duplication in the graph
            Set<String> processedClasses = new HashSet<>();
            Set<String> processedEdges = new HashSet<>();

            // Start the reactive analysis of the project
            disposables.add(
                    analyser.analyzeProject(selectedFolder)
                            .subscribeOn(Schedulers.io())
                            .flatMap(projectReport -> {
                                // Process all packages and their classes
                                return Observable.fromIterable(projectReport.getPackageReports().values())
                                        .flatMap(packageReport ->
                                                Observable.fromIterable(packageReport.getClassReports().values())
                                        );
                            })
                            .observeOn(Schedulers.single()) // Process graph operations on a single thread
                            .subscribe(
                                    // OnNext - Process each class report
                                    classReport -> {
                                        String className = classReport.getClassName();

                                        // Update UI with current progress
                                        Platform.runLater(() -> {
                                            this.view.updateClassesCount(analyser.getClassCounter().get());
                                            this.view.updateDependenciesCount(analyser.getDependencyCounter().get());
                                        });

                                        // Add source class node if not exists
                                        if (!processedClasses.contains(className)) {
                                            processedClasses.add(className);

                                            Platform.runLater(() -> {
                                                Node node = graph.addNode(className);
                                                // Use simple name for display
                                                String simpleClassName = className.contains(".") ?
                                                        className.substring(className.lastIndexOf('.') + 1) : className;
                                                node.setAttribute("ui.label", simpleClassName);
                                            });
                                        }

                                        // Process dependencies
                                        for (TypeDependency dep : classReport.getDependencies()) {
                                            String targetClass = dep.getTargetType();
                                            String edgeId = className + "->" + targetClass;

                                            // Add target class node if not exists
                                            if (!processedClasses.contains(targetClass)) {
                                                processedClasses.add(targetClass);

                                                Platform.runLater(() -> {
                                                    Node node = graph.addNode(targetClass);
                                                    // Use simple name for display
                                                    String simpleClassName = targetClass.contains(".") ?
                                                            targetClass.substring(targetClass.lastIndexOf('.') + 1) : targetClass;
                                                    node.setAttribute("ui.label", simpleClassName);
                                                });
                                            }

                                            // Add edge if not exists
                                            if (!processedEdges.contains(edgeId)) {
                                                processedEdges.add(edgeId);

                                                Platform.runLater(() -> {
                                                    Edge edge = graph.addEdge(edgeId, className, targetClass, true);
                                                    edge.setAttribute("ui.label", dep.getType().toString().toLowerCase());
                                                });
                                            }
                                        }
                                    },
                                    // OnError - Handle errors
                                    error -> {
                                        Platform.runLater(() -> {
                                            this.view.appendLog("Error during analysis: " + error.getMessage() + "\n");
                                            this.view.getStartButton().setDisable(false);
                                        });
                                    },
                                    // OnComplete - All done
                                    () -> {
                                        Platform.runLater(() -> {
                                            this.view.appendLog("Analysis complete. Built dependency graph with " +
                                                    processedClasses.size() + " nodes and " + processedEdges.size() + " edges.\n");
                                            this.view.getStartButton().setDisable(false);
                                        });
                                    }
                            )
            );

        } catch (Exception e) {
            this.view.appendLog("Error initializing graph viewer: " + e.getMessage() + "\n");
            this.view.getStartButton().setDisable(false);
        }
    }
}