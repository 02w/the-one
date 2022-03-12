/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import routing.MessageRouter;

/** A constant bit-rate connection between two DTN nodes. */
public class CBRConnection extends Connection {
  private final int speed;
  private double transferDoneTime;

  /**
   * Creates a new connection between nodes and sets the connection state to "up".
   *
   * @param fromNode The node that initiated the connection
   * @param fromInterface The interface that initiated the connection
   * @param toNode The node in the other side of the connection
   * @param toInterface The interface in the other side of the connection
   * @param connectionSpeed Transfer speed of the connection (Bps) when the connection is initiated
   */
  public CBRConnection(
      DTNHost fromNode,
      NetworkInterface fromInterface,
      DTNHost toNode,
      NetworkInterface toInterface,
      int connectionSpeed) {
    super(fromNode, fromInterface, toNode, toInterface);
    this.speed = connectionSpeed;
    this.transferDoneTime = 0;
  }

  /**
   * Sets a message that this connection is currently transferring. If message passing is controlled
   * by external events, this method is not needed (but then e.g. {@link #finalizeTransfer()} and
   * {@link #isMessageTransferred()} will not work either). Only a one message at a time can be
   * transferred using one connection.
   *
   * @param from The host sending the message
   * @param m The message
   * @return The value returned by {@link MessageRouter#receiveMessage(Message, DTNHost)}
   */
  @Override
  public int startTransfer(DTNHost from, Message m) {
    assert this.msgOnFly == null
        : "Already transferring "
            + this.msgOnFly
            + " from "
            + this.msgFromNode
            + " to "
            + this.getOtherNode(this.msgFromNode)
            + ". Can't "
            + "start transfer of "
            + m
            + " from "
            + from;

    this.msgFromNode = from;
    Message newMessage = m.replicate();
    int retVal = this.getOtherNode(from).receiveMessage(newMessage, from);

    if (retVal == MessageRouter.RCV_OK) {
      this.msgOnFly = newMessage;
      this.transferDoneTime = SimClock.getTime() + (1.0 * m.getSize()) / this.speed;
    }

    return retVal;
  }

  /** Aborts the transfer of the currently transferred message. */
  @Override
  public void abortTransfer() {
    assert this.msgOnFly != null : "No message to abort at " + this.msgFromNode;
    this.getOtherNode(this.msgFromNode)
        .messageAborted(this.msgOnFly.getId(), this.msgFromNode, this.getRemainingByteCount());
    this.clearMsgOnFly();
    this.transferDoneTime = 0;
  }

  /** Gets the transferdonetime */
  public double getTransferDoneTime() {
    return this.transferDoneTime;
  }

  /**
   * Returns true if the current message transfer is done.
   *
   * @return True if the transfer is done, false if not
   */
  @Override
  public boolean isMessageTransferred() {
    return this.getRemainingByteCount() == 0;
  }

  /** returns the current speed of the connection */
  @Override
  public double getSpeed() {
    return this.speed;
  }

  /**
   * Returns the amount of bytes to be transferred before ongoing transfer is ready or 0 if there's
   * no ongoing transfer or it has finished already
   *
   * @return the amount of bytes to be transferred
   */
  @Override
  public int getRemainingByteCount() {
    int remaining;

    if (this.msgOnFly == null) {
      return 0;
    }

    remaining = (int) ((this.transferDoneTime - SimClock.getTime()) * this.speed);

    return (remaining > 0 ? remaining : 0);
  }

  /** Returns a String presentation of the connection. */
  @Override
  public String toString() {
    return super.toString()
        + (this.isTransferring() ? " until " + String.format("%.2f", this.transferDoneTime) : "");
  }
}
