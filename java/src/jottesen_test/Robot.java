package jottesen_test;

import battlecode.common.*;

public abstract class Robot {

  protected final RobotController rc;
  
  protected final Team TEAM;
  protected final Team OPPONENT;
  
  public Robot(RobotController rc_) {
    rc = rc_;
    TEAM = rc.getTeam();
    OPPONENT = TEAM.opponent();
  };

  public abstract void run() throws GameActionException;
}