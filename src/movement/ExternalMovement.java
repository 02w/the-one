/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.DTNSim;
import core.Settings;
import core.SimClock;
import input.ExternalMovementReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import util.Tuple;

/** Movement model that uses external data of node locations. */
public class ExternalMovement extends MovementModel {
  /** Namespace for settings */
  public static final String EXTERNAL_MOVEMENT_NS = "ExternalMovement";
  /** external locations file's path -setting id ({@value}) */
  public static final String MOVEMENT_FILE_S = "file";
  /** number of preloaded intervals per preload run -setting id ({@value}) */
  public static final String NROF_PRELOAD_S = "nrofPreload";

  /** default initial location for excess nodes */
  private static final Coord DEF_INIT_LOC = new Coord(0, 0);
  /** minimum number intervals that should be preloaded ahead of sim time */
  private static final double MIN_AHEAD_INTERVALS = 2;
  private static ExternalMovementReader reader;
  private static String inputFileName;
  /** mapping of external id to movement model */
  private static Map<String, ExternalMovement> idMapping;
  /** initial locations for nodes */
  private static List<Tuple<String, Coord>> initLocations;
  /** time of the very first location data */
  private static double initTime;
  /** sampling interval (seconds) of the location data */
  private static double samplingInterval;
  /** last read time stamp after preloading */
  private static double lastPreloadTime;
  /** how many time intervals to load on every preload run */
  private static double nrofPreload = 10;

  static {
    DTNSim.registerForReset(ExternalMovement.class.getCanonicalName());
    ExternalMovement.reset();
  }

  /** the very first location of the node */
  private Coord intialLocation;
  /** queue of path-start-time, path tuples */
  private Queue<Tuple<Double, Path>> pathQueue;
  /** when was the path currently under construction started */
  private double latestPathStartTime;
  /** the last location of path waypoint */
  private Coord latestLocation;
  /** the path currently under construction */
  private Path latestPath;
  /** is this node active */
  private boolean isActive;

  /**
   * Constructor for the prototype. Run once per group.
   *
   * @param settings Where settings are read from
   */
  public ExternalMovement(Settings settings) {
    super(settings);

    if (ExternalMovement.idMapping == null) {
      // run these the first time object is created or after reset call
      Settings s = new Settings(ExternalMovement.EXTERNAL_MOVEMENT_NS);
      ExternalMovement.idMapping = new HashMap<>();
      ExternalMovement.inputFileName = s.getSetting(ExternalMovement.MOVEMENT_FILE_S);
      ExternalMovement.reader = new ExternalMovementReader(ExternalMovement.inputFileName);

      ExternalMovement.initLocations = ExternalMovement.reader.readNextMovements();
      ExternalMovement.initTime = ExternalMovement.reader.getLastTimeStamp();
      ExternalMovement.samplingInterval = -1;
      ExternalMovement.lastPreloadTime = -1;

      s.setNameSpace(ExternalMovement.EXTERNAL_MOVEMENT_NS);
      if (s.contains(ExternalMovement.NROF_PRELOAD_S)) {
        ExternalMovement.nrofPreload = s.getInt(ExternalMovement.NROF_PRELOAD_S);
        if (ExternalMovement.nrofPreload <= 0) {
          ExternalMovement.nrofPreload = 1;
        }
      }
    }
  }

  /**
   * Copy constructor. Gives out location data for the new node from location queue.
   *
   * @param mm The movement model to copy from
   */
  private ExternalMovement(MovementModel mm) {
    super(mm);

    this.pathQueue = new LinkedList<>();
    this.latestPath = null;

    if (ExternalMovement.initLocations.size() > 0) { // we have location data left
      // gets a new location from the list
      Tuple<String, Coord> initLoc = ExternalMovement.initLocations.remove(0);
      this.intialLocation = this.latestLocation = initLoc.getValue();
      this.latestPathStartTime = ExternalMovement.initTime;

      // puts the new model to model map for later updates
      ExternalMovement.idMapping.put(initLoc.getKey(), this);
      this.isActive = true;
    } else {
      // no more location data left for the new node -> set inactive
      this.intialLocation = ExternalMovement.DEF_INIT_LOC;
      this.isActive = false;
    }
  }

