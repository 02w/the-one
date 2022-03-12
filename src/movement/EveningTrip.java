/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;

/**
 * A class to encapsulate information about a shopping trip 1. Where the trip begins 2. Where it
 * ends 3. The path 4. All nodes in the group
 *
 * @author Frans Ekman
 */
public class EveningTrip {
  private final EveningActivityMovement[] eveningActivityNodes;
  private int eveningActivityNodesInBuffer;
  private Path path;
  private final Coord location;
  private Coord destination;
  private double waitTimeAtEnd;

  /**
   * Create a new instance of a EveningTrip
   *
   * @param nrOfeveningActivityNodes The number of shoppers in the group
   * @param location Where the trip starts
   */
  public EveningTrip(int nrOfeveningActivityNodes, Coord location) {
    this.eveningActivityNodes = new EveningActivityMovement[nrOfeveningActivityNodes];
    this.location = location;
    this.eveningActivityNodesInBuffer = 0;
  }

  /**
   * Add an evening activity node to the group
   *
   * @param eveningActivityNode
   * @return true if there was room in the group
   */
  public boolean addNode(EveningActivityMovement eveningActivityNode) {
    if (this.isFull()) {
      return false;
    } else {
      this.eveningActivityNodes[this.eveningActivityNodesInBuffer] = eveningActivityNode;
      this.eveningActivityNodesInBuffer++;
      return true;
    }
  }

  /**
   * @return The shopping trip path
   */
  public Path getPath() {
    if (this.path != null) {
      return new Path(this.path);
    } else {
      return null;
    }
  }

  /**
   * Sets the shopping path for the group
   *
   * @param path
   */
  public void setPath(Path path) {
    this.path = new Path(path);
  }

  /**
   * @return The location where the shopping trip starts
   */
  public Coord getLocation() {
    return this.location;
  }

  /**
   * @return true if the group is full
   */
  public boolean isFull() {
    return this.eveningActivityNodesInBuffer >= this.eveningActivityNodes.length;
  }

  /**
   * Checks if all members of the group have found their way to the meeting point
   *
   * @return true if all nodes are there
   */
  public boolean allMembersPresent() {
    if (!this.isFull()) {
      return false;
    }
    for (int i = 0; i < this.eveningActivityNodes.length; i++) {
      if (!this.eveningActivityNodes[i].isReadyToShop()) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return The destination square of the shopping trip
   */
  public Coord getDestination() {
    return this.destination;
  }

  /**
   * Sets the destination square of the trip. MUST be the same as the last node in the path
   *
   * @param destination
   */
  public void setDestination(Coord destination) {
    this.destination = destination;
  }

  public double getWaitTimeAtEnd() {
    return this.waitTimeAtEnd;
  }

  public void setWaitTimeAtEnd(double waitTimeAtEnd) {
    this.waitTimeAtEnd = waitTimeAtEnd;
  }
}
