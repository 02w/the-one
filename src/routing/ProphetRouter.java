/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import routing.util.RoutingInfo;
import util.Tuple;

/**
 * Implementation of PRoPHET router as described in <I>Probabilistic routing in intermittently
 * connected networks</I> by Anders Lindgren et al.
 */
public class ProphetRouter extends ActiveRouter {
  /** delivery predictability initialization constant */
  public static final double P_INIT = 0.75;
  /** delivery predictability transitivity scaling constant default value */
  public static final double DEFAULT_BETA = 0.25;
  /** delivery predictability aging constant */
  public static final double DEFAULT_GAMMA = 0.98;

  /** Prophet router's setting namespace ({@value}) */
  public static final String PROPHET_NS = "ProphetRouter";
  /**
   * Number of seconds in time unit -setting id ({@value}). How many seconds one time unit is when
   * calculating aging of delivery predictions. Should be tweaked for the scenario.
   */
  public static final String SECONDS_IN_UNIT_S = "secondsInTimeUnit";

  /**
   * Transitivity scaling constant (beta) -setting id ({@value}). Default value for setting is
   * {@link #DEFAULT_BETA}.
   */
  public static final String BETA_S = "beta";

  /**
   * Predictability aging constant (gamma) -setting id ({@value}). Default value for setting is
   * {@link #DEFAULT_GAMMA}.
   */
  public static final String GAMMA_S = "gamma";

  /** the value of nrof seconds in time unit -setting */
  private final int secondsInTimeUnit;
  /** value of beta setting */
  private final double beta;
  /** value of gamma setting */
  private final double gamma;

  /** delivery predictabilities */
  private Map<DTNHost, Double> preds;
  /** last delivery predictability update (sim)time */
  private double lastAgeUpdate;

  /**
   * Constructor. Creates a new message router based on the settings in the given Settings object.
   *
   * @param s The settings object
   */
  public ProphetRouter(Settings s) {
    super(s);
    Settings prophetSettings = new Settings(ProphetRouter.PROPHET_NS);
    this.secondsInTimeUnit = prophetSettings.getInt(ProphetRouter.SECONDS_IN_UNIT_S);
    if (prophetSettings.contains(ProphetRouter.BETA_S)) {
      this.beta = prophetSettings.getDouble(ProphetRouter.BETA_S);
    } else {
      this.beta = ProphetRouter.DEFAULT_BETA;
    }

    if (prophetSettings.contains(ProphetRouter.GAMMA_S)) {
      this.gamma = prophetSettings.getDouble(ProphetRouter.GAMMA_S);
    } else {
      this.gamma = ProphetRouter.DEFAULT_GAMMA;
    }

    this.initPreds();
  }

  /**
   * Copyconstructor.
   *
   * @param r The router prototype where setting values are copied from
   */
  protected ProphetRouter(ProphetRouter r) {
    super(r);
    this.secondsInTimeUnit = r.secondsInTimeUnit;
    this.beta = r.beta;
    this.gamma = r.gamma;
    this.initPreds();
  }

  /** Initializes predictability hash */
  private void initPreds() {
    this.preds = new HashMap<>();
  }

  @Override
  public void changedConnection(Connection con) {
    super.changedConnection(con);

    if (con.isUp()) {
      DTNHost otherHost = con.getOtherNode(this.getHost());
      this.updateDeliveryPredFor(otherHost);
      this.updateTransitivePreds(otherHost);
    }
  }

  /**
   * Updates delivery predictions for a host. <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT
   * </CODE>
   *
   * @param host The host we just met
   */
  private void updateDeliveryPredFor(DTNHost host) {
    double oldValue = this.getPredFor(host);
    double newValue = oldValue + (1 - oldValue) * ProphetRouter.P_INIT;
    this.preds.put(host, newValue);
  }

  /**
   * Returns the current prediction (P) value for a host or 0 if entry for the host doesn't exist.
   *
   * @param host The host to look the P for
   * @return the current P value
   */
  public double getPredFor(DTNHost host) {
    this.ageDeliveryPreds(); // make sure preds are updated before getting
    if (this.preds.containsKey(host)) {
      return this.preds.get(host);
    } else {
      return 0;
    }
  }

