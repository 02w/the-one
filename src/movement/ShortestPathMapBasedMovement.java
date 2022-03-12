/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Settings;
import java.util.List;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.PointsOfInterest;

/**
 * Map based movement model that uses Dijkstra's algorithm to find shortest paths between two random
 * map nodes and Points Of Interest
 */
public class ShortestPathMapBasedMovement extends MapBasedMovement implements SwitchableMovement {
  /** the Dijkstra shortest path finder */
  private final DijkstraPathFinder pathFinder;

  /** Points Of Interest handler */
  private final PointsOfInterest pois;

  /**
   * Creates a new movement model based on a Settings object's settings.
   *
   * @param settings The Settings object where the settings are read from
   */
  public ShortestPathMapBasedMovement(Settings settings) {
    super(settings);
    this.pathFinder = new DijkstraPathFinder(this.getOkMapNodeTypes());
    this.pois = new PointsOfInterest(this.getMap(), this.getOkMapNodeTypes(), settings,
        MovementModel.rng);
  }

  /**
   * Copyconstructor.
   *
   * @param mbm The ShortestPathMapBasedMovement prototype to base the new object to
   */
  protected ShortestPathMapBasedMovement(ShortestPathMapBasedMovement mbm) {
    super(mbm);
    this.pathFinder = mbm.pathFinder;
    this.pois = mbm.pois;
  }

  @Override
  public Path getPath() {
    Path p = new Path(this.generateSpeed());
    MapNode to = this.pois.selectDestination();

    List<MapNode> nodePath = this.pathFinder.getShortestPath(this.lastMapNode, to);

    // this assertion should never fire if the map is checked in read phase
    assert nodePath.size() > 0
        : "No path from "
            + this.lastMapNode
            + " to "
            + to
            + ". The simulation map isn't fully connected";

    for (MapNode node : nodePath) { // create a Path from the shortest path
      p.addWaypoint(node.getLocation());
    }

    this.lastMapNode = to;

    return p;
  }

  @Override
  public ShortestPathMapBasedMovement replicate() {
    return new ShortestPathMapBasedMovement(this);
  }
}
