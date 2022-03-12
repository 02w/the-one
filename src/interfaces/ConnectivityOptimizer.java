/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package interfaces;

import core.NetworkInterface;
import java.util.Collection;

/**
 * A superclass for schemes for optimizing the location of possible contacts with network interfaces
 * of a specific range
 */
public abstract class ConnectivityOptimizer {

  /** Adds a network interface to the optimizer (unless it is already present) */
  public abstract void addInterface(NetworkInterface ni);

  /** Adds a collection of network interfaces to the optimizer (except of those already added */
  public abstract void addInterfaces(Collection<NetworkInterface> interfaces);

  /** Updates a network interface's location */
  public abstract void updateLocation(NetworkInterface ni);

  /**
   * Finds all network interfaces that might be located so that they can be connected with the
   * network interface
   *
   * @param ni network interface that needs to be connected
   * @return A collection of network interfaces within proximity
   */
  public abstract Collection<NetworkInterface> getNearInterfaces(NetworkInterface ni);

  /** Finds all other interfaces that are registered to the ConnectivityOptimizer */
  public abstract Collection<NetworkInterface> getAllInterfaces();
}