  /**
   * Updates transitive (A->B->C) delivery predictions. <CODE>
   * P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
   * </CODE>
   *
   * @param host The B host who we just met
   */
  private void updateTransitivePreds(DTNHost host) {
    MessageRouter otherRouter = host.getRouter();
    assert otherRouter instanceof ProphetRouter
        : "PRoPHET only works " + " with other routers of same type";

    double pForHost = this.getPredFor(host); // P(a,b)
    Map<DTNHost, Double> othersPreds = ((ProphetRouter) otherRouter).getDeliveryPreds();

    for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
      if (e.getKey() == this.getHost()) {
        continue; // don't add yourself
      }

      double pOld = this.getPredFor(e.getKey()); // P(a,c)_old
      double pNew = pOld + (1 - pOld) * pForHost * e.getValue() * this.beta;
      this.preds.put(e.getKey(), pNew);
    }
  }

  /**
   * Ages all entries in the delivery predictions. <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>,
   * where k is number of time units that have elapsed since the last time the metric was aged.
   *
   * @see #SECONDS_IN_UNIT_S
   */
  private void ageDeliveryPreds() {
    double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / this.secondsInTimeUnit;

    if (timeDiff == 0) {
      return;
    }

    double mult = Math.pow(this.gamma, timeDiff);
    for (Map.Entry<DTNHost, Double> e : this.preds.entrySet()) {
      e.setValue(e.getValue() * mult);
    }

    this.lastAgeUpdate = SimClock.getTime();
  }

  /**
   * Returns a map of this router's delivery predictions
   *
   * @return a map of this router's delivery predictions
   */
  private Map<DTNHost, Double> getDeliveryPreds() {
    this.ageDeliveryPreds(); // make sure the aging is done
    return this.preds;
  }

  @Override
  public void update() {
    super.update();
    if (!this.canStartTransfer() || this.isTransferring()) {
      return; // nothing to transfer or is currently transferring
    }

    // try messages that could be delivered to final recipient
    if (this.exchangeDeliverableMessages() != null) {
      return;
    }

    this.tryOtherMessages();
  }

  /**
   * Tries to send all other messages to all connected hosts ordered by their delivery probability
   *
   * @return The return value of {@link #tryMessagesForConnected(List)}
   */
  private Tuple<Message, Connection> tryOtherMessages() {
    List<Tuple<Message, Connection>> messages = new ArrayList<>();

    Collection<Message> msgCollection = this.getMessageCollection();

    /* for all connected hosts collect all messages that have a higher
    probability of delivery by the other host */
    for (Connection con : this.getConnections()) {
      DTNHost other = con.getOtherNode(this.getHost());
      ProphetRouter othRouter = (ProphetRouter) other.getRouter();

      if (othRouter.isTransferring()) {
        continue; // skip hosts that are transferring
      }

      for (Message m : msgCollection) {
        if (othRouter.hasMessage(m.getId())) {
          continue; // skip messages that the other one has
        }
        if (othRouter.getPredFor(m.getTo()) > this.getPredFor(m.getTo())) {
          // the other node has higher probability of delivery
          messages.add(new Tuple<>(m, con));
        }
      }
    }

    if (messages.size() == 0) {
      return null;
    }

    // sort the message-connection tuples
    Collections.sort(messages, new TupleComparator());
    return this.tryMessagesForConnected(messages); // try to send messages
  }

  @Override
  public RoutingInfo getRoutingInfo() {
    this.ageDeliveryPreds();
    RoutingInfo top = super.getRoutingInfo();
    RoutingInfo ri = new RoutingInfo(this.preds.size() + " delivery prediction(s)");

    for (Map.Entry<DTNHost, Double> e : this.preds.entrySet()) {
      DTNHost host = e.getKey();
      Double value = e.getValue();

      ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", host, value)));
    }

    top.addMoreInfo(ri);
    return top;
  }

  @Override
  public MessageRouter replicate() {
    ProphetRouter r = new ProphetRouter(this);
    return r;
  }

  /**
   * Comparator for Message-Connection-Tuples that orders the tuples by their delivery probability
   * by the host on the other side of the connection (GRTRMax)
   */
  private class TupleComparator implements Comparator<Tuple<Message, Connection>> {

    @Override
    public int compare(Tuple<Message, Connection> tuple1, Tuple<Message, Connection> tuple2) {
      // delivery probability of tuple1's message with tuple1's connection
      double p1 =
          ((ProphetRouter) tuple1.getValue().getOtherNode(ProphetRouter.this.getHost()).getRouter())
              .getPredFor(tuple1.getKey().getTo());
      // -"- tuple2...
      double p2 =
          ((ProphetRouter) tuple2.getValue().getOtherNode(ProphetRouter.this.getHost()).getRouter())
              .getPredFor(tuple2.getKey().getTo());

      // bigger probability should come first
      if (p2 - p1 == 0) {
        /* equal probabilities -> let queue mode decide */
        return ProphetRouter.this.compareByQueueMode(tuple1.getKey(), tuple2.getKey());
      } else if (p2 - p1 < 0) {
        return -1;
      } else {
        return 1;
      }
    }
  }
}
