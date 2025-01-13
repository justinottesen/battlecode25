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
  
  public Robot(RobotController rc_) throws GameActionException {
    rc = rc_;
    
    TEAM = rc.getTeam();
    OPPONENT = TEAM.opponent();
    
    mapData = new MapData(rc);
    mapData.updateAllVisible();

    comms = new Communication(rc);
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