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

/**
 * A Class to model movement at home. If the node happens to be at some other location than its
 * home, it first walks the shortest path home location and then stays there until morning. A node
 * has only one home
 *
 * @author Frans Ekman
 */
public class HomeActivityMovement extends MapBasedMovement implements SwitchableMovement {

  public static final String HOME_LOCATIONS_FILE_SETTING = "homeLocationsFile";
  public static final String STD_FOR_TIME_DIFF_SETTING = "timeDiffSTD";
  private static final int WALKING_HOME_MODE = 0;
  private static final int AT_HOME_MODE = 1;
  private static final int READY_MODE = 2;
  private static final int DAY_LENGTH = 86000;
  private int mode;
  private final DijkstraPathFinder pathFinder;

  private final int distance;

  private Coord lastWaypoint;
  private Coord homeLocation;

  private List<Coord> allHomes;

  private final int timeDiffSTD;
  private final int timeDifference;

  /**
   * Creates a new instance of HomeActivityMovement
   *
   * @param settings
   */
  public HomeActivityMovement(Settings settings) {
    super(settings);
    this.distance = 100;
    this.pathFinder = new DijkstraPathFinder(null);
    this.mode = HomeActivityMovement.WALKING_HOME_MODE;

    String homeLocationsFile = null;
    try {
      homeLocationsFile = settings.getSetting(HomeActivityMovement.HOME_LOCATIONS_FILE_SETTING);
    } catch (Throwable t) {
      // Do nothing;
    }

    this.timeDiffSTD = settings.getInt(HomeActivityMovement.STD_FOR_TIME_DIFF_SETTING);

    if (homeLocationsFile == null) {
      MapNode[] mapNodes = this.getMap().getNodes().toArray(new MapNode[0]);
      int homeIndex = MovementModel.rng.nextInt(mapNodes.length - 1);
      this.homeLocation = mapNodes[homeIndex].getLocation().clone();
    } else {
      try {
        this.allHomes = new LinkedList<>();
        List<Coord> locationsRead = (new WKTReader()).readPoints(new File(homeLocationsFile));
        for (Coord coord : locationsRead) {
          SimMap map = this.getMap();
          Coord offset = map.getOffset();
          // mirror points if map data is mirrored
          if (map.isMirrored()) {
            coord.setLocation(coord.getX(), -coord.getY());
          }
          coord.translate(offset.getX(), offset.getY());
          this.allHomes.add(coord);
        }
        this.homeLocation = this.allHomes.get(MovementModel.rng.nextInt(this.allHomes.size())).clone();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (this.timeDiffSTD == -1) {
      this.timeDifference = MovementModel.rng.nextInt(HomeActivityMovement.DAY_LENGTH) -
          HomeActivityMovement.DAY_LENGTH / 2;
    } else if (this.timeDiffSTD == 0) {
      this.timeDifference = 0;
    } else {
      this.timeDifference =
          (int)
              Math.min(
                  Math.max((
                      MovementModel.rng.nextGaussian() * this.timeDiffSTD), -HomeActivityMovement.DAY_LENGTH
                      / 2),
                  HomeActivityMovement.DAY_LENGTH / 2);
    }
  }

  /**
   * Creates a new instance of HomeActivityMovement from a prototype
   *
   * @param proto
   */
  public HomeActivityMovement(HomeActivityMovement proto) {
    super(proto);
    this.distance = proto.distance;
    this.pathFinder = proto.pathFinder;
    this.mode = proto.mode;

    this.timeDiffSTD = proto.timeDiffSTD;

    if (proto.allHomes == null) {
      MapNode[] mapNodes = this.getMap().getNodes().toArray(new MapNode[0]);
      int homeIndex = MovementModel.rng.nextInt(mapNodes.length - 1);
      this.homeLocation = mapNodes[homeIndex].getLocation().clone();
    } else {
      this.allHomes = proto.allHomes;
      this.homeLocation = this.allHomes.get(MovementModel.rng.nextInt(this.allHomes.size())).clone();
    }

    if (this.timeDiffSTD == -1) {
      this.timeDifference = MovementModel.rng.nextInt(HomeActivityMovement.DAY_LENGTH) -
          HomeActivityMovement.DAY_LENGTH / 2;
    } else if (this.timeDiffSTD == 0) {
      this.timeDifference = 0;
    } else {
      this.timeDifference =
          (int)
              Math.min(
                  Math.max((
                      MovementModel.rng.nextGaussian() * this.timeDiffSTD), -HomeActivityMovement.DAY_LENGTH
                      / 2),
                  HomeActivityMovement.DAY_LENGTH / 2);
    }
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
    if (this.mode == HomeActivityMovement.WALKING_HOME_MODE) {
      // Try to find home
      SimMap map = super.getMap();
      if (map == null) {
        return null;
      }
      MapNode thisNode = map.getNodeByCoord(this.lastWaypoint);
      MapNode destinationNode = map.getNodeByCoord(this.homeLocation);
      List<MapNode> nodes = this.pathFinder.getShortestPath(thisNode, destinationNode);
      Path path = new Path(this.generateSpeed());
      for (MapNode node : nodes) {
        path.addWaypoint(node.getLocation());
      }
      this.lastWaypoint = this.homeLocation.clone();
      this.mode = HomeActivityMovement.AT_HOME_MODE;

      double newX = this.lastWaypoint.getX() + (MovementModel.rng.nextDouble() - 0.5) * this.distance;
      if (newX > this.getMaxX()) {
        newX = this.getMaxX();
      } else if (newX < 0) {
        newX = 0;
      }
      double newY = this.lastWaypoint.getY() + (MovementModel.rng.nextDouble() - 0.5) * this.distance;
      if (newY > this.getMaxY()) {
        newY = this.getMaxY();
      } else if (newY < 0) {
        newY = 0;
      }
      Coord c = new Coord(newX, newY);
      path.addWaypoint(c);
      return path;
    } else {
      Path path = new Path(1);
      path.addWaypoint(this.lastWaypoint.clone());
      this.mode = HomeActivityMovement.READY_MODE;
      return path;
    }
  }

  @Override
  protected double generateWaitTime() {
    if (this.mode == HomeActivityMovement.AT_HOME_MODE) {
      return HomeActivityMovement.DAY_LENGTH
          - ((SimClock.getIntTime() + HomeActivityMovement.DAY_LENGTH
          + this.timeDifference) % HomeActivityMovement.DAY_LENGTH);
    } else {
      return 0;
    }
  }

  @Override
  public MapBasedMovement replicate() {
    return new HomeActivityMovement(this);
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
    return this.mode == HomeActivityMovement.READY_MODE;
  }

  /**
   * @see SwitchableMovement
   */
  @Override
  public void setLocation(Coord lastWaypoint) {
    this.lastWaypoint = lastWaypoint.clone();
    this.mode = HomeActivityMovement.WALKING_HOME_MODE;
  }

  /**
   * @return Home location of the node
   */
  public Coord getHomeLocation() {
    return this.homeLocation.clone();
  }
}
