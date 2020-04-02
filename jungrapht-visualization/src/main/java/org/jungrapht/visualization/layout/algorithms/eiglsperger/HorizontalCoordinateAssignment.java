package org.jungrapht.visualization.layout.algorithms.eiglsperger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jungrapht.visualization.layout.algorithms.sugiyama.AverageMedian;
import org.jungrapht.visualization.layout.algorithms.sugiyama.LE;
import org.jungrapht.visualization.layout.algorithms.sugiyama.LV;
import org.jungrapht.visualization.layout.model.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see "Fast and Simple Horizontal Coordinate Assignment, Ulrik Brandes and Boris Köpf, Department
 *     of Computer & Information Science, University of Konstanz"
 */
public class HorizontalCoordinateAssignment<V, E>
    extends org.jungrapht.visualization.layout.algorithms.sugiyama.HorizontalCoordinateAssignment<
        V, E> {

  private static Logger log = LoggerFactory.getLogger(HorizontalCoordinateAssignment.class);

  public HorizontalCoordinateAssignment(
      LV<V>[][] layers,
      Graph<LV<V>, LE<V, E>> svGraph,
      Set<LE<V, E>> markedSegments,
      int horizontalOffset,
      int verticalOffset) {
    super(layers, svGraph, markedSegments, horizontalOffset, verticalOffset);
  }

  public void horizontalCoordinateAssignment() {
    VerticalAlignment.LeftmostUpper<V, E> upLeft =
        new VerticalAlignment.LeftmostUpper<>(layers, svGraph, markedSegments);
    upLeft.align();

    HorizontalCompaction<V> upLeftCompaction =
        new HorizontalCompaction<>(
            layers, upLeft.getRootMap(), upLeft.getAlignMap(), horizontalOffset, verticalOffset);
    if (log.isInfoEnabled()) {
      log.info("upLeft");
      log.info("alignMap:{}", upLeft.getAlignMap());
      log.info("rootMap:{}", upLeft.getRootMap());
      log.info("shift:{}", upLeftCompaction.getShift());
      log.info("sink:{}", upLeftCompaction.getSink());
    }

    VerticalAlignment.RightmostUpper<V, E> upRight =
        new VerticalAlignment.RightmostUpper<>(layers, svGraph, markedSegments);
    upRight.align();
    HorizontalCompaction<V> upRightCompaction =
        new HorizontalCompaction<>(
            layers, upRight.getRootMap(), upRight.getAlignMap(), horizontalOffset, verticalOffset);
    if (log.isInfoEnabled()) {
      log.info("upRight");
      log.info("alignMap:{}", upRight.getAlignMap());
      log.info("rootMap:{}", upRight.getRootMap());
      log.info("shift:{}", upRightCompaction.getShift());
      log.info("sink:{}", upRightCompaction.getSink());
    }

    VerticalAlignment.LeftmostLower<V, E> downLeft =
        new VerticalAlignment.LeftmostLower<>(layers, svGraph, markedSegments);
    downLeft.align();
    HorizontalCompaction<V> downLeftCompaction =
        new HorizontalCompaction<>(
            layers,
            downLeft.getRootMap(),
            downLeft.getAlignMap(),
            horizontalOffset,
            verticalOffset);
    if (log.isInfoEnabled()) {
      log.info("downLeft");
      log.info("alignMap:{}", downLeft.getAlignMap());
      log.info("rootMap:{}", downLeft.getRootMap());
      log.info("shift:{}", downLeftCompaction.getShift());
      log.info("sink:{}", downLeftCompaction.getSink());
    }

    VerticalAlignment.RightmostLower<V, E> downRight =
        new VerticalAlignment.RightmostLower<>(layers, svGraph, markedSegments);
    downRight.align();
    HorizontalCompaction<V> downRightCompaction =
        new HorizontalCompaction<>(
            layers,
            downRight.getRootMap(),
            downRight.getAlignMap(),
            horizontalOffset,
            verticalOffset);
    if (log.isInfoEnabled()) {
      log.info("downRight");
      log.info("alignMap:{}", downRight.getAlignMap());
      log.info("rootMap:{}", downRight.getRootMap());
      log.info("shift:{}", downRightCompaction.getShift());
      log.info("sink:{}", downRightCompaction.getSink());
    }

    for (int i = 0; i < layers.length; i++) {
      for (int j = 0; j < layers[i].length; j++) {
        LV<V> v = layers[i][j];
        Point upLeftPoint = upLeftCompaction.getPoint(v).add(horizontalOffset, verticalOffset);
        Point upRightPoint = upRightCompaction.getPoint(v).add(horizontalOffset, verticalOffset);
        Point downLeftPoint = downLeftCompaction.getPoint(v).add(horizontalOffset, verticalOffset);
        Point downRightPoint =
            downRightCompaction.getPoint(v).add(horizontalOffset, verticalOffset);

        Point balancedPoint =
            AverageMedian.averageMedianPoint(
                upLeftPoint, upRightPoint, downLeftPoint, downRightPoint);
        v.setPoint(balancedPoint);
      }
    }
  }

  /**
   * override to use pos instead of index
   *
   * @param v vertex to consider
   * @return v's pos (not its index in the rank)
   */
  @Override
  protected int pos(LV<V> v) {
    return v.getPos();
  }

  protected int idx(LV<V> v) {
    return v.getIndex();
  }

  /**
   * override to say that only QVertices are incident to an inner edge that spans from previous rank
   * to this one
   *
   * @param v vertex to check
   * @return true iv v is incident to an inner segment between v's rank and the preceding rank
   */
  @Override
  protected boolean incidentToInnerSegment(LV<V> v) {
    return v instanceof QVertex;
  }

  protected List<LV<V>> misAligned(Map<LV<V>, Point> pointMap) {
    List<LV<V>> misAligned = new ArrayList<>();
    for (Map.Entry<LV<V>, Point> entry : pointMap.entrySet()) {
      if (misAligned(entry.getKey(), pointMap)) {
        misAligned.add(entry.getKey());
      }
    }
    return misAligned;
  }

  protected List<LV<V>> misAligned(LV<V>[][] layers) {
    List<LV<V>> misAligned = new ArrayList<>();
    for (int i = 0; i < layers.length; i++) {
      for (int j = 0; j < layers[i].length; j++) {
        LV<V> v = layers[i][j];
        if (misAligned(v)) {
          misAligned.add(v);
        }
      }
    }
    return misAligned;
  }

  protected boolean misAligned(LV<V> v) {
    if (v instanceof SegmentVertex) {
      SegmentVertex<V> segmentVertex = (SegmentVertex<V>) v;
      PVertex<V> pVertex = segmentVertex.getSegment().pVertex;
      QVertex<V> qVertex = segmentVertex.getSegment().qVertex;
      // do pVertex and qVertex have different x values?
      Point p = pVertex.getPoint();
      Point q = qVertex.getPoint();
      if (p.x != q.x) {
        return true;
      }
    }
    return false;
  }

  protected boolean misAligned(LV<V> v, Map<LV<V>, Point> map) {
    if (v instanceof SegmentVertex) {
      SegmentVertex<V> segmentVertex = (SegmentVertex<V>) v;
      PVertex<V> pVertex = segmentVertex.getSegment().pVertex;
      QVertex<V> qVertex = segmentVertex.getSegment().qVertex;
      // do pVertex and qVertex have different x values?
      Point p = map.get(pVertex);
      Point q = map.get(qVertex);
      if (p.x != q.x && log.isTraceEnabled()) {
        log.trace(
            "segment {} misaligned with p at {} and q at {}", segmentVertex.getSegment(), p, q);
        return true;
      }
    }
    return false;
  }
}
