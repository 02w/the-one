/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** World contains all the nodes and is responsible for updating their location and connections. */
public class World {
  /** name space of optimization settings ({@value}) */
  public static final String OPTIMIZATION_SETTINGS_NS = "Optimization";

  /**
   * Should the order of node updates be different (random) within every update step -setting id
   * ({@value}). Boolean (true/false) variable. Default is @link {@link #DEF_RANDOMIZE_UPDATES}.
   */
  public static final String RANDOMIZE_UPDATES_S = "randomizeUpdateOrder";
  /** should the update order of nodes be randomized -setting's default value ({@value}) */
  public static final boolean DEF_RANDOMIZE_UPDATES = true;

  /**
   * Real-time simulation enabled -setting id ({@value}). If set to true and simulation time moves
   * faster than real time, the simulation will pause after each update round to wait until real
   * time catches up. Default = false.
   */
  public static final String REALTIME_SIM_S = "realtime";
  /** should the update order of nodes be randomized -setting's default value ({@value}) */

  /**
   * Should the connectivity simulation be stopped after one round -setting id ({@value}). Boolean
   * (true/false) variable. Default = false.
   */
  public static final String SIMULATE_CON_ONCE_S = "simulateConnectionsOnce";

  private final int sizeX;
  private final int sizeY;
  private final List<EventQueue> eventQueues;
  private final double updateInterval;
  private final SimClock simClock;
  private double nextQueueEventTime;
  private EventQueue nextEventQueue;
  /** list of nodes; nodes are indexed by their network address */
  private final List<DTNHost> hosts;

  private boolean simulateConnections;
  /**
   * nodes in the order they should be updated (if the order should be randomized; null value means
   * that the order should not be randomized)
   */
  private ArrayList<DTNHost> updateOrder;
  /** is cancellation of simulation requested from UI */
  private boolean isCancelled;

  private final List<UpdateListener> updateListeners;
  /** Queue of scheduled update requests */
  private final ScheduledUpdatesQueue scheduledUpdates;

  private boolean simulateConOnce;

  private boolean realtimeSimulation;
  private long simStartRealtime;

  /** Constructor. */
  public World(
      List<DTNHost> hosts,
      int sizeX,
      int sizeY,
      double updateInterval,
      List<UpdateListener> updateListeners,
      boolean simulateConnections,
      List<EventQueue> eventQueues) {
    this.hosts = hosts;
    this.sizeX = sizeX;
    this.sizeY = sizeY;
    this.updateInterval = updateInterval;
    this.updateListeners = updateListeners;
    this.simulateConnections = simulateConnections;
    this.eventQueues = eventQueues;

    this.simClock = SimClock.getInstance();
    this.scheduledUpdates = new ScheduledUpdatesQueue();
    this.isCancelled = false;

    this.simStartRealtime = -1;

    this.setNextEventQueue();
    this.initSettings();
  }

  /** Initializes settings fields that can be configured using Settings class */
  private void initSettings() {
    Settings s = new Settings(World.OPTIMIZATION_SETTINGS_NS);
    boolean randomizeUpdates = s.getBoolean(World.RANDOMIZE_UPDATES_S, World.DEF_RANDOMIZE_UPDATES);

    this.simulateConOnce = s.getBoolean(World.SIMULATE_CON_ONCE_S, false);

    this.realtimeSimulation = s.getBoolean(World.REALTIME_SIM_S, false);

    if (randomizeUpdates) {
      // creates the update order array that can be shuffled
      this.updateOrder = new ArrayList<>(this.hosts);
    } else { // null pointer means "don't randomize"
      this.updateOrder = null;
    }
  }

  /**
   * Moves hosts in the world for the time given time initialize host positions properly. SimClock
   * must be set to <CODE>-time</CODE> before calling this method.
   *
   * @param time The total time (seconds) to move
   */
  public void warmupMovementModel(double time) {
    if (time <= 0) {
      return;
    }

    while (SimClock.getTime() < -this.updateInterval) {
      this.moveHosts(this.updateInterval);
      this.simClock.advance(this.updateInterval);
    }

    double finalStep = -SimClock.getTime();

    this.moveHosts(finalStep);
    this.simClock.setTime(0);
  }

