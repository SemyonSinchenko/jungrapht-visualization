package org.jungrapht.visualization.layout.algorithms.util.sugiyama;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssignLayers<V, E> {

  private static final Logger log = LoggerFactory.getLogger(AssignLayers.class);
  protected Graph<SV<V>, SE<V, E>> dag;
  //  Map<SV<V>, SVData> svData = new HashMap<>();
  //  Map<SE<V,E>, SEData> seData = new HashMap<>();

  public AssignLayers(Graph<SV<V>, SE<V, E>> dag) {
    this.dag = dag;
  }

  //  public Map<SV<V>, SVData> getSVData() {
  //    return svData;
  //  }
  //
  //  public Map<SE<V, E>, SEData> getSeData() {
  //    return seData;
  //  }

  public List<List<SV<V>>> assignLayers() {
    int rank = 0;
    List<List<SV<V>>> sorted = new ArrayList<>();
    List<SE<V, E>> edges = dag.edgeSet().stream().collect(Collectors.toCollection(LinkedList::new));
    List<SV<V>> vertices =
        dag.vertexSet().stream().collect(Collectors.toCollection(LinkedList::new));
    List<SV<V>> start = getVerticesWithoutIncomingEdges(edges, vertices); // should be the roots
    //    log.info("start is {}", start);
    while (start.size() > 0) {
      for (int i = 0; i < start.size(); i++) {
        SV<V> v = start.get(i);
        v.rank = rank;
        v.index = i;
        //        svData.put(v, SVData.of(rank, i));
      }
      sorted.add(start); // add a row
      Set<SV<V>> fstart = new HashSet<>(start);
      // remove any edges that start in the new row
      edges.removeIf(e -> fstart.contains(dag.getEdgeSource(e)));
      // remove any vertices that have been added to the row
      vertices.removeIf(v -> fstart.contains(v));
      start = getVerticesWithoutIncomingEdges(edges, vertices);
      rank++;
      //      log.info("start is {}  rank:{}, ", start, rank);
    }
    return sorted;
  }

  List<SV<V>> getVerticesWithoutIncomingEdges(
      Collection<SE<V, E>> edges, Collection<SV<V>> vertices) {
    // get targets of all edges
    Set<SV<V>> targets = edges.stream().map(e -> dag.getEdgeTarget(e)).collect(Collectors.toSet());
    // from vertices, filter out any that are an edge target
    return vertices.stream().filter(v -> !targets.contains(v)).collect(Collectors.toList());
  }

  public static <V> void checkLayers(List<List<SV<V>>> layers) {
    for (int i = 0; i < layers.size(); i++) {
      List<SV<V>> layer = layers.get(i);
      //      log.info("layer: {}", layer);
      for (int j = 0; j < layer.size(); j++) {
        SV<V> sv = layer.get(j);
        //        log.info("sv {},{}: {}", i, j, sv);
        assert i == sv.getRank();
        assert j == sv.getIndex();
      }
    }
  }
}