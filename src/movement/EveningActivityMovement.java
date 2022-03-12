/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import input.WKTReader;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;

/**
 * A Class to model movement when people are out shopping or doing other activities with friends. If
 * the node happens to be at some other location than the place where the shopping starts (where it
 * meets its friends), it first travels to the destination along the shortest path.
 *
 * @author Frans Ekman
 */
public class EveningActivityMovement extends MapBasedMovement implements SwitchableMovement {

  public static final String NR_OF_MEETING_SPOTS_SETTING = "nrOfMeetingSpots";
  public static final String EVENING_ACTIVITY_CONTROL_SYSTEM_NR_SETTING = "shoppingControlSystemNr";
  public static final String MEETING_SPOTS_FILE_SETTING = "meetingSpotsFile";
  public static final String MIN_GROUP_SIZE_SETTING = "minGroupSize";
  public static final String MAX_GROUP_SIZE_SETTING = "maxGroupSize";
  public static final String MIN_WAIT_TIME_SETTING = "minAfterShoppingStopTime";
  public static final String MAX_WAIT_TIME_SETTING = "maxAfterShoppingStopTime";
  private static final int WALKING_TO_MEETING_SPOT_MODE = 0;
  private static final int EVENING_ACTIVITY_MODE = 1;
  private static int nrOfMeetingSpots = 10;
  private static int nextID = 0;
  private int mode;
  private boolean ready;
  private final DijkstraPathFinder pathFinder;
  private Coord lastWaypoint;
  private Coord startAtLocation;
  private final EveningActivityControlSystem scs;
  private EveningTrip trip;
  private boolean readyToShop;
  private final int id;
  private int minGroupSize;
  private int maxGroupSize;

