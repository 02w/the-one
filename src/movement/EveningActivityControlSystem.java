/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.DTNSim;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * This class controls the group mobility of the people meeting their friends in the evening
 *
 * @author Frans Ekman
 */
public class EveningActivityControlSystem {

  private static HashMap<Integer, EveningActivityControlSystem> controlSystems;

  static {
    DTNSim.registerForReset(EveningActivityControlSystem.class.getCanonicalName());
    EveningActivityControlSystem.reset();
  }

  private final HashMap<Integer, EveningActivityMovement> eveningActivityNodes;
  private List<Coord> meetingSpots;
  private EveningTrip[] nextTrips;
  private Random rng;

  /**
   * Creates a new instance of EveningActivityControlSystem without any nodes or meeting spots, with
   * the ID given as parameter
   *
   * @param id
   */
  private EveningActivityControlSystem(int id) {
    this.eveningActivityNodes = new HashMap<>();
  }

  public static void reset() {
    EveningActivityControlSystem.controlSystems = new HashMap<>();
  }

  /**
   * Returns a reference to a EveningActivityControlSystem with ID provided as parameter. If a
   * system does not already exist with the requested ID, a new one is created.
   *
   * @param id unique ID of the EveningActivityControlSystem
   * @return The EveningActivityControlSystem with the provided ID
   */
  public static EveningActivityControlSystem getEveningActivityControlSystem(int id) {
    if (EveningActivityControlSystem.controlSystems.containsKey(new Integer(id))) {
      return EveningActivityControlSystem.controlSystems.get(new Integer(id));
    } else {
      EveningActivityControlSystem scs = new EveningActivityControlSystem(id);
      EveningActivityControlSystem.controlSystems.put(new Integer(id), scs);
      return scs;
    }
  }

  /**
   * Register a evening activity node with the system
   *
   * @param eveningMovement activity movement
   */
  public void addEveningActivityNode(EveningActivityMovement eveningMovement) {
    this.eveningActivityNodes.put(new Integer(eveningMovement.getID()), eveningMovement);
  }

  /**
   * Sets the meeting locations the nodes can choose among
   *
   * @param meetingSpots
   */
  public void setMeetingSpots(List<Coord> meetingSpots) {
    this.meetingSpots = meetingSpots;
    this.nextTrips = new EveningTrip[meetingSpots.size()];
  }

  /**
   * This method gets the instruction for a node, i.e. When/where and with whom to go.
   *
   * @param eveningActivityNodeID unique ID of the node
   * @return Instructions object
   */
  public EveningTrip getEveningInstructions(int eveningActivityNodeID) {
    EveningActivityMovement eveningMovement =
        this.eveningActivityNodes.get(new Integer(eveningActivityNodeID));
    if (eveningMovement != null) {
      int index = eveningActivityNodeID % this.meetingSpots.size();
      if (this.nextTrips[index] == null) {
        int nrOfEveningMovementNodes =
            (int)
                (eveningMovement.getMinGroupSize()
                    + (double)
                            (eveningMovement.getMaxGroupSize() - eveningMovement.getMinGroupSize())
                        * this.rng.nextDouble());
        Coord loc = this.meetingSpots.get(index).clone();
        this.nextTrips[index] = new EveningTrip(nrOfEveningMovementNodes, loc);
      }
      this.nextTrips[index].addNode(eveningMovement);
      if (this.nextTrips[index].isFull()) {
        EveningTrip temp = this.nextTrips[index];
        this.nextTrips[index] = null;
        return temp;
      } else {
        return this.nextTrips[index];
      }
    }
    return null;
  }

  /**
   * Get the meeting spot for the node
   *
   * @param id
   * @return Coordinates of the spot
   */
  public Coord getMeetingSpotForID(int id) {
    int index = id % this.meetingSpots.size();
    Coord loc = this.meetingSpots.get(index).clone();
    return loc;
  }

  /**
   * Sets the random number generator to be used
   *
   * @param rand
   */
  public void setRandomNumberGenerator(Random rand) {
    this.rng = rand;
  }
}
