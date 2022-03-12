/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import java.util.Random;
import report.PingAppReporter;

/**
 * Simple ping application to demonstrate the application support. The application can be configured
 * to send pings with a fixed interval or to only answer to pings it receives. When the application
 * receives a ping it sends a pong message in response.
 *
 * <p>The corresponding <code>PingAppReporter</code> class can be used to record information about
 * the application behavior.
 *
 * @see PingAppReporter
 * @author teemuk
 */
public class PingApplication extends Application {
  /** Run in passive mode - don't generate pings but respond */
  public static final String PING_PASSIVE = "passive";
  /** Ping generation interval */
  public static final String PING_INTERVAL = "interval";
  /** Ping interval offset - avoids synchronization of ping sending */
  public static final String PING_OFFSET = "offset";
  /** Destination address range - inclusive lower, exclusive upper */
  public static final String PING_DEST_RANGE = "destinationRange";
  /** Seed for the app's random number generator */
  public static final String PING_SEED = "seed";
  /** Size of the ping message */
  public static final String PING_PING_SIZE = "pingSize";
  /** Size of the pong message */
  public static final String PING_PONG_SIZE = "pongSize";

  /** Application ID */
  public static final String APP_ID = "fi.tkk.netlab.PingApplication";

  // Private vars
  private double lastPing = 0;
  private double interval = 500;
  private boolean passive = false;
  private int seed = 0;
  private int destMin = 0;
  private int destMax = 1;
  private int pingSize = 1;
  private int pongSize = 1;
  private final Random rng;

  /**
   * Creates a new ping application with the given settings.
   *
   * @param s Settings to use for initializing the application.
   */
  public PingApplication(Settings s) {
    if (s.contains(PingApplication.PING_PASSIVE)) {
      this.passive = s.getBoolean(PingApplication.PING_PASSIVE);
    }
    if (s.contains(PingApplication.PING_INTERVAL)) {
      this.interval = s.getDouble(PingApplication.PING_INTERVAL);
    }
    if (s.contains(PingApplication.PING_OFFSET)) {
      this.lastPing = s.getDouble(PingApplication.PING_OFFSET);
    }
    if (s.contains(PingApplication.PING_SEED)) {
      this.seed = s.getInt(PingApplication.PING_SEED);
    }
    if (s.contains(PingApplication.PING_PING_SIZE)) {
      this.pingSize = s.getInt(PingApplication.PING_PING_SIZE);
    }
    if (s.contains(PingApplication.PING_PONG_SIZE)) {
      this.pongSize = s.getInt(PingApplication.PING_PONG_SIZE);
    }
    if (s.contains(PingApplication.PING_DEST_RANGE)) {
      int[] destination = s.getCsvInts(PingApplication.PING_DEST_RANGE, 2);
      this.destMin = destination[0];
      this.destMax = destination[1];
    }

    this.rng = new Random(this.seed);
    super.setAppID(PingApplication.APP_ID);
  }

  /**
   * Copy-constructor
   *
   * @param a
   */
  public PingApplication(PingApplication a) {
    super(a);
    this.lastPing = a.getLastPing();
    this.interval = a.getInterval();
    this.passive = a.isPassive();
    this.destMax = a.getDestMax();
    this.destMin = a.getDestMin();
    this.seed = a.getSeed();
    this.pongSize = a.getPongSize();
    this.pingSize = a.getPingSize();
    this.rng = new Random(this.seed);
  }

  /**
   * Handles an incoming message. If the message is a ping message replies with a pong message.
   * Generates events for ping and pong messages.
   *
   * @param msg message received by the router
   * @param host host to which the application instance is attached
   */
  @Override
  public Message handle(Message msg, DTNHost host) {
    String type = (String) msg.getProperty("type");
    if (type == null) {
      return msg; // Not a ping/pong message
    }

    // Respond with pong if we're the recipient
    if (msg.getTo() == host && type.equalsIgnoreCase("ping")) {
      String id = "pong" + SimClock.getIntTime() + "-" + host.getAddress();
      Message m = new Message(host, msg.getFrom(), id, this.getPongSize());
      m.addProperty("type", "pong");
      m.setAppID(PingApplication.APP_ID);
      host.createNewMessage(m);

      // Send event to listeners
      super.sendEventToListeners("GotPing", null, host);
      super.sendEventToListeners("SentPong", null, host);
    }

    // Received a pong reply
    if (msg.getTo() == host && type.equalsIgnoreCase("pong")) {
      // Send event to listeners
      super.sendEventToListeners("GotPong", null, host);
    }

    return msg;
  }

  /**
   * Draws a random host from the destination range
   *
   * @return host
   */
  private DTNHost randomHost() {
    int destaddr = 0;
    if (this.destMax == this.destMin) {
      destaddr = this.destMin;
    }
    destaddr = this.destMin + this.rng.nextInt(this.destMax - this.destMin);
    World w = SimScenario.getInstance().getWorld();
    return w.getNodeByAddress(destaddr);
  }

  @Override
  public Application replicate() {
    return new PingApplication(this);
  }

  /**
   * Sends a ping packet if this is an active application instance.
   *
   * @param host to which the application instance is attached
   */
  @Override
  public void update(DTNHost host) {
    if (this.passive) {
      return;
    }
    double curTime = SimClock.getTime();
    if (curTime - this.lastPing >= this.interval) {
      // Time to send a new ping
      Message m =
          new Message(
              host,
              this.randomHost(),
              "ping" + SimClock.getIntTime() + "-" + host.getAddress(),
              this.getPingSize());
      m.addProperty("type", "ping");
      m.setAppID(PingApplication.APP_ID);
      host.createNewMessage(m);

      // Call listeners
      super.sendEventToListeners("SentPing", null, host);

      this.lastPing = curTime;
    }
  }

  /**
   * @return the lastPing
   */
  public double getLastPing() {
    return this.lastPing;
  }

  /**
   * @param lastPing the lastPing to set
   */
  public void setLastPing(double lastPing) {
    this.lastPing = lastPing;
  }

  /**
   * @return the interval
   */
  public double getInterval() {
    return this.interval;
  }

  /**
   * @param interval the interval to set
   */
  public void setInterval(double interval) {
    this.interval = interval;
  }

  /**
   * @return the passive
   */
  public boolean isPassive() {
    return this.passive;
  }

  /**
   * @param passive the passive to set
   */
  public void setPassive(boolean passive) {
    this.passive = passive;
  }

  /**
   * @return the destMin
   */
  public int getDestMin() {
    return this.destMin;
  }

  /**
   * @param destMin the destMin to set
   */
  public void setDestMin(int destMin) {
    this.destMin = destMin;
  }

  /**
   * @return the destMax
   */
  public int getDestMax() {
    return this.destMax;
  }

  /**
   * @param destMax the destMax to set
   */
  public void setDestMax(int destMax) {
    this.destMax = destMax;
  }

  /**
   * @return the seed
   */
  public int getSeed() {
    return this.seed;
  }

  /**
   * @param seed the seed to set
   */
  public void setSeed(int seed) {
    this.seed = seed;
  }

  /**
   * @return the pongSize
   */
  public int getPongSize() {
    return this.pongSize;
  }

  /**
   * @param pongSize the pongSize to set
   */
  public void setPongSize(int pongSize) {
    this.pongSize = pongSize;
  }

  /**
   * @return the pingSize
   */
  public int getPingSize() {
    return this.pingSize;
  }

  /**
   * @param pingSize the pingSize to set
   */
  public void setPingSize(int pingSize) {
    this.pingSize = pingSize;
  }
}
