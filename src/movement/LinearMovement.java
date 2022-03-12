/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/** Movement model where all nodes move on a line (work in progress) */
public class LinearMovement extends MovementModel {
  /** Name space of the settings (append to group name space) */
  public static final String LINEAR_MOVEMENT_NS = "LinearMovement.";
  /** Per node group setting for defining the start coordinates of the line ({@value}) */
  public static final String START_LOCATION_S = "startLocation";
  /** Per node group setting for defining the end coordinates of the line ({@value}) */
  public static final String END_LOCATION_S = "endLocation";

  /**
   * Nodes' initial location type
   *
   * <ul>
   *   <li>0: random (evenly distributed)
   *   <li>1: evenly spaced
   * </ul>
   */
  public static final String INIT_LOC_S = "initLocType";
  /**
   * Nodes' target (where they're heading) type
   *
   * <ul>
   *   <li>0: random point on the line
   *   <li>1: far-end of the line
   * </ul>
   */
  public static final String TARGET_S = "targetType";

  /* values for the prototype */
  private Coord startLoc;
  /** The start location of the line */
  private Coord endLoc;
  /** The start location of the line */
  private int initLocType;

  private int targetType;
  private int nodeCount;
  /** how many nodes in this formation */
  private int lastIndex;
  /** index of the previous node */

  /* values for the per-node models */
  private Path nextPath;

  private Coord initLoc;

  /**
   * Creates a new movement model based on a Settings object's settings.
   *
   * @param s The Settings object where the settings are read from
   */
  public LinearMovement(Settings s) {
    super(s);
    int[] coords;

    coords = s.getCsvInts(LinearMovement.LINEAR_MOVEMENT_NS + LinearMovement.START_LOCATION_S, 2);
    this.startLoc = new Coord(coords[0], coords[1]);
    coords = s.getCsvInts(LinearMovement.LINEAR_MOVEMENT_NS + LinearMovement.END_LOCATION_S, 2);
    this.endLoc = new Coord(coords[0], coords[1]);
    this.initLocType = s.getInt(LinearMovement.LINEAR_MOVEMENT_NS + LinearMovement.INIT_LOC_S);
    this.targetType = s.getInt(LinearMovement.LINEAR_MOVEMENT_NS + LinearMovement.TARGET_S);
    this.nodeCount = s.getInt(core.SimScenario.NROF_HOSTS_S);

    this.lastIndex = 0;
  }

  /**
   * Copy constructor.
   *
   * @param ilm The LinearFormation prototype
   */
  public LinearMovement(LinearMovement ilm) {
    super(ilm);
    this.initLoc = this.calculateLocation(ilm, (ilm.initLocType == 1));
    this.nextPath = new Path(this.generateSpeed());
    this.nextPath.addWaypoint(this.initLoc);

    if (ilm.targetType == 0) {
        /* random target */
      this.nextPath.addWaypoint(this.calculateLocation(ilm, true));
    } else {
      this.nextPath.addWaypoint(this.calculateEndTarget(ilm, this.initLoc));
    }

    ilm.lastIndex++;
  }

  /**
   * Calculates and returns a location in the line
   *
   * @param proto The movement model prototype
   * @param isEven Is the distribution evenly spaced (false for random)
   * @return a location on the line
   */
  private Coord calculateLocation(LinearMovement proto, boolean isEven) {
    double dx = 0;
    double dy = 0;
    double placementFraction;

    double xDiff = (proto.endLoc.getX() - proto.startLoc.getX());
    double yDiff = (proto.endLoc.getY() - proto.startLoc.getY());
    Coord c = proto.startLoc.clone();

    if (isEven) {
      placementFraction = (1.0 * proto.lastIndex / proto.nodeCount);
      dx = placementFraction * xDiff;
      dy = placementFraction * yDiff;
    } else {
        /* random */
      dx = MovementModel.rng.nextDouble() * xDiff;
      dy = MovementModel.rng.nextDouble() * yDiff;
    }

    c.translate(dx, dy);
    return c;
  }

  /**
   * Calculates and returns the far-end of the line
   *
   * @param proto The movement model prototype
   * @param initLoc The initial location
   * @return the coordinates for the far-end of the line
   */
  private Coord calculateEndTarget(LinearMovement proto, Coord initLoc) {
    return (proto.startLoc.distance(initLoc) > proto.endLoc.distance(initLoc)
        ? proto.startLoc
        : proto.endLoc);
  }

  /**
   * Returns the the location of the node in the formation
   *
   * @return the the location of the node in the formation
   */
  @Override
  public Coord getInitialLocation() {
    return this.initLoc;
  }

  /**
   * Returns a single coordinate path (using the only possible coordinate)
   *
   * @return a single coordinate path
   */
  @Override
  public Path getPath() {
    Path p = this.nextPath;
    this.nextPath = null;
    return p;
  }

  /** Returns Double.MAX_VALUE (no paths available) */
  @Override
  public double nextPathAvailable() {
    if (this.nextPath == null) {
      return Double.MAX_VALUE; // no new paths available
    } else {
      return 0;
    }
  }

  @Override
  public int getMaxX() {
    return (int) (this.endLoc.getX() > this.startLoc.getX() ? this.endLoc.getX() : this.startLoc.getX());
  }

  @Override
  public int getMaxY() {
    return (int) (this.endLoc.getY() > this.startLoc.getY() ? this.endLoc.getY() : this.startLoc.getY());
  }

  @Override
  public LinearMovement replicate() {
    return new LinearMovement(this);
  }
}
