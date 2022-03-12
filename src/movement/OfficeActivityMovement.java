/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import core.SimClock;
import input.WKTReader;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
import util.ParetoRNG;

/**
 * This class models movement at an office. If the node happens to be at some other location than
 * the office, it first walks the shortest path to the office and then stays there until the end of
 * the work day. A node has only works at one office.
 *
 * @author Frans Ekman
 */
public class OfficeActivityMovement extends MapBasedMovement implements SwitchableMovement {

  public static final String WORK_DAY_LENGTH_SETTING = "workDayLength";
  public static final String NR_OF_OFFICES_SETTING = "nrOfOffices";
  public static final String OFFICE_SIZE_SETTING = "officeSize";
  public static final String OFFICE_WAIT_TIME_PARETO_COEFF_SETTING = "officeWaitTimeParetoCoeff";
  public static final String OFFICE_MIN_WAIT_TIME_SETTING = "officeMinWaitTime";
  public static final String OFFICE_MAX_WAIT_TIME_SETTING = "officeMaxWaitTime";
  public static final String OFFICE_LOCATIONS_FILE_SETTING = "officeLocationsFile";
  private static final int WALKING_TO_OFFICE_MODE = 0;
  private static final int AT_OFFICE_MODE = 1;
  private static int nrOfOffices = 50;

  private int mode;
  private final int workDayLength;
  private int startedWorkingTime;
  private boolean ready;
  private final DijkstraPathFinder pathFinder;

  private final ParetoRNG paretoRNG;

  private final int distance;
  private final double officeWaitTimeParetoCoeff;
  private final double officeMinWaitTime;
  private final double officeMaxWaitTime;

  private List<Coord> allOffices;

  private Coord lastWaypoint;
  private Coord officeLocation;
  private final Coord deskLocation;

  private boolean sittingAtDesk;

