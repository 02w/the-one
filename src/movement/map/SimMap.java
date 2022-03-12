/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement.map;

import core.Coord;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A simulation map for node movement. */
public class SimMap implements Serializable {
  private Coord minBound;
  private Coord maxBound;
  /** list representation of the map for efficient list-returning */
  private final ArrayList<MapNode> nodes;
  /** hash map presentation of the map for efficient finding node by coord */
  private final Map<Coord, MapNode> nodesMap;
  /** offset of map translations */
  private final Coord offset;
  /** is this map data mirrored after reading */
  private boolean isMirrored;

  /** is re-hash needed before using hash mode (some coordinates changed) */
  private boolean needsRehash = false;

  public SimMap(Map<Coord, MapNode> nodes) {
    this.offset = new Coord(0, 0);
    this.nodes = new ArrayList<>(nodes.values());
    this.nodesMap = nodes;
    this.isMirrored = false;
    this.setBounds();
  }

  /**
   * Returns all the map nodes in a list
   *
   * @return all the map nodes in a list
   */
  public List<MapNode> getNodes() {
    return this.nodes;
  }

  /**
   * Returns a MapNode at given coordinates or null if there's no MapNode in the location of the
   * coordinate
   *
   * @param c The coordinate
   * @return The map node in that location or null if it doesn't exist
   */
  public MapNode getNodeByCoord(Coord c) {
    if (this.needsRehash) { // some coordinates have changed after creating hash
      this.nodesMap.clear();
      for (MapNode node : this.getNodes()) {
        this.nodesMap.put(node.getLocation(), node); // re-hash
      }
    }

    return this.nodesMap.get(c);
  }

  /**
   * Returns the upper left corner coordinate of the map
   *
   * @return the upper left corner coordinate of the map
   */
  public Coord getMinBound() {
    return this.minBound;
  }

  /**
   * Returns the lower right corner coordinate of the map
   *
   * @return the lower right corner coordinate of the map
   */
  public Coord getMaxBound() {
    return this.maxBound;
  }

  /**
   * Returns the offset that has been caused by translates made to this map (does NOT take into
   * account mirroring).
   *
   * @return The current offset
   */
  public Coord getOffset() {
    return this.offset;
  }

  /**
   * Returns true if this map has been mirrored after reading
   *
   * @return true if this map has been mirrored after reading
   * @see #mirror()
   */
  public boolean isMirrored() {
    return this.isMirrored;
  }

  /**
   * Translate whole map by dx and dy
   *
   * @param dx The amount to translate X coordinates
   * @param dy the amount to translate Y coordinates
   */
  public void translate(double dx, double dy) {
    for (MapNode n : this.nodes) {
      n.getLocation().translate(dx, dy);
    }

    this.minBound.translate(dx, dy);
    this.maxBound.translate(dx, dy);
    this.offset.translate(dx, dy);

    this.needsRehash = true;
  }

  /** Mirrors all map coordinates around X axis (x'=x, y'=-y). */
  public void mirror() {
    assert !this.isMirrored : "Map data already mirrored";

    Coord c;
    for (MapNode n : this.nodes) {
      c = n.getLocation();
      c.setLocation(c.getX(), -c.getY());
    }
    this.setBounds();
    this.isMirrored = true;
    this.needsRehash = true;
  }

  /** Updates the min & max bounds to conform to the values of the map nodes. */
  private void setBounds() {
    double minX, minY, maxX, maxY;
    Coord c;
    minX = minY = Double.MAX_VALUE;
    maxX = maxY = -Double.MAX_VALUE;

    for (MapNode n : this.nodes) {
      c = n.getLocation();
      if (c.getX() < minX) {
        minX = c.getX();
      }
      if (c.getX() > maxX) {
        maxX = c.getX();
      }
      if (c.getY() < minY) {
        minY = c.getY();
      }
      if (c.getY() > maxY) {
        maxY = c.getY();
      }
    }
    this.minBound = new Coord(minX, minY);
    this.maxBound = new Coord(maxX, maxY);
  }

  /**
   * Returns a String representation of the map
   *
   * @return a String representation of the map
   */
  @Override
  public String toString() {
    return this.nodes.toString();
  }
}
