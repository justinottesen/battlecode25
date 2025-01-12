package jottesen_test;

import battlecode.common.*;

public abstract class Robot {

  protected final RobotController rc;
  
  protected final Team TEAM;
  protected final Team OPPONENT;

  protected final int MAP_WIDTH;
  protected final int MAP_HEIGHT;
  protected final MapLocation MAP_CENTER;

  protected final boolean[][] SRP_ARRAY;
  protected final boolean[][] PAINT_ARRAY;
  protected final boolean[][] MONEY_ARRAY;
  protected final boolean[][] CHIP_ARRAY;
  
  public Robot(RobotController rc_) throws GameActionException {
    rc = rc_;

    TEAM = rc.getTeam();
    OPPONENT = TEAM.opponent();

    MAP_WIDTH = rc.getMapWidth();
    MAP_HEIGHT = rc.getMapHeight();
    MAP_CENTER = new MapLocation(MAP_WIDTH / 2, MAP_HEIGHT / 2);

    SRP_ARRAY = rc.getResourcePattern();
    PAINT_ARRAY = rc.getTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER);
    MONEY_ARRAY = rc.getResourcePattern();
    CHIP_ARRAY = rc.getResourcePattern();
  };

  public abstract void run() throws GameActionException;
}