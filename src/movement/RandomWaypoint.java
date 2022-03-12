/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/** Random waypoint movement model. Creates zig-zag paths within the simulation area. */
public class RandomWaypoint extends MovementModel {
  /** how many waypoints should there be per path */
  private static final int PATH_LENGTH = 1;

  private Coord lastWaypoint;

  public RandomWaypoint(Settings settings) {
    super(settings);
  }

  protected RandomWaypoint(RandomWaypoint rwp) {
    super(rwp);
  }

  /**
   * Returns a possible (random) placement for a host
   *
   * @return Random position on the map
   */
  @Override
  public Coord getInitialLocation() {
    assert MovementModel.rng != null : "MovementModel not initialized!";
    Coord c = this.randomCoord();

    this.lastWaypoint = c;
    return c;
  }

  @Override
  public Path getPath() {
    Path p;
    p = new Path(this.generateSpeed());
    p.addWaypoint(this.lastWaypoint.clone());
    Coord c = this.lastWaypoint;

    for (int i = 0; i < RandomWaypoint.PATH_LENGTH; i++) {
      c = this.randomCoord();
      p.addWaypoint(c);
    }

    this.lastWaypoint = c;
    return p;
  }

  @Override
  public RandomWaypoint replicate() {
    return new RandomWaypoint(this);
  }

  protected Coord randomCoord() {
    return new Coord(MovementModel.rng.nextDouble() * this.getMaxX(),
        MovementModel.rng.nextDouble() * this.getMaxY());
  }
}
