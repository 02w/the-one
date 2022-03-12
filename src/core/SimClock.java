/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

/** Wall clock for checking the simulation time. */
public class SimClock {
  private static double clockTime = 0.0;
  private static SimClock clock = null;

  static {
    DTNSim.registerForReset(SimClock.class.getCanonicalName());
    SimClock.reset();
  }

  private SimClock() {}

  /**
   * Get the instance of the class that can also change the time.
   *
   * @return The instance of this clock
   */
  public static SimClock getInstance() {
    if (SimClock.clock == null) {
      SimClock.clock = new SimClock();
    }
    return SimClock.clock;
  }

  /**
   * Returns the current time (seconds since start)
   *
   * @return Time as a double
   */
  public static double getTime() {
    return SimClock.clockTime;
  }

  /**
   * Sets the time of the clock.
   *
   * @param time the time to set
   */
  public void setTime(double time) {
    SimClock.clockTime = time;
  }

  /**
   * Returns the current time rounded to the nearest integer
   *
   * @return Time as integer
   */
  public static int getIntTime() {
    return (int) Math.round(SimClock.clockTime);
  }

  /**
   * Returns a string presentation of the sim time shown with the given amount of decimals
   *
   * @param decimals The number of decimals to show
   * @return The sim time
   */
  public static String getFormattedTime(int decimals) {
    return String.format("%." + decimals + "f", SimClock.clockTime);
  }

  /** Resets the static fields of the class */
  public static void reset() {
    SimClock.clockTime = 0;
  }

  /**
   * Advances the time by n seconds
   *
   * @param time Nrof seconds to increase the time
   */
  public void advance(double time) {
    SimClock.clockTime += time;
  }

  /**
   * Returns the current simulation time in a string
   *
   * @return the current simulation time in a string
   */
  @Override
  public String toString() {
    return "SimTime: " + SimClock.clockTime;
  }
}
