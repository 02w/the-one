/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import java.util.List;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;

/**
 * The CarMovement class representing the car movement submodel
 *
 * @author Frans Ekman
 */
public class CarMovement extends MapBasedMovement implements SwitchableMovement, TransportMovement {

  private Coord from;
  private Coord to;

  private final DijkstraPathFinder pathFinder;

  /**
   * Car movement constructor
   *
   * @param settings
   */
  public CarMovement(Settings settings) {
    super(settings);
    this.pathFinder = new DijkstraPathFinder(this.getOkMapNodeTypes());
  }

  /**
   * Construct a new CarMovement instance from a prototype
   *
   * @param proto
   */
  public CarMovement(CarMovement proto) {
    super(proto);
    this.pathFinder = proto.pathFinder;
  }

  /**
   * Sets the next route to be taken
   *
   * @param nodeLocation
   * @param nodeDestination
   */
  @Override
  public void setNextRoute(Coord nodeLocation, Coord nodeDestination) {
    this.from = nodeLocation.clone();
    this.to = nodeDestination.clone();
  }

  @Override
  public Path getPath() {
    Path path = new Path(this.generateSpeed());

    MapNode fromNode = this.getMap().getNodeByCoord(this.from);
    MapNode toNode = this.getMap().getNodeByCoord(this.to);

    List<MapNode> nodePath = this.pathFinder.getShortestPath(fromNode, toNode);

    for (MapNode node : nodePath) { // create a Path from the shortest path
      path.addWaypoint(node.getLocation());
    }

    this.lastMapNode = toNode;

    return path;
  }

  /**
   * @see SwitchableMovement
   * @return true
   */
  @Override
  public boolean isReady() {
    return true;
  }
}