  /**
   * Creates a new instance of EveningActivityMovement
   *
   * @param settings
   */
  public EveningActivityMovement(Settings settings) {
    super(settings);
    super.backAllowed = false;
    this.pathFinder = new DijkstraPathFinder(null);
    this.mode = EveningActivityMovement.WALKING_TO_MEETING_SPOT_MODE;

    EveningActivityMovement.nrOfMeetingSpots = settings.getInt(
        EveningActivityMovement.NR_OF_MEETING_SPOTS_SETTING);

    this.minGroupSize = settings.getInt(EveningActivityMovement.MIN_GROUP_SIZE_SETTING);
    this.maxGroupSize = settings.getInt(EveningActivityMovement.MAX_GROUP_SIZE_SETTING);

    MapNode[] mapNodes = this.getMap().getNodes().toArray(new MapNode[0]);

    String shoppingSpotsFile = null;
    try {
      shoppingSpotsFile = settings.getSetting(EveningActivityMovement.MEETING_SPOTS_FILE_SETTING);
    } catch (Throwable t) {
      // Do nothing;
    }

    List<Coord> meetingSpotLocations = null;

    if (shoppingSpotsFile == null) {
      meetingSpotLocations = new LinkedList<>();
      for (int i = 0; i < mapNodes.length; i++) {
        if ((i % (mapNodes.length / EveningActivityMovement.nrOfMeetingSpots)) == 0) {
          this.startAtLocation = mapNodes[i].getLocation().clone();
          meetingSpotLocations.add(this.startAtLocation.clone());
        }
      }
    } else {
      try {
        meetingSpotLocations = new LinkedList<>();
        List<Coord> locationsRead = (new WKTReader()).readPoints(new File(shoppingSpotsFile));
        for (Coord coord : locationsRead) {
          SimMap map = this.getMap();
          Coord offset = map.getOffset();
          // mirror points if map data is mirrored
          if (map.isMirrored()) {
            coord.setLocation(coord.getX(), -coord.getY());
          }
          coord.translate(offset.getX(), offset.getY());
          meetingSpotLocations.add(coord);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    this.id = EveningActivityMovement.nextID++;

    int scsID = settings.getInt(EveningActivityMovement.EVENING_ACTIVITY_CONTROL_SYSTEM_NR_SETTING);

    this.scs = EveningActivityControlSystem.getEveningActivityControlSystem(scsID);
    this.scs.setRandomNumberGenerator(MovementModel.rng);
    this.scs.addEveningActivityNode(this);
    this.scs.setMeetingSpots(meetingSpotLocations);

    this.maxPathLength = 100;
    this.minPathLength = 10;

    this.maxWaitTime = settings.getInt(EveningActivityMovement.MAX_WAIT_TIME_SETTING);
    this.minWaitTime = settings.getInt(EveningActivityMovement.MIN_WAIT_TIME_SETTING);
  }

  /**
   * Creates a new instance of EveningActivityMovement from a prototype
   *
   * @param proto
   */
  public EveningActivityMovement(EveningActivityMovement proto) {
    super(proto);
    this.pathFinder = proto.pathFinder;
    this.mode = proto.mode;
    this.id = EveningActivityMovement.nextID++;
    this.scs = proto.scs;
    this.scs.addEveningActivityNode(this);
    this.setMinGroupSize(proto.getMinGroupSize());
    this.setMaxGroupSize(proto.getMaxGroupSize());
  }

  public static void reset() {
    EveningActivityMovement.nextID = 0;
  }

  /**
   * @return Unique ID of the shopper
   */
  public int getID() {
    return this.id;
  }

  @Override
  public Coord getInitialLocation() {

    MapNode[] mapNodes = this.getMap().getNodes().toArray(new MapNode[0]);
    int index = MovementModel.rng.nextInt(mapNodes.length - 1);
    this.lastWaypoint = mapNodes[index].getLocation().clone();
    return this.lastWaypoint.clone();
  }

  @Override
  public Path getPath() {
    if (this.mode == EveningActivityMovement.WALKING_TO_MEETING_SPOT_MODE) {
      // Try to find to the shopping center
      SimMap map = super.getMap();
      if (map == null) {
        return null;
      }
      MapNode thisNode = map.getNodeByCoord(this.lastWaypoint);
      MapNode destinationNode = map.getNodeByCoord(this.startAtLocation);

      List<MapNode> nodes = this.pathFinder.getShortestPath(thisNode, destinationNode);
      Path path = new Path(this.generateSpeed());
      for (MapNode node : nodes) {
        path.addWaypoint(node.getLocation());
      }
      this.lastWaypoint = this.startAtLocation.clone();
      this.mode = EveningActivityMovement.EVENING_ACTIVITY_MODE;
      return path;
    } else if (this.mode == EveningActivityMovement.EVENING_ACTIVITY_MODE) {
      this.readyToShop = true;
      if (this.trip.allMembersPresent()) {
        Path path = this.trip.getPath();
        if (path == null) {
          super.lastMapNode = super.getMap().getNodeByCoord(this.lastWaypoint);
          path = super.getPath(); // TODO Create levy walk path
          this.lastWaypoint = super.lastMapNode.getLocation();
          this.trip.setPath(path);
          double waitTimeAtEnd = (this.maxWaitTime
              - this.minWaitTime) * MovementModel.rng.nextDouble() + this.minWaitTime;
          this.trip.setWaitTimeAtEnd(waitTimeAtEnd);
          this.trip.setDestination(this.lastWaypoint);
        }
        this.lastWaypoint = this.trip.getDestination();
        this.ready = true;
        return path;
      }
    }

    return null;
  }

  @Override
  protected double generateWaitTime() {
    if (this.ready) {
      double wait = this.trip.getWaitTimeAtEnd();
      return wait;
    } else {
      return 0;
    }
  }

  @Override
  public MapBasedMovement replicate() {
    return new EveningActivityMovement(this);
  }

  /**
   * @see SwitchableMovement
   */
  @Override
  public Coord getLastLocation() {
    return this.lastWaypoint.clone();
  }

  /**
   * @see SwitchableMovement
   */
  @Override
  public boolean isReady() {
    return this.ready;
  }

  /**
   * @see SwitchableMovement
   */
  @Override
  public void setLocation(Coord lastWaypoint) {
    this.lastWaypoint = lastWaypoint.clone();
    this.ready = false;
    this.mode = EveningActivityMovement.WALKING_TO_MEETING_SPOT_MODE;
  }

  /**
   * Sets the node ready to start a shopping trip.
   *
   * @return The coordinate of the place where the shopping trip starts
   */
  public Coord getShoppingLocationAndGetReady() {
    this.readyToShop = false; // New shopping round starts
    this.trip = this.scs.getEveningInstructions(this.id);
    this.startAtLocation = this.trip.getLocation().clone();
    return this.startAtLocation.clone();
  }

  public Coord getShoppingLocation() {
    return this.scs.getMeetingSpotForID(this.id).clone();
  }

  /**
   * Checks if a node is at the correct place where the shopping begins
   *
   * @return true if node is ready and waiting for the rest of the group to arrive
   */
  public boolean isReadyToShop() {
    return this.readyToShop;
  }

  public int getMinGroupSize() {
    return this.minGroupSize;
  }

  public void setMinGroupSize(int minGroupSize) {
    this.minGroupSize = minGroupSize;
  }

  public int getMaxGroupSize() {
    return this.maxGroupSize;
  }

  public void setMaxGroupSize(int maxGroupSize) {
    this.maxGroupSize = maxGroupSize;
  }
}
