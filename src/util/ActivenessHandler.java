/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package util;

import core.Settings;
import core.SettingsError;
import core.SimClock;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Object of this class tell the models when a node belonging to a certain group is active and when
 * not.
 */
public class ActivenessHandler {

  /**
   * Active times -setting id ({@value})
   *
   * <p>Syntax: <CODE>start, end</CODE>
   *
   * <p>Defines the simulation time ranges when the node is active. Multiple timer ranges can be
   * concatenated by repeating the sequence. Time limits must be in order and must not overlap.
   *
   * <p>Example: 100,200,6000,6500<br>
   * Here node is active from 100 to 200 and from 6000 to 6500 simulated seconds from the start of
   * the simulation.
   */
  public static final String ACTIVE_TIMES_S = "activeTimes";

  /**
   * Active periods -setting id ({@value}).
   *
   * <p>Syntax: <CODE>activeTime, inactiveTime</CODE>
   *
   * <p>Defines the activity and inactivity periods.
   *
   * <p>Example: 2000,500<br>
   * Here node is periodically first active for 2000 seconds, followed by 500 seconds of
   * inactiveness, and 2000 seconds of activeness, 500 seconds of inactiveness, etc.
   */
  public static final String ACTIVE_PERIODS_S = "activePeriods";

  /**
   * Active periods offset -setting id ({@value}).
   *
   * <p>Defines how much the activity periods are offset from sim time 0. Value X means that the
   * first period has been on for X seconds at sim time 0. Default = 0 (i.e., first active period
   * starts at 0)
   */
  public static final String ACTIVE_PERIODS_OFFSET_S = "activePeriodsOffset";

  private final Queue<TimeRange> activeTimes;
  private int[] activePeriods;
  private int activePeriodsOffset;

  private TimeRange curRange = null;

  public ActivenessHandler(Settings s) {
    this.activeTimes = this.parseActiveTimes(s);

    if (this.activeTimes != null) {
      this.curRange = this.activeTimes.poll();
    } else if (s.contains(ActivenessHandler.ACTIVE_PERIODS_S)) {
      this.activePeriods = s.getCsvInts(ActivenessHandler.ACTIVE_PERIODS_S, 2);
      this.activePeriodsOffset = s.getInt(ActivenessHandler.ACTIVE_PERIODS_OFFSET_S, 0);
    } else {
      this.activePeriods = null;
    }
  }

  private Queue<TimeRange> parseActiveTimes(Settings s) {
    double[] times;
    String sName = s.getFullPropertyName(ActivenessHandler.ACTIVE_TIMES_S);

    if (s.contains(ActivenessHandler.ACTIVE_TIMES_S)) {
      times = s.getCsvDoubles(ActivenessHandler.ACTIVE_TIMES_S);
      if (times.length % 2 != 0) {
        throw new SettingsError(
            "Invalid amount of values ("
                + times.length
                + ") for setting "
                + sName
                + ". Must "
                + "be divisable by 2");
      }
    } else {
      return null; // no setting -> always active
    }

    Queue<TimeRange> timesList = new LinkedList<>();

    for (int i = 0; i < times.length; i += 2) {
      double start = times[i];
      double end = times[i + 1];

      if (start > end) {
        throw new SettingsError(
            "Start time ("
                + start
                + ") is "
                + " bigger than end time ("
                + end
                + ") in setting "
                + sName);
      }

      timesList.add(new TimeRange(start, end));
    }

    return timesList;
  }

  /**
   * Returns true if node should be active at the moment
   *
   * @return true if node should be active at the moment
   */
  public boolean isActive() {
    return this.isActive(0);
  }

  /**
   * Returns true if node should be active after/before offset amount of time from now
   *
   * @param offset The offset
   * @return true if node should be active, false if not
   */
  public boolean isActive(int offset) {
    if (this.activeTimes == null) {
      if (this.activePeriods == null) {
        return true; // no inactive times nor periods -> always active
      } else {
        /* using active periods mode */
        int timeIndex =
            (SimClock.getIntTime() + this.activePeriodsOffset + offset)
                % (this.activePeriods[0] + this.activePeriods[1]);
        return timeIndex <= this.activePeriods[0];
      }
    }

    if (this.curRange == null) {
      return false; // out of active times
    }

    double time = SimClock.getTime() + offset;

    if (this.curRange.isOut(time)) { // time for the next time range
      this.curRange = this.activeTimes.poll();
      if (this.curRange == null) {
        return false; // out of active times
      }
    }

    return this.curRange.isInRange(time);
  }

  /** Class for handling time ranges */
  private class TimeRange {
    private final double start;
    private final double end;

    /**
     * Constructor.
     *
     * @param start The start time
     * @param end The end time
     */
    public TimeRange(double start, double end) {
      this.start = start;
      this.end = end;
    }

    /**
     * Returns true if the given time is within start and end time (inclusive).
     *
     * @param time The time to check
     * @return true if the time is within limits
     */
    public boolean isInRange(double time) {
      return !(time < this.start) && !(time > this.end); // out of range
    }

    /**
     * Returns true if given time is bigger than end the end time
     *
     * @param time The time to check
     * @return true if given time is bigger than end
     */
    public boolean isOut(double time) {
      return time > this.end;
    }
  }
}
