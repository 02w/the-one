/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * Classes derived from this can make use of other movement models that implement the
 * SwitchableMovement interface.
 *
 * @author Frans Ekman
 */
public abstract class ExtendedMovementModel extends MovementModel {

  private SwitchableMovement currentMovementModel;
  private boolean getPathCalledOnce;

  /** Creates a new ExtendedMovementModel */
  public ExtendedMovementModel() {
    super();
  }

  /**
   * Creates a new ExtendedMovementModel
   *
   * @param settings
   */
  public ExtendedMovementModel(Settings settings) {
    super(settings);
  }

  /**
   * Creates a new ExtendedMovementModel from a prototype
   *
   * @param mm
   */
  public ExtendedMovementModel(ExtendedMovementModel mm) {
    super(mm);
  }

  /**
   * @return The movement model currently in use
   */
  public SwitchableMovement getCurrentMovementModel() {
    return this.currentMovementModel;
  }

  /**
   * Sets the current movement model to be used the next time getPath() is called
   *
   * @param mm Next movement model
   */
  public void setCurrentMovementModel(SwitchableMovement mm) {
    Coord lastLocation = null;
    if (this.currentMovementModel != null) {
      lastLocation = this.currentMovementModel.getLastLocation();
    }
    this.currentMovementModel = mm;
    if (lastLocation != null) {
      this.currentMovementModel.setLocation(lastLocation);
    }
  }

  @Override
  public Path getPath() {
    if (this.getPathCalledOnce) {
      if (this.currentMovementModel.isReady()) {
        this.newOrders();
      }
    }
    this.getPathCalledOnce = true;
    return ((MovementModel) this.currentMovementModel).getPath();
  }

  @Override
  protected double generateWaitTime() {
    return ((MovementModel) this.currentMovementModel).generateWaitTime();
  }

  /**
   * Method is called between each getPath() request when the current MM is ready (isReady() method
   * returns true). Subclasses should implement all changes of state that need to be made here, for
   * example switching mobility model, etc.
   *
   * @return true if success
   */
  public abstract boolean newOrders();
}