  /** Checks if more paths should be preloaded and preloads them if needed. */
  private static void checkPathNeed() {
    if (ExternalMovement.samplingInterval == -1) { // first preload
      ExternalMovement.lastPreloadTime = ExternalMovement.readMorePaths();
    }

    if (!Double.isNaN(ExternalMovement.lastPreloadTime)
        && SimClock.getTime() >= ExternalMovement.lastPreloadTime - (
        ExternalMovement.samplingInterval * ExternalMovement.MIN_AHEAD_INTERVALS)) {
      for (int i = 0; i < ExternalMovement.nrofPreload && !Double.isNaN(
          ExternalMovement.lastPreloadTime); i++) {
        ExternalMovement.lastPreloadTime = ExternalMovement.readMorePaths();
      }
    }
  }

  /**
   * Reads paths for the next time instance from the reader
   *
   * @return The time stamp of the reading or Double.NaN if no movements were read.
   */
  private static double readMorePaths() {
    List<Tuple<String, Coord>> list = ExternalMovement.reader.readNextMovements();
    double time = ExternalMovement.reader.getLastTimeStamp();

    if (ExternalMovement.samplingInterval == -1) {
      ExternalMovement.samplingInterval = time - ExternalMovement.initTime;
    }

    for (Tuple<String, Coord> t : list) {
      ExternalMovement em = ExternalMovement.idMapping.get(t.getKey());
      if (em != null) { // skip unknown IDs, i.e. IDs not mentioned in...
        // ...init phase or if there are more IDs than nodes
        em.addLocation(t.getValue(), time);
      }
    }

    if (list.size() > 0) {
      return time;
    } else {
      return Double.NaN;
    }
  }

  /** Reset state so that next instance will have a fresh state */
  public static void reset() {
    ExternalMovement.idMapping = null;
  }

  @Override
  public Coord getInitialLocation() {
    return this.intialLocation;
  }

  @Override
  public boolean isActive() {
    return this.isActive;
  }

  /**
   * Adds a new location with a time to this model's move pattern. If the node stayed stationary
   * during the update, the current path is put to the queue and a new path is started once the node
   * starts moving.
   *
   * @param loc The location
   * @param time When should the node be there
   */
  private void addLocation(Coord loc, double time) {
    assert ExternalMovement.samplingInterval > 0 : "Non-positive sampling interval!";

    if (loc.equals(this.latestLocation)) { // node didn't move
      if (this.latestPath != null) {
        // constructing path -> end constructing and put it in the queue
        this.pathQueue.add(new Tuple<>(this.latestPathStartTime, this.latestPath));
        this.latestPath = null;
      }

      this.latestPathStartTime = time;
      return;
    }

    if (this.latestPath == null) {
      this.latestPath = new Path();
    }

    double speed = loc.distance(this.latestLocation) / ExternalMovement.samplingInterval;
    this.latestPath.addWaypoint(loc, speed);

    this.latestLocation = loc;
  }

  /**
   * Returns a sim time when the next path is available.
   *
   * @return The sim time when node should ask the next time for a path
   */
  @Override
  public double nextPathAvailable() {
    if (this.pathQueue.size() == 0) {
      return this.latestPathStartTime;
    } else {
      return this.pathQueue.element().getKey();
    }
  }

  @Override
  public Path getPath() {
    Path p;

    ExternalMovement.checkPathNeed(); // check if we should preload more paths

    if (SimClock.getTime() < this.nextPathAvailable()) {
      return null;
    }

    if (this.pathQueue.size() == 0) { // nothing in the queue, return latest
      p = this.latestPath;
      this.latestPath = null;
    } else { // return first path in the queue
      p = this.pathQueue.remove().getValue();
    }

    return p;
  }

  @Override
  public int getMaxX() {
    return (int) (ExternalMovement.reader.getMaxX() - ExternalMovement.reader.getMinX()) + 1;
  }

  @Override
  public int getMaxY() {
    return (int) (ExternalMovement.reader.getMaxY() - ExternalMovement.reader.getMinY()) + 1;
  }

  @Override
  public MovementModel replicate() {
    return new ExternalMovement(this);
  }
}
