/*
 * Copyright (c) 2003, The JUNG Authors
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or https://github.com/tomnelson/jungrapht-visualization/blob/master/LICENSE for a description.
 *
 */
package org.jungrapht.samples;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;
import org.jungrapht.samples.util.LayoutHelper;
import org.jungrapht.samples.util.SpanningTreeAdapter;
import org.jungrapht.samples.util.TestGraphs;
import org.jungrapht.visualization.VisualizationModel;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.DefaultModalGraphMouse;
import org.jungrapht.visualization.control.ModalGraphMouse;
import org.jungrapht.visualization.decorators.EllipseShapeFunction;
import org.jungrapht.visualization.layout.algorithms.*;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.subLayout.Collapsable;
import org.jungrapht.visualization.subLayout.GraphCollapser;
import org.jungrapht.visualization.util.PredicatedParallelEdgeIndexFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A demo that shows how collections of vertices can be collapsed into a single vertex. In this
 * demo, the vertices that are collapsed are those mouse-selected by the user. Any criteria could be
 * used to form the vertex collections to be collapsed, perhaps some common characteristic of those
 * vertex objects.
 *
 * <p>Note that the collection types don't use generics in this demo, because the vertices are of
 * two types: String for plain vertices, and {@code Graph<String, Integer>} for the collapsed
 * vertices.
 *
 * @author Tom Nelson
 */
public class VertexCollapseDemoWithLayouts extends JPanel {

  private static final Logger log = LoggerFactory.getLogger(VertexCollapseDemoWithLayouts.class);

  String instructions =
      "<html>Use the mouse to select multiple vertices"
          + "<p>either by dragging a region, or by shift-clicking"
          + "<p>on multiple vertices."
          + "<p>After you select vertices, use the Collapse button"
          + "<p>to combine them into a single vertex."
          + "<p>Select a 'collapsed' vertex and use the Expand button"
          + "<p>to restore the collapsed vertices."
          + "<p>The Restore button will restore the original graph."
          + "<p>If you select 2 (and only 2) vertices, then press"
          + "<p>the Compress Edges button, parallel edges between"
          + "<p>those two vertices will no longer be expanded."
          + "<p>If you select 2 (and only 2) vertices, then press"
          + "<p>the Expand Edges button, parallel edges between"
          + "<p>those two vertices will be expanded."
          + "<p>You can drag the vertices with the mouse."
          + "<p>Use the 'Picking'/'Transforming' combo-box to switch"
          + "<p>between picking and transforming mode.</html>";
  /** the graph */
  Graph<Collapsable<?>, Integer> graph;

  /** the visual component and renderer for the graph */
  VisualizationViewer<Collapsable<?>, Integer> vv;

  LayoutAlgorithm<Collapsable<?>> layoutAlgorithm;

