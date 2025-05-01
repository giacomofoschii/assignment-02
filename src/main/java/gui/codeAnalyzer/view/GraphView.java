package gui.codeAnalyzer.view;

import javafx.scene.layout.Pane;
import org.graphstream.ui.fx_viewer.FxViewPanel;

/**
 * Specialized view class that handles graph visualization.
 */
public class GraphView {

    private final Pane graphPane;

    public GraphView() {
        this.graphPane = new Pane();
    }

    /**
     * Gets the graph pane for displaying the dependency graph.
     *
     * @return The Pane containing the graph visualization
     */
    public Pane getGraphPane() {
        return this.graphPane;
    }

    /**
     * Displays the graph in the graph pane.
     *
     * @param viewPanel The GraphStream view panel to display
     */
    public void displayGraph(FxViewPanel viewPanel) {
        this.graphPane.getChildren().clear();
        this.graphPane.getChildren().add(viewPanel);

        //Bind the view panel to the graph pane dimensions
        viewPanel.prefWidthProperty().bind(this.graphPane.widthProperty());
        viewPanel.prefHeightProperty().bind(this.graphPane.heightProperty());
    }
}
