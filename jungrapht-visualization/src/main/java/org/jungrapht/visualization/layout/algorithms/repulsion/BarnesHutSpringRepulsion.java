package org.jungrapht.visualization.layout.algorithms.repulsion;

import com.google.common.cache.LoadingCache;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jungrapht.visualization.layout.algorithms.SpringLayoutAlgorithm;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.layout.quadtree.BarnesHutQuadTree;
import org.jungrapht.visualization.layout.quadtree.ForceObject;
import org.jungrapht.visualization.layout.quadtree.Node;

/**
 * @author Tom Nelson
 * @param <V> the vertex type
 */
public class BarnesHutSpringRepulsion<V>
    extends StandardSpringRepulsion<
        V, BarnesHutSpringRepulsion<V>, BarnesHutSpringRepulsion.Builder<V>>
    implements BarnesHutRepulsion<
        V, BarnesHutSpringRepulsion<V>, BarnesHutSpringRepulsion.Builder<V>> {

  public static class Builder<V>
      extends StandardSpringRepulsion.Builder<
          V, BarnesHutSpringRepulsion<V>, BarnesHutSpringRepulsion.Builder<V>>
      implements BarnesHutRepulsion.Builder<
          V, BarnesHutSpringRepulsion<V>, BarnesHutSpringRepulsion.Builder<V>> {

    private double theta = Node.DEFAULT_THETA;
    private BarnesHutQuadTree<V> tree = BarnesHutQuadTree.<V>builder().build();

    public Builder<V> layoutModel(LayoutModel<V> layoutModel) {
      this.layoutModel = layoutModel;
      this.tree =
          BarnesHutQuadTree.<V>builder()
              .bounds(layoutModel.getWidth(), layoutModel.getHeight())
              .theta(theta)
              .build();
      return this;
    }

    public Builder<V> theta(double theta) {
      this.theta = theta;
      return this;
    }

    public Builder<V> nodeData(
        LoadingCache<V, SpringLayoutAlgorithm.SpringVertexData> springVertexData) {
      this.springVertexData = springVertexData;
      return this;
    }

    public Builder<V> repulsionRangeSquared(int repulsionRangeSquared) {
      this.repulsionRangeSquared = repulsionRangeSquared;
      return this;
    }

    @Override
    public Builder<V> random(Random random) {
      this.random = random;
      return this;
    }

    public BarnesHutSpringRepulsion<V> build() {
      return new BarnesHutSpringRepulsion(this);
    }
  }

  protected BarnesHutQuadTree<V> tree;

  public static Builder barnesHutBuilder() {
    return new Builder();
  }

  protected BarnesHutSpringRepulsion(Builder<V> builder) {
    super(builder);
    this.tree = builder.tree;
  }

  public void step() {
    tree.rebuild(
        layoutModel
            .getLocations()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry ->
                        org.jungrapht.visualization.layout.quadtree.Point.of(
                            entry.getValue().x, entry.getValue().y))));
  }

  public void calculateRepulsion() {
    Graph<V, ?> graph = layoutModel.getGraph();

    try {
      for (V vertex : graph.vertexSet()) {

        if (layoutModel.isLocked(vertex)) {
          continue;
        }

        SpringLayoutAlgorithm.SpringVertexData svd = springVertexData.getUnchecked(vertex);
        if (svd == null) {
          continue;
        }
        Point forcePoint = layoutModel.apply(vertex);
        ForceObject<V> nodeForceObject =
            new ForceObject(vertex, forcePoint.x, forcePoint.y) {
              @Override
              protected void addForceFrom(ForceObject other) {

                if (other == null || vertex == other.getElement()) {
                  return;
                }
                org.jungrapht.visualization.layout.quadtree.Point p = this.p;
                org.jungrapht.visualization.layout.quadtree.Point p2 = other.p;
                if (p == null || p2 == null) {
                  return;
                }
                double vx = p.x - p2.x;
                double vy = p.y - p2.y;
                double distanceSq = p.distanceSquared(p2);
                if (distanceSq == 0) {
                  f = f.add(random.nextDouble(), random.nextDouble());
                } else if (distanceSq < repulsionRangeSquared) {
                  double factor = 1;
                  f = f.add(factor * vx / distanceSq, factor * vy / distanceSq);
                }
              }
            };
        tree.applyForcesTo(nodeForceObject);
        Point f = Point.of(nodeForceObject.f.x, nodeForceObject.f.y);
        double dlen = f.x * f.x + f.y * f.y;
        if (dlen > 0) {
          dlen = Math.sqrt(dlen) / 2;
          svd.repulsiondx += f.x / dlen;
          svd.repulsiondy += f.y / dlen;
        }
      }
    } catch (ConcurrentModificationException cme) {
      calculateRepulsion();
    }
  }
}
