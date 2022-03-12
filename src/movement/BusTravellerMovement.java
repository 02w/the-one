/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import java.util.List;
import java.util.Random;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;

/**
 * This class controls the movement of bus travellers. A bus traveller belongs to a bus control
 * system. A bus traveller has a destination and a start location. If the direct path to the
 * destination is longer than the path the node would have to walk if it would take the bus, the
 * node uses the bus. If the destination is not provided, the node will pass a random number of
 * stops determined by Markov chains (defined in settings).
 *
 * @author Frans Ekman
 */
public class BusTravellerMovement extends MapBasedMovement
    implements SwitchableMovement, TransportMovement {

  public static final String PROBABILITIES_STRING = "probs";
  public static final String PROBABILITY_TAKE_OTHER_BUS = "probTakeOtherBus";

  public static final int STATE_WAITING_FOR_BUS = 0;
  public static final int STATE_DECIDED_TO_ENTER_A_BUS = 1;
  public static final int STATE_TRAVELLING_ON_BUS = 2;
  public static final int STATE_WALKING_ELSEWHERE = 3;
  private static int nextID = 0;
  private int state;
  private Path nextPath;
  private Coord location;
  private Coord latestBusStop;
  private final BusControlSystem controlSystem;
  private final int id;
  private final ContinueBusTripDecider cbtd;
  private double[] probabilities;
  private double probTakeOtherBus;
  private final DijkstraPathFinder pathFinder;
  private Coord startBusStop;
  private Coord endBusStop;
  private boolean takeBus;

  /**
   * Creates a BusTravellerModel
   *
   * @param settings
   */
  public BusTravellerMovement(Settings settings) {
    super(settings);
    int bcs = settings.getInt(BusControlSystem.BUS_CONTROL_SYSTEM_NR);
    this.controlSystem = BusControlSystem.getBusControlSystem(bcs);
    this.id = BusTravellerMovement.nextID++;
    this.controlSystem.registerTraveller(this);
    this.nextPath = new Path();
    this.state = BusTravellerMovement.STATE_WALKING_ELSEWHERE;
    if (settings.contains(BusTravellerMovement.PROBABILITIES_STRING)) {
      this.probabilities = settings.getCsvDoubles(BusTravellerMovement.PROBABILITIES_STRING);
    }
    if (settings.contains(BusTravellerMovement.PROBABILITY_TAKE_OTHER_BUS)) {
      this.probTakeOtherBus = settings.getDouble(BusTravellerMovement.PROBABILITY_TAKE_OTHER_BUS);
    }
    this.cbtd = new ContinueBusTripDecider(MovementModel.rng, this.probabilities);
    this.pathFinder = new DijkstraPathFinder(null);
    this.takeBus = true;
  }

  /**
   * Creates a BusTravellerModel from a prototype
   *
   * @param proto
   */
  public BusTravellerMovement(BusTravellerMovement proto) {
    super(proto);
    this.state = proto.state;
    this.controlSystem = proto.controlSystem;
    if (proto.location != null) {
      this.location = proto.location.clone();
    }
    this.nextPath = proto.nextPath;
    this.id = BusTravellerMovement.nextID++;
    this.controlSystem.registerTraveller(this);
    this.probabilities = proto.probabilities;
    this.cbtd = new ContinueBusTripDecider(MovementModel.rng, this.probabilities);
    this.pathFinder = proto.pathFinder;
    this.probTakeOtherBus = proto.probTakeOtherBus;
    this.takeBus = true;
  }

  /**
   * Help method to find the closest coordinate from a list of coordinates, to a specific location
   *
   * @param allCoords list of coordinates to compare
   * @param coord destination node
   * @return closest to the destination
   */
  private static Coord getClosestCoordinate(List<Coord> allCoords, Coord coord) {
    Coord closestCoord = null;
    double minDistance = Double.POSITIVE_INFINITY;
    for (Coord temp : allCoords) {
      double distance = temp.distance(coord);
      if (distance < minDistance) {
        minDistance = distance;
        closestCoord = temp;
      }
    }
    return closestCoord.clone();
  }

  public static void reset() {
    BusTravellerMovement.nextID = 0;
  }

  @Override
  public Coord getInitialLocation() {

    MapNode[] mapNodes = this.getMap().getNodes().toArray(new MapNode[0]);
    int index = MovementModel.rng.nextInt(mapNodes.length - 1);
    this.location = mapNodes[index].getLocation().clone();

    List<Coord> allStops = this.controlSystem.getBusStops();
    Coord closestToNode = BusTravellerMovement.getClosestCoordinate(allStops, this.location.clone());
    this.latestBusStop = closestToNode.clone();

    return this.location.clone();
  }

  @Override
  public Path getPath() {
    if (!this.takeBus) {
      return null;
    }
    if (this.state == BusTravellerMovement.STATE_WAITING_FOR_BUS) {
      return null;
    } else if (this.state == BusTravellerMovement.STATE_DECIDED_TO_ENTER_A_BUS) {
      this.state = BusTravellerMovement.STATE_TRAVELLING_ON_BUS;
      List<Coord> coords = this.nextPath.getCoords();
      this.location = (coords.get(coords.size() - 1)).clone();
      return this.nextPath;
    } else if (this.state == BusTravellerMovement.STATE_WALKING_ELSEWHERE) {
      // Try to find back to the bus stop
      SimMap map = this.controlSystem.getMap();
      if (map == null) {
        return null;
      }
      MapNode thisNode = map.getNodeByCoord(this.location);
      MapNode destinationNode = map.getNodeByCoord(this.latestBusStop);
      List<MapNode> nodes = this.pathFinder.getShortestPath(thisNode, destinationNode);
      Path path = new Path(this.generateSpeed());
      for (MapNode node : nodes) {
        path.addWaypoint(node.getLocation());
      }
      this.location = this.latestBusStop.clone();
      return path;
    }

    return null;
  }

  /**
   * Switches state between getPath() calls
   *
   * @return Always 0
   */
  @Override
  protected double generateWaitTime() {
    if (this.state == BusTravellerMovement.STATE_WALKING_ELSEWHERE) {
      if (this.location.equals(this.latestBusStop)) {
        this.state = BusTravellerMovement.STATE_WAITING_FOR_BUS;
      }
    }
    if (this.state == BusTravellerMovement.STATE_TRAVELLING_ON_BUS) {
      this.state = BusTravellerMovement.STATE_WAITING_FOR_BUS;
    }
    return 0;
  }

  @Override
  public MapBasedMovement replicate() {
    return new BusTravellerMovement(this);
  }

  public int getState() {
    return this.state;
  }

  /**
   * Get the location where the bus is located when it has moved its path
   *
   * @return The end point of the last path returned
   */
  public Coord getLocation() {
    if (this.location == null) {
      return null;
    }
    return this.location.clone();
  }

  /**
   * @see SwitchableMovement
   */
  @Override
  public void setLocation(Coord lastWaypoint) {
    this.location = lastWaypoint.clone();
  }

  /**
   * Notifies the node at the bus stop that a bus is there. Nodes inside busses are also notified.
   *
   * @param nextPath The next path the bus is going to take
   */
  public void enterBus(Path nextPath) {

    if (this.startBusStop != null && this.endBusStop != null) {
      if (this.location.equals(this.endBusStop)) {
        this.state = BusTravellerMovement.STATE_WALKING_ELSEWHERE;
        this.latestBusStop = this.location.clone();
      } else {
        this.state = BusTravellerMovement.STATE_DECIDED_TO_ENTER_A_BUS;
        this.nextPath = nextPath;
      }
      return;
    }

    if (!this.cbtd.continueTrip()) {
      this.state = BusTravellerMovement.STATE_WAITING_FOR_BUS;
      this.nextPath = null;
      /* It might decide not to start walking somewhere and wait
      for the next bus */
      if (MovementModel.rng.nextDouble() > this.probTakeOtherBus) {
        this.state = BusTravellerMovement.STATE_WALKING_ELSEWHERE;
        this.latestBusStop = this.location.clone();
      }
    } else {
      this.state = BusTravellerMovement.STATE_DECIDED_TO_ENTER_A_BUS;
      this.nextPath = nextPath;
    }
  }

  public int getID() {
    return this.id;
  }

  /**
   * Sets the next route for the traveller, so that it can decide wether it should take the bus or
   * not.
   *
   * @param nodeLocation
   * @param nodeDestination
   */
  @Override
  public void setNextRoute(Coord nodeLocation, Coord nodeDestination) {

    // Find closest stops to current location and destination
    List<Coord> allStops = this.controlSystem.getBusStops();

    Coord closestToNode = BusTravellerMovement.getClosestCoordinate(allStops, nodeLocation);
    Coord closestToDestination = BusTravellerMovement.getClosestCoordinate(allStops, nodeDestination);

    // Check if it is shorter to walk than take the bus
    double directDistance = nodeLocation.distance(nodeDestination);
    double busDistance =
        nodeLocation.distance(closestToNode) + nodeDestination.distance(closestToDestination);

    this.takeBus = !(directDistance < busDistance);

    this.startBusStop = closestToNode;
    this.endBusStop = closestToDestination;
    this.latestBusStop = this.startBusStop.clone();
  }

  /**
   * @see SwitchableMovement
   */
  @Override
  public Coord getLastLocation() {
    return this.location.clone();
  }

  /**
   * @see SwitchableMovement
   */
  @Override
  public boolean isReady() {
    return this.state == BusTravellerMovement.STATE_WALKING_ELSEWHERE;
  }

  /**
   * Small class to help nodes decide if they should continue the bus trip. Keeps the state of
   * nodes, i.e. how many stops they have passed so far. Markov chain probabilities for the
   * decisions.
   *
   * <p>NOT USED BY THE WORKING DAY MOVEMENT MODEL
   *
   * @author Frans Ekman
   */
  class ContinueBusTripDecider {

    private final double[] probabilities; // Probability to travel with bus
    private int state;
    private final Random rng;

    public ContinueBusTripDecider(Random rng, double[] probabilities) {
      this.rng = rng;
      this.probabilities = probabilities;
      this.state = 0;
    }

    /**
     * @return true if node should continue
     */
    public boolean continueTrip() {
      double rand = this.rng.nextDouble();
      if (rand < this.probabilities[this.state]) {
        this.incState();
        return true;
      } else {
        this.resetState();
        return false;
      }
    }

    /** Call when a stop has been passed */
    private void incState() {
      if (this.state < this.probabilities.length - 1) {
        this.state++;
      }
    }

    /** Call when node has finished it's trip */
    private void resetState() {
      this.state = 0;
    }
  }
}
