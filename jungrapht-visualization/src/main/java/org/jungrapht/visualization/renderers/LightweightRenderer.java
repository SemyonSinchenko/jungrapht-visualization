package org.jungrapht.visualization.renderers;

import org.jungrapht.visualization.RenderContext;
import org.jungrapht.visualization.VisualizationModel;
import org.jungrapht.visualization.spatial.Spatial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <V> vertex type
 * @param <E> edge type
 */
public class LightweightRenderer<V, E> implements Renderer<V, E> {

  private static final Logger log = LoggerFactory.getLogger(LightweightRenderer.class);
  protected Vertex<V, E> vertexRenderer = new LightweightVertexRenderer<>();
  protected Renderer.Edge<V, E> edgeRenderer = new LightweightEdgeRenderer<>();

  @Override
  public void render(
      RenderContext<V, E> renderContext,
      VisualizationModel<V, E> visualizationModel,
      Spatial<V> vertexSpatial,
      Spatial<E> edgeSpatial) {
    // BiModalRenderer handles this
  }

  @Override
  public void render(
      RenderContext<V, E> renderContext, VisualizationModel<V, E> visualizationModel) {
    // BiModalRenderer handles this
  }

  @Override
  public void renderVertex(
      RenderContext<V, E> renderContext, VisualizationModel<V, E> visualizationModel, V v) {
    vertexRenderer.paintVertex(renderContext, visualizationModel, v);
  }

  @Override
  public void renderVertexLabel(
      RenderContext<V, E> renderContext, VisualizationModel<V, E> visualizationModel, V v) {}

  @Override
  public void renderEdge(
      RenderContext<V, E> renderContext, VisualizationModel<V, E> visualizationModel, E e) {
    edgeRenderer.paintEdge(renderContext, visualizationModel, e);
  }

  @Override
  public void renderEdgeLabel(
      RenderContext<V, E> renderContext, VisualizationModel<V, E> visualizationModel, E e) {}

  @Override
  public void setVertexRenderer(Vertex<V, E> r) {
    this.vertexRenderer = r;
  }

  @Override
  public void setEdgeRenderer(Renderer.Edge<V, E> r) {
    this.edgeRenderer = r;
  }

  /** @return the edgeLabelRenderer */
  @Override
  public Renderer.EdgeLabel<V, E> getEdgeLabelRenderer() {
    return null;
  }

  /** @param edgeLabelRenderer the edgeLabelRenderer to set */
  @Override
  public void setEdgeLabelRenderer(Renderer.EdgeLabel<V, E> edgeLabelRenderer) {}

  /** @return the vertexLabelRenderer */
  @Override
  public VertexLabel<V, E> getVertexLabelRenderer() {
    return null;
  }

  /** @param vertexLabelRenderer the vertexLabelRenderer to set */
  @Override
  public void setVertexLabelRenderer(VertexLabel<V, E> vertexLabelRenderer) {}

  /** @return the edgeRenderer */
  @Override
  public Renderer.Edge<V, E> getEdgeRenderer() {
    return edgeRenderer;
  }

  /** @return the vertexRenderer */
  @Override
  public Vertex<V, E> getVertexRenderer() {
    return vertexRenderer;
  }
}
