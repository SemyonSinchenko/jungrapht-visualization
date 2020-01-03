package org.jungrapht.visualization.layout.algorithms.brandeskopf;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jungrapht.visualization.layout.algorithms.sugiyama.SugiyamaEdge;
import org.jungrapht.visualization.layout.algorithms.sugiyama.SugiyamaVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VerticalAlignment<V, E> {

  private static final Logger log = LoggerFactory.getLogger(VerticalAlignment.class);

  protected Map<SugiyamaVertex<V>, SugiyamaVertex<V>> rootMap = new HashMap<>();
  protected Map<SugiyamaVertex<V>, SugiyamaVertex<V>> alignMap = new HashMap<>();
  protected SugiyamaVertex<V>[][] layers;
  protected Graph<SugiyamaVertex<V>, SugiyamaEdge<V, E>> svGraph;
  protected Set<SugiyamaEdge<V, E>> markedSegments;

  public Map<SugiyamaVertex<V>, SugiyamaVertex<V>> getRootMap() {
    return rootMap;
  }

  public Map<SugiyamaVertex<V>, SugiyamaVertex<V>> getAlignMap() {
    return alignMap;
  }

  public abstract void align();

  protected VerticalAlignment(
      SugiyamaVertex<V>[][] layers,
      Graph<SugiyamaVertex<V>, SugiyamaEdge<V, E>> svGraph,
      Set<SugiyamaEdge<V, E>> markedSegments) {
    this.layers = layers;
    this.svGraph = svGraph;
    this.markedSegments = markedSegments;
    // initialize root and align
    Arrays.stream(layers)
        .flatMap(Arrays::stream)
        .forEach(
            v -> {
              rootMap.put(v, v);
              alignMap.put(v, v);
            });
    align();
  }

  boolean notMarked(SugiyamaEdge<V, E> edge) {
    return !markedSegments.contains(edge);
  }

  SugiyamaVertex<V> root(SugiyamaVertex<V> v) {
    return rootMap.get(v);
  }

  void root(SugiyamaVertex<V> k, SugiyamaVertex<V> v) {
    rootMap.put(k, v);
  }

  SugiyamaVertex<V> align(SugiyamaVertex<V> v) {
    return alignMap.get(v);
  }

  void align(SugiyamaVertex<V> k, SugiyamaVertex<V> v) {
    alignMap.put(k, v);
  }

  int pos(SugiyamaVertex<V> v) {
    return v.getIndex();
  }

  /**
   * start at first layer, work down, looking at predecessors
   *
   * @param <V>
   * @param <E>
   */
  public static class LeftmostUpper<V, E> extends VerticalAlignment<V, E> {

    public LeftmostUpper(
        SugiyamaVertex<V>[][] layers,
        Graph<SugiyamaVertex<V>, SugiyamaEdge<V, E>> svGraph,
        Set<SugiyamaEdge<V, E>> markedSegments) {
      super(layers, svGraph, markedSegments);
    }

    @Override
    public void align() {
      //    for (int i=1; i< layers.size(); i++) {
      for (int i = 0; i <= layers.length - 1; i++) { // zero based
        int r = -1;
        SugiyamaVertex<V>[] currentLayer = layers[i];
        //      for (int k=1; k <= currentLayer.size(); k++) {
        for (int k = 0; k <= currentLayer.length - 1; k++) { // zero based
          // if the vertex at k has source nodes
          SugiyamaVertex<V> vkofi = currentLayer[k];
          List<SugiyamaVertex<V>> neighbors =
              Graphs.predecessorListOf(svGraph, vkofi)
                  .stream()
                  .sorted(Comparator.comparingInt(SugiyamaVertex::getIndex))
                  .collect(Collectors.toList());
          log.trace("predecessors of {} are {}", vkofi, neighbors);
          int d = neighbors.size();
          if (d > 0) {
            int floor = (int) Math.floor((d - 1) / 2.0); // zero based
            int ceil = (int) Math.ceil((d - 1) / 2.0); // zero based
            log.trace("ceil: {}, floor: {}", ceil, floor);
            for (int m : new LinkedHashSet<>(Arrays.asList(floor, ceil))) {
              if (align(vkofi) == vkofi) {
                SugiyamaVertex<V> um = neighbors.get(m);
                // if edge um->vkofi is not marked
                SugiyamaEdge<V, E> edge = svGraph.getEdge(um, vkofi);
                if (notMarked(edge) && r < pos(um)) {
                  // align[um] <- vkofi
                  align(um, vkofi);
                  // root[vkofi] <- root[um]
                  root(vkofi, root(um));
                  // align[vkofi] <- root[vkofi]
                  align(vkofi, root(vkofi));
                  // r = pos[um]
                  r = pos(um);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * start at last layer, work upwards looking at successor positions
   *
   * @param <V>
   * @param <E>
   */
  public static class LeftmostLower<V, E> extends VerticalAlignment<V, E> {
    public LeftmostLower(
        SugiyamaVertex<V>[][] layers,
        Graph<SugiyamaVertex<V>, SugiyamaEdge<V, E>> svGraph,
        Set<SugiyamaEdge<V, E>> markedSegments) {
      super(layers, svGraph, markedSegments);
    }

    @Override
    public void align() {
      //    for (int i=1; i< layers.size(); i++) {
      for (int i = layers.length - 2; i >= 0; i--) { // zero based
        //    for (int i = 0; i < layers.size() - 1; i++) { // zero based
        int r = -1;
        SugiyamaVertex<V>[] currentLayer = layers[i];
        //      for (int k=1; k <= currentLayer.size(); k++) {
        for (int k = 0; k <= currentLayer.length - 1; k++) { // zero based
          // if the vertex at k has source nodes
          SugiyamaVertex<V> vkofi = currentLayer[k];
          List<SugiyamaVertex<V>> neighbors =
              Graphs.successorListOf(svGraph, vkofi)
                  .stream()
                  .sorted(Comparator.comparingInt(SugiyamaVertex::getIndex))
                  .collect(Collectors.toList());
          log.trace("successors of {} are {}", vkofi, neighbors);
          int d = neighbors.size();
          if (d > 0) {
            int floor = (int) Math.floor((d - 1) / 2.0); // zero based
            int ceil = (int) Math.ceil((d - 1) / 2.0); // zero based
            for (int m : new LinkedHashSet<>(Arrays.asList(floor, ceil))) {
              if (align(vkofi) == vkofi) {
                SugiyamaVertex<V> um = neighbors.get(m);
                // if edge um->vkofi is not marked
                SugiyamaEdge<V, E> edge = svGraph.getEdge(vkofi, um);
                if (notMarked(edge) && r < pos(um)) {
                  align(um, vkofi);
                  root(vkofi, root(um));
                  align(vkofi, root(vkofi));
                  r = pos(um);
                }
              }
            }
          }
        }
      }
    }
  }

  public static class RightmostUpper<V, E> extends VerticalAlignment<V, E> {
    public RightmostUpper(
        SugiyamaVertex<V>[][] layers,
        Graph<SugiyamaVertex<V>, SugiyamaEdge<V, E>> svGraph,
        Set<SugiyamaEdge<V, E>> markedSegments) {
      super(layers, svGraph, markedSegments);
    }

    @Override
    public void align() {
      //    for (int i=1; i< layers.size(); i++) {
      for (int i = 1; i <= layers.length - 1; i++) { // zero based
        SugiyamaVertex<V>[] currentLayer = layers[i];
        SugiyamaVertex<V>[] previousLayer = layers[i - 1];
        int r = previousLayer.length + 1;
        //              for (int k=0; k <= currentLayer.size()-1; k++) {
        for (int k = currentLayer.length - 1; k >= 0; k--) {
          // if the vertex at k has source nodes
          SugiyamaVertex<V> vkofi = currentLayer[k];
          List<SugiyamaVertex<V>> neighbors =
              Graphs.predecessorListOf(svGraph, vkofi)
                  .stream()
                  .sorted(Comparator.comparingInt(SugiyamaVertex::getIndex))
                  .collect(Collectors.toList());
          int d = neighbors.size();
          if (d > 0) {
            int floor = (int) Math.floor((d - 1) / 2.0); // zero based
            int ceil = (int) Math.ceil((d - 1) / 2.0); // zero based
            for (int m : new LinkedHashSet<>(Arrays.asList(ceil, floor))) {
              if (align(vkofi) == vkofi) {
                SugiyamaVertex<V> um = neighbors.get(m);
                // if edge um->vkofi is not marked
                SugiyamaEdge<V, E> edge = svGraph.getEdge(um, vkofi);
                if (notMarked(edge) && r > pos(um)) {
                  align(um, vkofi);
                  root(vkofi, root(um));
                  align(vkofi, root(vkofi));
                  r = pos(um);
                }
              }
            }
          }
        }
      }
    }
  }

  public static class RightmostLower<V, E> extends VerticalAlignment<V, E> {
    public RightmostLower(
        SugiyamaVertex<V>[][] layers,
        Graph<SugiyamaVertex<V>, SugiyamaEdge<V, E>> svGraph,
        Set<SugiyamaEdge<V, E>> markedSegments) {
      super(layers, svGraph, markedSegments);
    }

    @Override
    public void align() {
      //    for (int i=1; i< layers.size(); i++) {
      for (int i = layers.length - 2; i >= 0; i--) { // zero based
        //                for (int i = 0; i < layers.size() - 1; i++) { // zero based
        SugiyamaVertex<V>[] currentLayer = layers[i];
        SugiyamaVertex<V>[] nextLayer = layers[i + 1];
        int r = nextLayer.length + 1;
        //      for (int k=1; k <= currentLayer.size(); k++) {
        for (int k = currentLayer.length - 1; k >= 0; k--) {
          //                    for (int k = 0; k <= currentLayer.size() - 1; k++) { // zero based
          // if the vertex at k has target nodes
          SugiyamaVertex<V> vkofi = currentLayer[k];
          List<SugiyamaVertex<V>> neighbors =
              Graphs.successorListOf(svGraph, vkofi)
                  .stream()
                  .sorted(Comparator.comparingInt(SugiyamaVertex::getIndex))
                  .collect(Collectors.toList());
          int d = neighbors.size();
          if (d > 0) {
            int floor = (int) Math.floor((d - 1) / 2.0); // zero based
            int ceil = (int) Math.ceil((d - 1) / 2.0); // zero based
            for (int m : new LinkedHashSet<>(Arrays.asList(ceil, floor))) {
              if (align(vkofi) == vkofi) {
                SugiyamaVertex<V> um = neighbors.get(m);
                // if edge vm->v is not marked
                SugiyamaEdge<V, E> edge = svGraph.getEdge(vkofi, um);
                if (notMarked(edge) && r > pos(um)) {
                  align(um, vkofi);
                  root(vkofi, root(um));
                  align(vkofi, root(vkofi));
                  r = pos(um);
                }
              }
            }
          }
        }
      }
    }
  }
}