  /** Goes through all event Queues and sets the event queue that has the next event. */
  public void setNextEventQueue() {
    EventQueue nextQueue = this.scheduledUpdates;
    double earliest = nextQueue.nextEventsTime();

    /* find the queue that has the next event */
    for (EventQueue eq : this.eventQueues) {
      if (eq.nextEventsTime() < earliest) {
        nextQueue = eq;
        earliest = eq.nextEventsTime();
      }
    }

    this.nextEventQueue = nextQueue;
    this.nextQueueEventTime = earliest;
  }

  /**
   * Update (move, connect, disconnect etc.) all hosts in the world. Runs all external events that
   * are due between the time when this method is called and after one update interval.
   */
  public void update() {
    double runUntil = SimClock.getTime() + this.updateInterval;

    if (this.realtimeSimulation) {
      if (this.simStartRealtime < 0) {
        /* first update round */
        this.simStartRealtime = System.currentTimeMillis();
      }

      long sleepTime =
          (long) (SimClock.getTime() * 1000 - (System.currentTimeMillis() - this.simStartRealtime));
      if (sleepTime > 0) {
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
          throw new SimError("Sleep interrupted:" + e);
        }
      }
    }

    this.setNextEventQueue();

    /* process all events that are due until next interval update */
    while (this.nextQueueEventTime <= runUntil) {
      this.simClock.setTime(this.nextQueueEventTime);
      ExternalEvent ee = this.nextEventQueue.nextEvent();
      ee.processEvent(this);
      this.updateHosts(); // update all hosts after every event
      this.setNextEventQueue();
    }

    this.moveHosts(this.updateInterval);
    this.simClock.setTime(runUntil);

    this.updateHosts();

    /* inform all update listeners */
    for (UpdateListener ul : this.updateListeners) {
      ul.updated(this.hosts);
    }
  }

  /**
   * Updates all hosts (calls update for every one of them). If update order randomizing is on
   * (updateOrder array is defined), the calls are made in random order.
   */
  private void updateHosts() {
    if (this.updateOrder == null) { // randomizing is off
      for (int i = 0, n = this.hosts.size(); i < n; i++) {
        if (this.isCancelled) {
          break;
        }
        this.hosts.get(i).update(this.simulateConnections);
      }
    } else { // update order randomizing is on
      assert this.updateOrder.size() == this.hosts.size() : "Nrof hosts has changed unexpectedly";
      Random rng = new Random(SimClock.getIntTime());
      Collections.shuffle(this.updateOrder, rng);
      for (int i = 0, n = this.hosts.size(); i < n; i++) {
        if (this.isCancelled) {
          break;
        }
        this.updateOrder.get(i).update(this.simulateConnections);
      }
    }

    if (this.simulateConOnce && this.simulateConnections) {
      this.simulateConnections = false;
    }
  }

  /**
   * Moves all hosts in the world for a given amount of time
   *
   * @param timeIncrement The time how long all nodes should move
   */
  private void moveHosts(double timeIncrement) {
    for (int i = 0, n = this.hosts.size(); i < n; i++) {
      DTNHost host = this.hosts.get(i);
      host.move(timeIncrement);
    }
  }

  /** Asynchronously cancels the currently running simulation */
  public void cancelSim() {
    this.isCancelled = true;
  }

  /**
   * Returns the hosts in a list
   *
   * @return the hosts in a list
   */
  public List<DTNHost> getHosts() {
    return this.hosts;
  }

  /**
   * Returns the x-size (width) of the world
   *
   * @return the x-size (width) of the world
   */
  public int getSizeX() {
    return this.sizeX;
  }

  /**
   * Returns the y-size (height) of the world
   *
   * @return the y-size (height) of the world
   */
  public int getSizeY() {
    return this.sizeY;
  }

  /**
   * Returns a node from the world by its address
   *
   * @param address The address of the node
   * @return The requested node or null if it wasn't found
   */
  public DTNHost getNodeByAddress(int address) {
    if (address < 0 || address >= this.hosts.size()) {
      throw new SimError(
          "No host for address "
              + address
              + ". Address "
              + "range of 0-"
              + (this.hosts.size() - 1)
              + " is valid");
    }

    DTNHost node = this.hosts.get(address);
    assert node.getAddress() == address
        : "Node indexing failed. " + "Node " + node + " in index " + address;

    return node;
  }

  /**
   * Schedules an update request to all nodes to happen at the specified simulation time.
   *
   * @param simTime The time of the update
   */
  public void scheduleUpdate(double simTime) {
    this.scheduledUpdates.addUpdate(simTime);
  }
}
