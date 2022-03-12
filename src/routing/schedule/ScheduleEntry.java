/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing.schedule;

import java.io.Serializable;

public class ScheduleEntry implements Serializable {
  private static final long serialVersionUID = 42L;

  private final double time;
  private final int from;
  private final int to;
  private final int via;
  private double delta;
  private final double duration;
  private int usageCount;

  /**
   * Constructor of new schedule entry
   *
   * @param time When the journey from "from" starts
   * @param from The source
   * @param via The node that takes us there (or -1 if n/a)
   * @param to The destination
   * @param duration Time it takes from the source to destination
   */
  public ScheduleEntry(double time, int from, int via, int to, double duration) {
    this.time = time;
    this.from = from;
    this.via = via;
    this.to = to;
    this.duration = duration;
    this.delta = 0;
    this.usageCount = 0;
  }

  /**
   * Returns time + delta
   *
   * @return the time
   */
  public double getTime() {
    return this.time + this.delta;
  }

  /**
   * @return the destination
   */
  public int getTo() {
    return this.to;
  }

  /**
   * @return the source
   */
  public int getFrom() {
    return this.from;
  }

  /**
   * @return the via
   */
  public int getVia() {
    return this.via;
  }

  /**
   * Return the time it takes to get from source to destination
   *
   * @return the duration
   */
  public double getDuration() {
    return this.duration;
  }

  public double getDestinationTime() {
    return this.getTime() + this.getDuration();
  }

  /**
   * @return the delta
   */
  public double getDelta() {
    return this.delta;
  }

  /**
   * @param delta the delta to set
   */
  public void setDelta(double delta) {
    this.delta = delta;
  }

  /**
   * @return the usageCount
   */
  public int getUsageCount() {
    return this.usageCount;
  }

  public void increaseUsageCount() {
    this.usageCount++;
  }

  @Override
  public String toString() {
    return this.time
        + "(+"
        + this.delta
        + "): "
        + this.from
        + "->"
        + (this.via > 0 ? this.via + "->" : "")
        + this.to
        + " ("
        + this.duration
        + ")";
  }
}
