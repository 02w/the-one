/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement.map;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/** Implementation of the Dijkstra's shortest path algorithm. */
public class DijkstraPathFinder {
  /** Value for infinite distance */
  private static final Double INFINITY = Double.MAX_VALUE;
  /** Initial size of the priority queue */
  private static final int PQ_INIT_SIZE = 11;

  /** Map of node distances from the source node */
  private DistanceMap distances;
  /** Set of already visited nodes (where the shortest path is known) */
  private Set<MapNode> visited;
  /** Priority queue of unvisited nodes discovered so far */
  private Queue<MapNode> unvisited;
  /** Map of previous nodes on the shortest path(s) */
  private Map<MapNode, MapNode> prevNodes;

  private final int[] okMapNodes;

  /**
   * Constructor.
   *
   * @param okMapNodes The map node types that are OK for paths or null if all nodes are OK
   */
  public DijkstraPathFinder(int[] okMapNodes) {
    super();
    this.okMapNodes = okMapNodes;
  }

  /**
   * Initializes a new search with a source node
   *
   * @param node The path's source node
   */
  private void initWith(MapNode node) {
    assert (this.okMapNodes == null || node.isType(this.okMapNodes));

    // create needed data structures
    this.unvisited = new PriorityQueue<>(DijkstraPathFinder.PQ_INIT_SIZE, new DistanceComparator());
    this.visited = new HashSet<>();
    this.prevNodes = new HashMap<>();
    this.distances = new DistanceMap();

    // set distance to source 0 and initialize unvisited queue
    this.distances.put(node, 0);
    this.unvisited.add(node);
  }

  /**
   * Finds and returns a shortest path between two map nodes
   *
   * @param from The source of the path
   * @param to The destination of the path
   * @return a shortest path between the source and destination nodes in a list of MapNodes or an
   *     empty list if such path is not available
   */
  public List<MapNode> getShortestPath(MapNode from, MapNode to) {
    List<MapNode> path = new LinkedList<>();

    if (from.compareTo(to) == 0) { // source and destination are the same
      path.add(from); // return a list containing only source node
      return path;
    }

    this.initWith(from);
    MapNode node = null;

    // always take the node with shortest distance
    while ((node = this.unvisited.poll()) != null) {
      if (node == to) {
        break; // we found the destination -> no need to search further
      }

      this.visited.add(node); // mark the node as visited
      this.relax(node); // add/update neighbor nodes' distances
    }

    // now we either have the path or such path wasn't available
    if (node == to) { // found a path
      path.add(0, to);
      MapNode prev = this.prevNodes.get(to);
      while (prev != from) {
        path.add(0, prev); // always put previous node to beginning
        prev = this.prevNodes.get(prev);
      }

      path.add(0, from); // finally put the source node to first node
    }

    return path;
  }

  /**
   * Relaxes the neighbors of a node (updates the shortest distances).
   *
   * @param node The node whose neighbors are relaxed
   */
  private void relax(MapNode node) {
    double nodeDist = this.distances.get(node);
    for (MapNode n : node.getNeighbors()) {
      if (this.visited.contains(n)) {
        continue; // skip visited nodes
      }

      if (this.okMapNodes != null && !n.isType(this.okMapNodes)) {
        continue; // skip nodes that are not OK
      }

      // n node's distance from path's source node
      double nDist = nodeDist + this.getDistance(node, n);

      if (this.distances.get(n) > nDist) { // stored distance > found dist?
        this.prevNodes.put(n, node);
        this.setDistance(n, nDist);
      }
    }
  }

  /**
   * Sets the distance from source node to a node
   *
   * @param n The node whose distance is set
   * @param distance The distance of the node from the source node
   */
  private void setDistance(MapNode n, double distance) {
    this.unvisited.remove(n); // remove node from old place in the queue
    this.distances.put(n, distance); // update distance
    this.unvisited.add(n); // insert node to the new place in the queue
  }

  /**
   * Returns the (euclidean) distance between the two map nodes
   *
   * @param from The first node
   * @param to The second node
   * @return Euclidean distance between the two map nodes
   */
  private double getDistance(MapNode from, MapNode to) {
    return from.getLocation().distance(to.getLocation());
  }

  /** Comparator that compares two map nodes by their distance from the source node. */
  private class DistanceComparator implements Comparator<MapNode> {

    /**
     * Compares two map nodes by their distance from the source node
     *
     * @return -1, 0 or 1 if node1's distance is smaller, equal to, or bigger than node2's distance
     */
    @Override
    public int compare(MapNode node1, MapNode node2) {
      double dist1 = DijkstraPathFinder.this.distances.get(node1);
      double dist2 = DijkstraPathFinder.this.distances.get(node2);

      if (dist1 > dist2) {
        return 1;
      } else if (dist1 < dist2) {
        return -1;
      } else {
        return node1.compareTo(node2);
      }
    }
  }

  /** Simple Map implementation for storing distances. */
  private class DistanceMap {
    private final HashMap<MapNode, Double> map;

    /** Constructor. Creates an empty distance map */
    public DistanceMap() {
      this.map = new HashMap<>();
    }

    /**
     * Returns the distance to a node. If no distance value is found, returns {@link
     * DijkstraPathFinder#INFINITY} as the value.
     *
     * @param node The node whose distance is requested
     * @return The distance to that node
     */
    public double get(MapNode node) {
      Double value = this.map.get(node);
      if (value != null) {
        return value;
      } else {
        return DijkstraPathFinder.INFINITY;
      }
    }

    /**
     * Puts a new distance value for a map node
     *
     * @param node The node
     * @param distance Distance to that node
     */
    public void put(MapNode node, double distance) {
      this.map.put(node, distance);
    }

    /**
     * Returns a string representation of the map's contents
     *
     * @return a string representation of the map's contents
     */
    @Override
    public String toString() {
      return this.map.toString();
    }
  }
}