  GraphCollapser collapser;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public VertexCollapseDemoWithLayouts() {
    setLayout(new BorderLayout());
    // create a simple graph for the demo
    Graph<String, Integer> generatedGraph = TestGraphs.getOneComponentGraph();

    // make a graph of the same type but with Collapsable vertex types
    this.graph =
        GraphTypeBuilder.<Collapsable<?>, Integer>forGraphType(generatedGraph.getType())
            .edgeSupplier(SupplierUtil.createIntegerSupplier())
            .buildGraph();

    for (Integer edge : generatedGraph.edgeSet()) {
      Collapsable<?> source = Collapsable.of(generatedGraph.getEdgeSource(edge));
      Collapsable<?> target = Collapsable.of(generatedGraph.getEdgeTarget(edge));
      this.graph.addVertex(source);
      this.graph.addVertex(target);
      this.graph.addEdge(source, target, edge);
    }

    collapser = new GraphCollapser<>(graph);

    layoutAlgorithm = new FRLayoutAlgorithm<>();

    Dimension preferredSize = new Dimension(400, 400);

    final VisualizationModel<Collapsable<?>, Integer> visualizationModel =
        VisualizationModel.builder(graph)
            .layoutAlgorithm(layoutAlgorithm)
            .layoutSize(preferredSize)
            .build();

    // the regular graph mouse for the normal view
    final DefaultModalGraphMouse<Collapsable<?>, Number> graphMouse = new DefaultModalGraphMouse();

    vv =
        VisualizationViewer.builder(visualizationModel)
            .graphMouse(graphMouse)
            .viewSize(preferredSize)
            .build();

    vv.getRenderContext().setVertexShapeFunction(new ClusterShapeFunction());

    final Set exclusions = new HashSet();
    final PredicatedParallelEdgeIndexFunction eif =
        new PredicatedParallelEdgeIndexFunction(exclusions::contains);
    vv.getRenderContext().setParallelEdgeIndexFunction(eif);

    // add a listener for ToolTips
    vv.setVertexToolTipFunction(Object::toString);

    VisualizationScrollPane visualizationScrollPane = new VisualizationScrollPane(vv);
    add(visualizationScrollPane);

    JComboBox modeBox = graphMouse.getModeComboBox();
    modeBox.addItemListener(graphMouse.getModeListener());
    graphMouse.setMode(ModalGraphMouse.Mode.PICKING);

    LayoutHelper.Layouts[] combos = LayoutHelper.getCombos();
    final JRadioButton animateLayoutTransition = new JRadioButton("Animate Layout Transition");

    final JComboBox jcb = new JComboBox(combos);
    jcb.addItemListener(
        e -> {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            SwingUtilities.invokeLater(
                () -> {
                  LayoutHelper.Layouts layoutType = (LayoutHelper.Layouts) jcb.getSelectedItem();
                  LayoutAlgorithm layoutAlgorithm = layoutType.getLayoutAlgorithm();
                  log.trace("got a {}", layoutAlgorithm);
                  if ((layoutAlgorithm instanceof TreeLayout)
                      && vv.getVisualizationModel().getGraph().getType().isUndirected()) {
                    Graph tree =
                        SpanningTreeAdapter.getSpanningTree(vv.getVisualizationModel().getGraph());
                    LayoutModel positionModel = getTreeLayoutPositions(tree, layoutAlgorithm);
                    vv.getVisualizationModel().getLayoutModel().setInitializer(positionModel);
                    layoutAlgorithm = new StaticLayoutAlgorithm();
                  }
                  if (animateLayoutTransition.isSelected()) {
                    LayoutAlgorithmTransition.animate(vv, layoutAlgorithm);
                  } else {
                    LayoutAlgorithmTransition.apply(vv, layoutAlgorithm);
                  }
                });
          }
        });

    jcb.setSelectedItem(LayoutHelper.Layouts.FR);

    jcb.setSelectedItem(LayoutHelper.Layouts.FR);

    JButton collapse = new JButton("Collapse");
    collapse.addActionListener(
        e ->
            SwingUtilities.invokeLater(
                () -> {
                  Collection<Collapsable<?>> picked =
                      new HashSet(vv.getSelectedVertexState().getSelected());
                  if (picked.size() > 1) {
                    Graph<Collapsable<?>, Integer> inGraph = vv.getVisualizationModel().getGraph();
                    LayoutModel<Collapsable<?>> layoutModel =
                        vv.getVisualizationModel().getLayoutModel();
                    Graph<Collapsable<?>, Number> clusterGraph =
                        collapser.getClusterGraph(inGraph, picked);
                    log.trace("clusterGraph:" + clusterGraph);
                    Graph<Collapsable<?>, Integer> g = collapser.collapse(inGraph, clusterGraph);
                    log.trace("g:" + g);

                    double sumx = 0;
                    double sumy = 0;
                    for (Collapsable<?> v : picked) {
                      Point p = layoutModel.apply(v);
                      sumx += p.x;
                      sumy += p.y;
                    }
                    Point cp = Point.of(sumx / picked.size(), sumy / picked.size());
                    layoutModel
                        .getLayoutStateChangeSupport()
                        .fireLayoutStateChanged(layoutModel, true);
                    layoutModel.lock(false);
                    layoutModel.set(Collapsable.of(clusterGraph), cp);
                    log.trace("put the cluster at " + cp);
                    layoutModel.lock(Collapsable.of(clusterGraph), true);
                    vv.getVisualizationModel().setGraph(g);
                    vv.getRenderContext().getParallelEdgeIndexFunction().reset();
                    layoutModel.accept(vv.getVisualizationModel().getLayoutAlgorithm());
                    vv.getSelectedVertexState().clear();

                    vv.repaint();
                  }
                }));

    JButton expand = new JButton("Expand");
    expand.addActionListener(
        e ->
            SwingUtilities.invokeLater(
                () -> {
                  Collection<Collapsable<?>> picked =
                      new HashSet(vv.getSelectedVertexState().getSelected());
                  for (Collapsable<?> v : picked) {
                    if (v.get() instanceof Graph) {
                      Graph<Collapsable<?>, Integer> inGraph =
                          vv.getVisualizationModel().getGraph();
                      LayoutModel<Collapsable<?>> layoutModel =
                          vv.getVisualizationModel().getLayoutModel();
                      Graph<Collapsable<?>, Integer> g = collapser.expand(graph, inGraph, v);

                      layoutModel.lock(false);
                      vv.getVisualizationModel().setGraph(g);

                      vv.getRenderContext().getParallelEdgeIndexFunction().reset();
                    }
                    vv.getSelectedVertexState().clear();
                    vv.repaint();
                  }
                }));

