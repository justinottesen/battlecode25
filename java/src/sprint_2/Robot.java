package sprint_2;

import sprint_2.util.*;

import battlecode.common.*;

public abstract class Robot {

  public static RobotController rc;
  
  public static Team TEAM;
  public static Team OPPONENT;
  
  public static int CREATED_ROUND;

  // Goal stack
  
  public Robot(RobotController rc_) throws GameActionException {
    rc = rc_;
    
    TEAM = rc.getTeam();
    OPPONENT = TEAM.opponent();

    CREATED_ROUND = rc.getRoundNum();
    
    // THIS ORDER OF INITIALIZATION IS IMPORTANT
    MapData.init();
    MapData.updateAllVisible();
    GoalManager.init();
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