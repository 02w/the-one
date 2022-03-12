/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import java.util.LinkedList;
import java.util.List;
import movement.map.MapNode;

/**
 * This class controls the movement of busses. It informs the bus control system the bus is
 * registered with every time the bus stops.
 *
 * @author Frans Ekman
 */
public class BusMovement extends MapRouteMovement {

  private static int nextID = 0;
  private final BusControlSystem controlSystem;
  private final int id;
  private boolean startMode;
  private List<Coord> stops;

  /**
   * Creates a new instance of BusMovement
   *
   * @param settings
   */
  public BusMovement(Settings settings) {
    super(settings);
    int bcs = settings.getInt(BusControlSystem.BUS_CONTROL_SYSTEM_NR);
    this.controlSystem = BusControlSystem.getBusControlSystem(bcs);
    this.controlSystem.setMap(super.getMap());
    this.id = BusMovement.nextID++;
    this.controlSystem.registerBus(this);
    this.startMode = true;
    this.stops = new LinkedList<>();
    List<MapNode> stopNodes = super.getStops();
    for (MapNode node : stopNodes) {
      this.stops.add(node.getLocation().clone());
    }
    this.controlSystem.setBusStops(this.stops);
  }

  /**
   * Create a new instance from a prototype
   *
   * @param proto
   */
  public BusMovement(BusMovement proto) {
    super(proto);
    this.controlSystem = proto.controlSystem;
    this.id = BusMovement.nextID++;
    this.controlSystem.registerBus(this);
    this.startMode = true;
  }

  @Override
  public Coord getInitialLocation() {
    return (super.getInitialLocation()).clone();
  }

  @Override
  public Path getPath() {
    Coord lastLocation = (super.getLastLocation()).clone();
    Path path = super.getPath();
    if (!this.startMode) {
      this.controlSystem.busHasStopped(this.id, lastLocation, path);
    }
    this.startMode = false;
    return path;
  }

  @Override
  public BusMovement replicate() {
    return new BusMovement(this);
  }

  /**
   * Returns unique ID of the bus
   *
   * @return unique ID of the bus
   */
  public int getID() {
    return this.id;
  }
}