    JButton compressEdges = new JButton("Compress Edges");
    compressEdges.addActionListener(
        e -> {
          Set picked = vv.getSelectedVertexState().getSelected();
          if (picked.size() == 2) {
            Iterator pickedIter = picked.iterator();
            Object vertexU = pickedIter.next();
            Object vertexV = pickedIter.next();
            Graph graph = vv.getVisualizationModel().getGraph();
            Collection edges = new HashSet(graph.edgesOf(vertexU));
            edges.retainAll(graph.edgesOf(vertexV));
            exclusions.addAll(edges);
            vv.repaint();
          }
        });

    JButton expandEdges = new JButton("Expand Edges");
    expandEdges.addActionListener(
        e -> {
          Set picked = vv.getSelectedVertexState().getSelected();
          if (picked.size() == 2) {
            Iterator pickedIter = picked.iterator();
            Object vertexU = pickedIter.next();
            Object vertexV = pickedIter.next();
            Graph graph = vv.getVisualizationModel().getGraph();
            Collection edges = new HashSet(graph.edgesOf(vertexU));
            edges.retainAll(graph.edgesOf(vertexV));
            exclusions.removeAll(edges);
            vv.repaint();
          }
        });

    JButton reset = new JButton("Reset");
    reset.addActionListener(
        e -> {
          vv.getVisualizationModel().setGraph(graph);
          exclusions.clear();
          vv.repaint();
        });

    JButton help = new JButton("Help");
    help.addActionListener(
        e ->
            JOptionPane.showMessageDialog(
                (JComponent) e.getSource(), instructions, "Help", JOptionPane.PLAIN_MESSAGE));

    JPanel controls = new JPanel(new FlowLayout());
    //    controls.add(ControlHelpers.getZoomControls(vv, "Zoom"));
    JPanel collapseControls = new JPanel(new GridLayout(0, 1));
    collapseControls.setBorder(BorderFactory.createTitledBorder("Picked"));
    collapseControls.add(collapse);
    collapseControls.add(expand);
    collapseControls.add(compressEdges);
    collapseControls.add(expandEdges);
    collapseControls.add(reset);
    controls.add(collapseControls);
    JPanel controlPanel = new JPanel(new GridLayout(0, 1));
    JPanel modePanel = new JPanel();
    modePanel.setBorder(new TitledBorder("Mouse Mode"));
    modePanel.add(modeBox);

    controlPanel.add(modePanel);

    JPanel jcbPanel = new JPanel(new GridLayout(0, 1));
    jcbPanel.setBorder(new TitledBorder("Layouts"));
    jcbPanel.add(jcb);

    jcbPanel.add(animateLayoutTransition);
    controlPanel.add(jcbPanel);

    controls.add(controlPanel);
    controls.add(help);
    add(controls, BorderLayout.SOUTH);
  }

  LayoutModel getTreeLayoutPositions(Graph tree, LayoutAlgorithm treeLayout) {
    LayoutModel model = LayoutModel.builder().size(600, 600).graph(tree).build();
    model.accept(treeLayout);
    return model;
  }

  /**
   * a demo class that will create a vertex shape that is either a polygon or star. The number of
   * sides corresponds to the number of vertices that were collapsed into the vertex represented by
   * this shape.
   */
  static class ClusterShapeFunction extends EllipseShapeFunction<Collapsable<?>> {

    ClusterShapeFunction() {
      setSizeFunction(new ClusterSizeFunction(20));
    }

    @Override
    public Shape apply(Collapsable<?> v) {
      if (v.get() instanceof Graph) {
        int size = ((Graph) v.get()).vertexSet().size();
        if (size < 8) {
          int sides = Math.max(size, 3);
          return factory.getRegularPolygon(v, sides);
        } else {
          return factory.getRegularStar(v, size);
        }
      }
      return super.apply(v);
    }
  }

  /**
   * A demo class that will make vertices larger if they represent a collapsed collection of
   * original vertices
   */
  static class ClusterSizeFunction implements Function<Collapsable<?>, Integer> {
    int size;

    public ClusterSizeFunction(Integer size) {
      this.size = size;
    }

    public Integer apply(Collapsable<?> v) {
      if (v.get() instanceof Graph) {
        return 30;
      }
      return size;
    }
  }

  public static void main(String[] args) {
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    f.getContentPane().add(new VertexCollapseDemoWithLayouts());
    f.pack();
    f.setVisible(true);
  }
}
