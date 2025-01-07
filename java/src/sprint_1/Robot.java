package sprint_1;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class Robot {

  protected final RobotController rc;

  public Robot(RobotController rc_) {
    rc = rc_;
  };

  abstract public void run() throws GameActionException;
}