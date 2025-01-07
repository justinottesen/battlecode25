package sprint_1.robots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class Robot {

  public static RobotController rc;

  public Robot(RobotController rc_) {
    rc = rc_;
  };

  abstract public void run() throws GameActionException;
}