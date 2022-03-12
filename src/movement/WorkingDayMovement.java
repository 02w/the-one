/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * This movement model makes use of several other movement models to simulate movement with daily
 * routines. People wake up in the morning, go to work, go shopping or similar activities in the
 * evening and finally go home to sleep.
 *
 * @author Frans Ekman
 */
public class WorkingDayMovement extends ExtendedMovementModel {

  public static final String PROBABILITY_TO_OWN_CAR_SETTING = "ownCarProb";
  public static final String PROBABILITY_TO_GO_SHOPPING_SETTING = "probGoShoppingAfterWork";
  private static final int BUS_TO_WORK_MODE = 0;
  private static final int BUS_TO_HOME_MODE = 1;
  private static final int BUS_TO_EVENING_ACTIVITY_MODE = 2;
  private static final int WORK_MODE = 3;
  private static final int HOME_MODE = 4;
  private static final int EVENING_ACTIVITY_MODE = 5;
  private final BusTravellerMovement busTravellerMM;
  private final OfficeActivityMovement workerMM;
  private final HomeActivityMovement homeMM;
  private final EveningActivityMovement eveningActivityMovement;
  private final CarMovement carMM;
  private final TransportMovement movementUsedForTransfers;
  private int mode;

  private final double ownCarProb;
  private final double doEveningActivityProb;

  /**
   * Creates a new instance of WorkingDayMovement
   *
   * @param settings
   */
  public WorkingDayMovement(Settings settings) {
    super(settings);
    this.busTravellerMM = new BusTravellerMovement(settings);
    this.workerMM = new OfficeActivityMovement(settings);
    this.homeMM = new HomeActivityMovement(settings);
    this.eveningActivityMovement = new EveningActivityMovement(settings);
    this.carMM = new CarMovement(settings);
    this.ownCarProb = settings.getDouble(WorkingDayMovement.PROBABILITY_TO_OWN_CAR_SETTING);
    if (MovementModel.rng.nextDouble() < this.ownCarProb) {
      this.movementUsedForTransfers = this.carMM;
    } else {
      this.movementUsedForTransfers = this.busTravellerMM;
    }
    this.doEveningActivityProb = settings.getDouble(
        WorkingDayMovement.PROBABILITY_TO_GO_SHOPPING_SETTING);

    this.setCurrentMovementModel(this.homeMM);
    this.mode = WorkingDayMovement.HOME_MODE;
  }

  /**
   * Creates a new instance of WorkingDayMovement from a prototype
   *
   * @param proto
   */
  public WorkingDayMovement(WorkingDayMovement proto) {
    super(proto);
    this.busTravellerMM = new BusTravellerMovement(proto.busTravellerMM);
    this.workerMM = new OfficeActivityMovement(proto.workerMM);
    this.homeMM = new HomeActivityMovement(proto.homeMM);
    this.eveningActivityMovement = new EveningActivityMovement(proto.eveningActivityMovement);
    this.carMM = new CarMovement(proto.carMM);

    this.ownCarProb = proto.ownCarProb;
    if (MovementModel.rng.nextDouble() < this.ownCarProb) {
      this.movementUsedForTransfers = this.carMM;
    } else {
      this.movementUsedForTransfers = this.busTravellerMM;
    }
    this.doEveningActivityProb = proto.doEveningActivityProb;

    this.setCurrentMovementModel(this.homeMM);
    this.mode = proto.mode;
  }

  @Override
  public boolean newOrders() {
    switch (this.mode) {
      case WorkingDayMovement.WORK_MODE:
        if (this.workerMM.isReady()) {
          this.setCurrentMovementModel(this.movementUsedForTransfers);
          if (this.doEveningActivityProb > MovementModel.rng.nextDouble()) {
            this.movementUsedForTransfers.setNextRoute(
                this.workerMM.getOfficeLocation(),
                this.eveningActivityMovement.getShoppingLocationAndGetReady());
            this.mode = WorkingDayMovement.BUS_TO_EVENING_ACTIVITY_MODE;
          } else {
            this.movementUsedForTransfers.setNextRoute(
                this.workerMM.getOfficeLocation(), this.homeMM.getHomeLocation());
            this.mode = WorkingDayMovement.BUS_TO_HOME_MODE;
          }
        }
        break;
      case WorkingDayMovement.HOME_MODE:
        if (this.homeMM.isReady()) {
          this.setCurrentMovementModel(this.movementUsedForTransfers);
          this.movementUsedForTransfers.setNextRoute(
              this.homeMM.getHomeLocation(), this.workerMM.getOfficeLocation());
          this.mode = WorkingDayMovement.BUS_TO_WORK_MODE;
        }
        break;
      case WorkingDayMovement.EVENING_ACTIVITY_MODE:
        if (this.eveningActivityMovement.isReady()) {
          this.setCurrentMovementModel(this.movementUsedForTransfers);
          this.movementUsedForTransfers.setNextRoute(
              this.eveningActivityMovement.getLastLocation(), this.homeMM.getHomeLocation());
          this.mode = WorkingDayMovement.BUS_TO_HOME_MODE;
        }
        break;
      case WorkingDayMovement.BUS_TO_WORK_MODE:
        if (this.movementUsedForTransfers.isReady()) {
          this.setCurrentMovementModel(this.workerMM);
          this.mode = WorkingDayMovement.WORK_MODE;
        }
        break;
      case WorkingDayMovement.BUS_TO_HOME_MODE:
        if (this.movementUsedForTransfers.isReady()) {
          this.setCurrentMovementModel(this.homeMM);
          this.mode = WorkingDayMovement.HOME_MODE;
        }
        break;
      case WorkingDayMovement.BUS_TO_EVENING_ACTIVITY_MODE:
        if (this.movementUsedForTransfers.isReady()) {
          this.setCurrentMovementModel(this.eveningActivityMovement);
          this.mode = WorkingDayMovement.EVENING_ACTIVITY_MODE;
        }
        break;
      default:
        break;
    }
    return true;
  }

  @Override
  public Coord getInitialLocation() {
    Coord homeLoc = this.homeMM.getHomeLocation().clone();
    this.homeMM.setLocation(homeLoc);
    return homeLoc;
  }

  @Override
  public MovementModel replicate() {
    return new WorkingDayMovement(this);
  }

  public Coord getOfficeLocation() {
    return this.workerMM.getOfficeLocation().clone();
  }

  public Coord getHomeLocation() {
    return this.homeMM.getHomeLocation().clone();
  }

  public Coord getShoppingLocation() {
    return this.eveningActivityMovement.getShoppingLocation().clone();
  }
}
