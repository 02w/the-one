/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * Random Walk movement model
 *
 * @author Frans Ekman
 */
public class RandomWalk extends MovementModel implements SwitchableMovement {

  private Coord lastWaypoint;
  private final double minDistance;
  private final double maxDistance;

  public RandomWalk(Settings settings) {
    super(settings);
    this.minDistance = 0;
    this.maxDistance = 50;
  }

  private RandomWalk(RandomWalk rwp) {
    super(rwp);
    this.minDistance = rwp.minDistance;
    this.maxDistance = rwp.maxDistance;
  }

  /**
   * Returns a possible (random) placement for a host
   *
   * @return Random position on the map
   */
  @Override
  public Coord getInitialLocation() {
    assert MovementModel.rng != null : "MovementModel not initialized!";
    double x = MovementModel.rng.nextDouble() * this.getMaxX();
    double y = MovementModel.rng.nextDouble() * this.getMaxY();
    Coord c = new Coord(x, y);

    this.lastWaypoint = c;
    return c;
  }

  @Override
  public Path getPath() {
    Path p;
    p = new Path(this.generateSpeed());
    p.addWaypoint(this.lastWaypoint.clone());
    double maxX = this.getMaxX();
    double maxY = this.getMaxY();

    Coord c = null;
    while (true) {

      double angle = MovementModel.rng.nextDouble() * 2 * Math.PI;
      double distance = this.minDistance + MovementModel.rng.nextDouble() * (this.maxDistance - this.minDistance);

      double x = this.lastWaypoint.getX() + distance * Math.cos(angle);
      double y = this.lastWaypoint.getY() + distance * Math.sin(angle);

      c = new Coord(x, y);

      if (x > 0 && y > 0 && x < maxX && y < maxY) {
        break;
      }
    }

    p.addWaypoint(c);

    this.lastWaypoint = c;
    return p;
  }

  @Override
  public RandomWalk replicate() {
    return new RandomWalk(this);
  }

  @Override
  public Coord getLastLocation() {
    return this.lastWaypoint;
  }

  @Override
  public void setLocation(Coord lastWaypoint) {
    this.lastWaypoint = lastWaypoint;
  }

  @Override
  public boolean isReady() {
    return true;
  }
}
