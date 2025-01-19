package sprint_2;

import sprint_2.util.*;

import battlecode.common.*;

public abstract class Robot {

  protected final RobotController rc;
  
  protected final Team TEAM;
  protected final Team OPPONENT;

  protected final int CREATED_ROUND;

  // Common utility classes
  protected final MapData mapData;
  protected final Pathfinding pathfinding;
  protected final GoalManager goals;
  protected final Communication comms;

  // Goal stack
  
  public Robot(RobotController rc_) throws GameActionException {
    rc = rc_;
    
    TEAM = rc.getTeam();
    OPPONENT = TEAM.opponent();

    CREATED_ROUND = rc.getRoundNum();
    
    mapData = new MapData(rc);
    mapData.updateAllVisible();

    pathfinding = new Pathfinding(rc, mapData);
    pathfinding.setExplore();
    goals = new GoalManager(rc, pathfinding, mapData);

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