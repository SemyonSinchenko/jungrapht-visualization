package org.jungrapht.visualization.layout.algorithms.eiglsperger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GraphLayers {

  private static final Logger log = LoggerFactory.getLogger(GraphLayers.class);

  private GraphLayers() {}

  public static <V, E> List<List<LV<V>>> assign(Graph<LV<V>, LE<V, E>> dag) {
    int rank = 0;
    List<List<LV<V>>> sorted = new ArrayList<>();
    List<LE<V, E>> edges = dag.edgeSet().stream().collect(Collectors.toCollection(LinkedList::new));
    List<LV<V>> vertices =
        dag.vertexSet().stream().collect(Collectors.toCollection(LinkedList::new));
    List<LV<V>> start =
        getVerticesWithoutIncomingEdges(dag, edges, vertices); // should be the roots

    // if there are multiple components, arrange the first row order to group their roots
    ConnectivityInspector<LV<V>, ?> connectivityInspector = new ConnectivityInspector<>(dag);
    List<Set<LV<V>>> componentVertices = connectivityInspector.connectedSets();

    if (componentVertices.size() > 1) {
      start = groupByComponentMembership(componentVertices, start);
    }

    // sort the first layer so that isolated vertices and loop vertices are grouped together and at
    // one end of the rank
    start.sort(Comparator.comparingInt(v -> vertexIsolationScore(dag, v)));

    while (start.size() > 0) {
      for (int i = 0; i < start.size(); i++) {
        LV<V> v = start.get(i);
        v.setRank(rank);
        v.setIndex(i);
      }
      sorted.add(start); // add a row
      Set<LV<V>> fstart = new HashSet<>(start);
      // remove any edges that start in the new row
      edges.removeIf(e -> fstart.contains(dag.getEdgeSource(e)));
      // remove any vertices that have been added to the row
      vertices.removeIf(fstart::contains);
      start = getVerticesWithoutIncomingEdges(dag, edges, vertices);
      if (componentVertices.size() > 1) {
        start = groupByComponentMembership(componentVertices, start);
      }
      rank++;
    }
    return sorted;
  }

  private static <V> List<LV<V>> groupByComponentMembership(
      List<Set<LV<V>>> componentVertices, List<LV<V>> list) {
    List<LV<V>> groupedRow = new ArrayList<>();
    for (Set<LV<V>> set : componentVertices) {
      groupedRow.addAll(list.stream().filter(set::contains).collect(Collectors.toList()));
    }
    return groupedRow;
  }

  /**
   * Find all vertices that have no incoming edges by
   *
   * <p>
   *
   * <ul>
   *   <li>collect all vertices that are edge targets
   *   <li>collect all vertices that are not part of that set of targets
   * </ul>
   *
   * Note that loop edges have already been removed from the graph, so any vertices that have only
   * loop edges will appear in the set of vertices without incoming edges.
   *
   * @param dag the {@code Graph} to examine
   * @param edges all edges from the {@code Graph}. Note that loop edges have already been removed
   *     from this set
   * @param vertices all vertices in the {@code Graph} (including vertices that have only loop edges
   * @param <V> vertex type
   * @param <E> edge type
   * @return vertices in the graph that have no incoming edges (or only loop edges)
   */
  private static <V, E> List<LV<V>> getVerticesWithoutIncomingEdges(
      Graph<LV<V>, LE<V, E>> dag, Collection<LE<V, E>> edges, Collection<LV<V>> vertices) {
    // get targets of all edges
    Set<LV<V>> targets = edges.stream().map(e -> dag.getEdgeTarget(e)).collect(Collectors.toSet());
    // from vertices, filter out any that are an edge target
    return vertices.stream().filter(v -> !targets.contains(v)).collect(Collectors.toList());
  }

  public static <V> void checkLayers(List<List<LV<V>>> layers) {
    for (int i = 0; i < layers.size(); i++) {
      List<LV<V>> layer = layers.get(i);
      for (int j = 0; j < layer.size(); j++) {
        LV<V> LV = layer.get(j);
        if (i != LV.getRank()) {
          log.error("{} is not the rank of {}", i, LV);
          throw new RuntimeException("rank is wrong");
        }
        if (j != LV.getIndex()) {
          log.error("{} is not the index of {}", j, LV);
          throw new RuntimeException("index is wrong");
        }
      }
    }
  }

  public static <V> void checkLayers(LV<V>[][] layers) {
    if (log.isTraceEnabled()) {
      for (int i = 0; i < layers.length; i++) {
        for (int j = 0; j < layers[i].length; j++) {
          if (i != layers[i][j].getRank()) {
            log.error("{} is not the rank of {}", i, layers[i][j]);
            throw new RuntimeException(i + " is not the rank of " + layers[i][j]);
          }
          if (j != layers[i][j].getIndex()) {
            log.error("{} is not the index of {}", j, layers[i][j]);
            throw new RuntimeException(j + " is not the index of " + layers[i][j]);
          }
        }
      }
    }
  }

  public static <V, E> boolean isLoopVertex(Graph<V, E> graph, V v) {
    return graph.outgoingEdgesOf(v).equals(graph.incomingEdgesOf(v));
  }

  public static <V, E> boolean isZeroDegreeVertex(Graph<V, E> graph, V v) {
    return graph.degreeOf(v) == 0;
  }

  public static <V, E> boolean isIsolatedVertex(Graph<V, E> graph, V v) {
    return isLoopVertex(graph, v) || isZeroDegreeVertex(graph, v);
  }

  /**
   * to set vertex order to normal -> loop -> zeroDegree
   *
   * @param graph
   * @param v
   * @param <V>
   * @param <E>
   * @return
   */
  public static <V, E> int vertexIsolationScore(Graph<V, E> graph, V v) {
    if (isZeroDegreeVertex(graph, v)) return 2;
    if (isLoopVertex(graph, v)) return 1;
    return 0;
  }
}
