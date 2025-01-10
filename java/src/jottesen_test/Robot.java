package jottesen_test;

import battlecode.common.*;

public abstract class Robot {

  public static RobotController rc;

  public Robot(RobotController rc_) {
    rc = rc_;
  };

  public abstract void run() throws GameActionException;
}