package org.jungrapht.visualization.layout.algorithms.util.sugiyama;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jungrapht.visualization.layout.model.Point;

public class ArticulatedEdge<V, E> extends SE<V, E> {

  public static <V, E> ArticulatedEdge of(SE<V, E> edge, SV<V> source, SV<V> target) {

    return new ArticulatedEdge(edge, source, target);
  }

  protected ArticulatedEdge(SE<V, E> edge, SV<V> source, SV<V> target) {

    super(edge.edge, source, target);
    this.se = edge;
  }

  /**
   * two synthetic edges are created by splitting an existing SE<V,E> edge. This is a reference to
   * that edge The edge what was split will gain an intermediate vertex between the source and
   * target vertices each time it or one of its split-off edges is further split
   */
  protected SE<V, E> se;

  protected final List<SV<V>> intermediateVertices = new ArrayList<>();
  protected final List<Point> intermediatePoints = new ArrayList<>();

  public List<Point> getIntermediatePoints() {
    return intermediatePoints;
  }

  public void addIntermediateVertex(SV<V> v) {
    intermediateVertices.add(v);
  }

  public void addIntermediatePoint(Point p) {
    intermediatePoints.add(p);
  }

  public List<SV<V>> getIntermediateVertices() {
    return Collections.unmodifiableList(intermediateVertices);
  }

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public String toString() {
    return "ArticulatedEdge{"
        + "edge="
        + edge
        + ", source="
        + source
        + ", intermediateVertices="
        + intermediateVertices
        + ", target="
        + target
        + '}';
  }
}