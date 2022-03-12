/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import java.util.ArrayList;
import java.util.List;

/** A Path between multiple Coordinates. */
public class Path {
  /** coordinates of the path */
  private final List<Coord> coords;
  /** speeds in the path legs */
  private List<Double> speeds;

  private int nextWpIndex;

  /** Creates a path with zero speed. */
  public Path() {
    this.nextWpIndex = 0;
    this.coords = new ArrayList<>();
    this.speeds = new ArrayList<>(1);
  }

  /**
   * Copy constructor. Creates a copy of this path with a shallow copy of the coordinates and
   * speeds.
   *
   * @param path The path to create the copy from
   */
  public Path(Path path) {
    this.nextWpIndex = path.nextWpIndex;
    this.coords = new ArrayList<>(path.coords);
    this.speeds = new ArrayList<>(path.speeds);
  }

  /**
   * Creates a path with constant speed
   *
   * @param speed The speed on the path
   */
  public Path(double speed) {
    this();
    this.setSpeed(speed);
  }

  /**
   * Returns a reference to the coordinates of this path
   *
   * @return coordinates of the path
   */
  public List<Coord> getCoords() {
    return this.coords;
  }

  /**
   * Adds a new waypoint to the end of the path.
   *
   * @param wp The waypoint to add
   */
  public void addWaypoint(Coord wp) {
    assert this.speeds.size() <= 1
        : "This method should be used only for" + " paths with constant speed";
    this.coords.add(wp);
  }

  /**
   * Adds a new waypoint with a speed towards that waypoint
   *
   * @param wp The waypoint
   * @param speed The speed towards that waypoint
   */
  public void addWaypoint(Coord wp, double speed) {
    this.coords.add(wp);
    this.speeds.add(speed);
  }

  /**
   * Returns the next waypoint on this path
   *
   * @return the next waypoint
   */
  public Coord getNextWaypoint() {
    assert this.hasNext() : "Path didn't have " + (this.nextWpIndex + 1) + ". waypoint";
    return this.coords.get(this.nextWpIndex++);
  }

  /**
   * Returns true if the path has more waypoints, false if not
   *
   * @return true if the path has more waypoints, false if not
   */
  public boolean hasNext() {
    return this.nextWpIndex < this.coords.size();
  }

  /**
   * Returns the speed towards the next waypoint (asked with {@link #getNextWaypoint()}.
   *
   * @return the speed towards the next waypoint
   */
  public double getSpeed() {
    assert this.speeds.size() != 0 : "No speed set";
    assert this.nextWpIndex != 0 : "No waypoint asked";

    if (this.speeds.size() == 1) {
      return this.speeds.get(0);
    } else {
      return this.speeds.get(this.nextWpIndex - 1);
    }
  }

  /** Sets a constant speed for the whole path. Any previously set speed(s) is discarded. */
  public void setSpeed(double speed) {
    this.speeds = new ArrayList<>(1);
    this.speeds.add(speed);
  }

  /**
   * Returns a string presentation of the path's coordinates
   *
   * @return Path as a string
   */
  @Override
  public String toString() {
    String s = "";
    for (int i = 0, n = this.coords.size(); i < n; i++) {
      Coord c = this.coords.get(i);
      s += "->" + c;
      if (this.speeds.size() > 1) {
        s += String.format("@%.2f ", this.speeds.get(i));
      }
    }
    return s;
  }

  public List<Double> getSpeeds() {
    return this.speeds;
  }
}
