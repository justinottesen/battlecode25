package jottesen_test;

import battlecode.common.*;

public abstract class Robot {

  protected final RobotController rc;
  
  protected final Team TEAM;
  protected final Team OPPONENT;

  protected final int MAP_WIDTH;
  protected final int MAP_HEIGHT;
  protected final MapLocation MAP_CENTER;
  
  public Robot(RobotController rc_) throws GameActionException {
    rc = rc_;

    TEAM = rc.getTeam();
    OPPONENT = TEAM.opponent();

    MAP_WIDTH = rc.getMapWidth();
    MAP_HEIGHT = rc.getMapHeight();
    MAP_CENTER = new MapLocation(MAP_WIDTH / 2, MAP_HEIGHT / 2);

  };

  public abstract void run() throws GameActionException;
}