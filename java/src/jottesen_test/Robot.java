package jottesen_test;

import jottesen_test.util.*;

import battlecode.common.*;

public abstract class Robot {

  protected final RobotController rc;
  
  protected final Team TEAM;
  protected final Team OPPONENT;

  // Common utility classes
  protected final MapData mapData;
  protected final Communication comms;

  // Tower counts
  protected int paintTowers;
  protected int moneyTowers;
  protected int defenseTowers;
  
  public Robot(RobotController rc_) throws GameActionException {
    rc = rc_;
    
    TEAM = rc.getTeam();
    OPPONENT = TEAM.opponent();
    
    mapData = new MapData(rc);
    mapData.updateAllVisible();

    comms = new Communication(rc);

    //set all tower counts to -1 that way we know that they haven't been set yet
    paintTowers = -1;
    moneyTowers = -1;
    defenseTowers = -1;
  };

  final public void run() throws GameActionException {
    doMicro(); // Act based on immediate surroundings
    doMacro(); // Secondarily act to achieve big picture goal
  }

  /**
   * Handles the micro game of the robot.
   */
  protected abstract void doMicro() throws GameActionException;

  /**
   * Handles the macro game of the robot.
   */
  protected abstract void doMacro() throws GameActionException;
}