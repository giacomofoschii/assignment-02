package reactive.view;

import javafx.scene.layout.Pane;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;

/**
 * Specialized view class that handles graph visualization.
 */
public class GraphView {

    private final Pane graphPane;
    private FxViewPanel viewPanel;

    public GraphView() {
        this.graphPane = new Pane();
    }

    public Graph initializeGraph() {
        Graph graph = new SingleGraph("Dependencies");
        graph.setAttribute("ui.stylesheet",
                "node { " +
                        "size: 30px; " +
                        "text-size: 12px; " +
                        "text-color: #000; " +
                        "fill-color: #B3E5FC; " +
                        "stroke-mode: plain; " +
                        "stroke-color: #0288D1; " +
                        "stroke-width: 1px; " +
                        "text-background-mode: rounded-box; " +
                        "text-background-color: rgba(255, 255, 255, 200); " +
                        "text-padding: 3px; " +
                        "text-offset: 0px, -25px; " +
                        "} " +
                        "edge { " +
                        "shape: line; " +
                        "arrow-shape: arrow; " +
                        "arrow-size: 12px, 6px; " +
                        "fill-color: #757575; " +
                        "size: 1px; " +
                        "}");
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");
        return graph;
    }

    public Pane getGraphPane() {
        return this.graphPane;
    }

    public void displayGraph(FxViewPanel viewPanel) {
        this.graphPane.getChildren().clear();
        this.graphPane.getChildren().add(viewPanel);
        this.viewPanel = viewPanel;

        if (this.viewPanel.getCamera() != null) {
            this.viewPanel.getCamera().setViewPercent(4.5);
        }

        // Bind the view panel to the graph pane dimensions
        viewPanel.prefWidthProperty().bind(this.graphPane.widthProperty());
        viewPanel.prefHeightProperty().bind(this.graphPane.heightProperty());
    }

    public void setZoom(double zoomFactor) {
        if (this.viewPanel != null) {
            this.viewPanel.getCamera().setViewPercent(1/zoomFactor);
        }
    }
}