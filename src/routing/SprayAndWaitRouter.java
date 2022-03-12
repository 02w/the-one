/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Spray and wait router as depicted in <I>Spray and Wait: An Efficient Routing
 * Scheme for Intermittently Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 */
public class SprayAndWaitRouter extends ActiveRouter {
  /** identifier for the initial number of copies setting ({@value}) */
  public static final String NROF_COPIES = "nrofCopies";
  /** identifier for the binary-mode setting ({@value}) */
  public static final String BINARY_MODE = "binaryMode";
  /** SprayAndWait router's settings name space ({@value}) */
  public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
  /** Message property key */
  public static final String MSG_COUNT_PROPERTY = SprayAndWaitRouter.SPRAYANDWAIT_NS + "." + "copies";

  protected int initialNrofCopies;
  protected boolean isBinary;

  public SprayAndWaitRouter(Settings s) {
    super(s);
    Settings snwSettings = new Settings(SprayAndWaitRouter.SPRAYANDWAIT_NS);

    this.initialNrofCopies = snwSettings.getInt(SprayAndWaitRouter.NROF_COPIES);
    this.isBinary = snwSettings.getBoolean(SprayAndWaitRouter.BINARY_MODE);
  }

  /**
   * Copy constructor.
   *
   * @param r The router prototype where setting values are copied from
   */
  protected SprayAndWaitRouter(SprayAndWaitRouter r) {
    super(r);
    this.initialNrofCopies = r.initialNrofCopies;
    this.isBinary = r.isBinary;
  }

  @Override
  public int receiveMessage(Message m, DTNHost from) {
    return super.receiveMessage(m, from);
  }

  @Override
  public Message messageTransferred(String id, DTNHost from) {
    Message msg = super.messageTransferred(id, from);
    Integer nrofCopies = (Integer) msg.getProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY);

    assert nrofCopies != null : "Not a SnW message: " + msg;

    if (this.isBinary) {
      /* in binary S'n'W the receiving node gets floor(n/2) copies */
      nrofCopies = (int) Math.floor(nrofCopies / 2.0);
    } else {
      /* in standard S'n'W the receiving node gets only single copy */
      nrofCopies = 1;
    }

    msg.updateProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY, nrofCopies);
    return msg;
  }

  @Override
  public boolean createNewMessage(Message msg) {
    this.makeRoomForNewMessage(msg.getSize());

    msg.setTtl(this.msgTtl);
    msg.addProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY, new Integer(this.initialNrofCopies));
    this.addToMessages(msg, true);
    return true;
  }

  @Override
  public void update() {
    super.update();
    if (!this.canStartTransfer() || this.isTransferring()) {
      return; // nothing to transfer or is currently transferring
    }

    /* try messages that could be delivered to final recipient */
    if (this.exchangeDeliverableMessages() != null) {
      return;
    }

    /* create a list of SAWMessages that have copies left to distribute */
    @SuppressWarnings(value = "unchecked")
    List<Message> copiesLeft = this.sortByQueueMode(this.getMessagesWithCopiesLeft());

    if (copiesLeft.size() > 0) {
      /* try to send those messages */
      this.tryMessagesToConnections(copiesLeft, this.getConnections());
    }
  }

  /**
   * Creates and returns a list of messages this router is currently carrying and still has copies
   * left to distribute (nrof copies > 1).
   *
   * @return A list of messages that have copies left
   */
  protected List<Message> getMessagesWithCopiesLeft() {
    List<Message> list = new ArrayList<>();

    for (Message m : this.getMessageCollection()) {
      Integer nrofCopies = (Integer) m.getProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY);
      assert nrofCopies != null : "SnW message " + m + " didn't have " + "nrof copies property!";
      if (nrofCopies > 1) {
        list.add(m);
      }
    }

    return list;
  }

  /**
   * Called just before a transfer is finalized (by {@link ActiveRouter#update()}). Reduces the
   * number of copies we have left for a message. In binary Spray and Wait, sending host is left
   * with floor(n/2) copies, but in standard mode, nrof copies left is reduced by one.
   */
  @Override
  protected void transferDone(Connection con) {
    Integer nrofCopies;
    String msgId = con.getMessage().getId();
    /* get this router's copy of the message */
    Message msg = this.getMessage(msgId);

    if (msg == null) { // message has been dropped from the buffer after..
      return; // ..start of transfer -> no need to reduce amount of copies
    }

    /* reduce the amount of copies left */
    nrofCopies = (Integer) msg.getProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY);
    if (this.isBinary) {
      /* in binary S'n'W the sending node keeps ceil(n/2) copies */
      nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
    } else {
      nrofCopies--;
    }
    msg.updateProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY, nrofCopies);
  }

  @Override
  public SprayAndWaitRouter replicate() {
    return new SprayAndWaitRouter(this);
  }
}