  /**
   * OfficeActivityMovement constructor
   *
   * @param settings
   */
  public OfficeActivityMovement(Settings settings) {
    super(settings);

    this.workDayLength = settings.getInt(OfficeActivityMovement.WORK_DAY_LENGTH_SETTING);
    OfficeActivityMovement.nrOfOffices = settings.getInt(
        OfficeActivityMovement.NR_OF_OFFICES_SETTING);

    this.distance = settings.getInt(OfficeActivityMovement.OFFICE_SIZE_SETTING);
    this.officeWaitTimeParetoCoeff = settings.getDouble(
        OfficeActivityMovement.OFFICE_WAIT_TIME_PARETO_COEFF_SETTING);
    this.officeMinWaitTime = settings.getDouble(OfficeActivityMovement.OFFICE_MIN_WAIT_TIME_SETTING);
    this.officeMaxWaitTime = settings.getDouble(OfficeActivityMovement.OFFICE_MAX_WAIT_TIME_SETTING);

    this.startedWorkingTime = -1;
    this.pathFinder = new DijkstraPathFinder(null);
    this.mode = OfficeActivityMovement.WALKING_TO_OFFICE_MODE;

    String officeLocationsFile = null;
    try {
      officeLocationsFile = settings.getSetting(
          OfficeActivityMovement.OFFICE_LOCATIONS_FILE_SETTING);
    } catch (Throwable t) {
      // Do nothing;
    }

    if (officeLocationsFile == null) {
      MapNode[] mapNodes = this.getMap().getNodes().toArray(new MapNode[0]);
      int officeIndex = MovementModel.rng.nextInt(mapNodes.length - 1) / (mapNodes.length / OfficeActivityMovement.nrOfOffices);
      this.officeLocation = mapNodes[officeIndex].getLocation().clone();
    } else {
      try {
        this.allOffices = new LinkedList<>();
        List<Coord> locationsRead = (new WKTReader()).readPoints(new File(officeLocationsFile));
        for (Coord coord : locationsRead) {
          SimMap map = this.getMap();
          Coord offset = map.getOffset();
          // mirror points if map data is mirrored
          if (map.isMirrored()) {
            coord.setLocation(coord.getX(), -coord.getY());
          }
          coord.translate(offset.getX(), offset.getY());
          this.allOffices.add(coord);
        }
        this.officeLocation = this.allOffices.get(MovementModel.rng.nextInt(this.allOffices.size())).clone();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    this.deskLocation = this.getRandomCoorinateInsideOffice();
    this.paretoRNG = new ParetoRNG(MovementModel.rng, this.officeWaitTimeParetoCoeff, this.officeMinWaitTime,
        this.officeMaxWaitTime);
  }

  /**
   * Copyconstructor
   *
   * @param proto
   */
  public OfficeActivityMovement(OfficeActivityMovement proto) {
    super(proto);
    this.workDayLength = proto.workDayLength;
    this.startedWorkingTime = -1;
    this.distance = proto.distance;
    this.pathFinder = proto.pathFinder;
    this.mode = proto.mode;

    if (proto.allOffices == null) {
      MapNode[] mapNodes = this.getMap().getNodes().toArray(new MapNode[0]);
      int officeIndex = MovementModel.rng.nextInt(mapNodes.length - 1) / (mapNodes.length / OfficeActivityMovement.nrOfOffices);
      this.officeLocation = mapNodes[officeIndex].getLocation().clone();
    } else {
      this.allOffices = proto.allOffices;
      this.officeLocation = this.allOffices.get(MovementModel.rng.nextInt(this.allOffices.size())).clone();
    }

    this.officeWaitTimeParetoCoeff = proto.officeWaitTimeParetoCoeff;
    this.officeMinWaitTime = proto.officeMinWaitTime;
    this.officeMaxWaitTime = proto.officeMaxWaitTime;

    this.deskLocation = this.getRandomCoorinateInsideOffice();
    this.paretoRNG = proto.paretoRNG;
  }

  public Coord getRandomCoorinateInsideOffice() {
    double x_coord = this.officeLocation.getX() + (0.5 - MovementModel.rng.nextDouble()) * this.distance;
    if (x_coord > this.getMaxX()) {
      x_coord = this.getMaxX();
    } else if (x_coord < 0) {
      x_coord = 0;
    }
    double y_coord = this.officeLocation.getY() + (0.5 - MovementModel.rng.nextDouble()) * this.distance;
    if (y_coord > this.getMaxY()) {
      y_coord = this.getMaxY();
    } else if (y_coord < 0) {
      y_coord = 0;
    }
    return new Coord(x_coord, y_coord);
  }

  @Override
  public Coord getInitialLocation() {
    double x = MovementModel.rng.nextDouble() * this.getMaxX();
    double y = MovementModel.rng.nextDouble() * this.getMaxY();
    Coord c = new Coord(x, y);

    this.lastWaypoint = c;
    return c.clone();
  }

  @Override
  public Path getPath() {
    if (this.mode == OfficeActivityMovement.WALKING_TO_OFFICE_MODE) {
      // Try to find to the office
      SimMap map = super.getMap();
      if (map == null) {
        return null;
      }
      MapNode thisNode = map.getNodeByCoord(this.lastWaypoint);
      MapNode destinationNode = map.getNodeByCoord(this.officeLocation);
      List<MapNode> nodes = this.pathFinder.getShortestPath(thisNode, destinationNode);
      Path path = new Path(this.generateSpeed());
      for (MapNode node : nodes) {
        path.addWaypoint(node.getLocation());
      }
      this.lastWaypoint = this.officeLocation.clone();
      this.mode = OfficeActivityMovement.AT_OFFICE_MODE;
      return path;
    }

    if (this.startedWorkingTime == -1) {
      this.startedWorkingTime = SimClock.getIntTime();
    }
    if (SimClock.getIntTime() - this.startedWorkingTime >= this.workDayLength) {
      Path path = new Path(1);
      path.addWaypoint(this.lastWaypoint.clone());
      this.ready = true;
      return path;
    }
    Coord c;
    if (this.sittingAtDesk) {
      c = this.getRandomCoorinateInsideOffice();
      this.sittingAtDesk = false;
    } else {
      c = this.deskLocation.clone();
      this.sittingAtDesk = true;
    }

    Path path = new Path(1);
    path.addWaypoint(c);
    return path;
  }

  @Override
  protected double generateWaitTime() {
    int timeLeft = this.workDayLength - (SimClock.getIntTime() - this.startedWorkingTime);

    int waitTime = (int) this.paretoRNG.getDouble();
    if (waitTime > timeLeft) {
      return timeLeft;
    }
    return waitTime;
  }

  @Override
  public MapBasedMovement replicate() {
    return new OfficeActivityMovement(this);
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
    this.startedWorkingTime = -1;
    this.ready = false;
    this.mode = OfficeActivityMovement.WALKING_TO_OFFICE_MODE;
  }

  /**
   * @return The location of the office
   */
  public Coord getOfficeLocation() {
    return this.officeLocation.clone();
  }
}
