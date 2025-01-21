package quals;

import quals.util.*;

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
    BugPath.init();
  };

  final public void run() throws GameActionException {
    MovementManager.update();
    MapData.updateVisibleRuins();
    Goal.Type currentGoalType = GoalManager.current().type;
    if ((currentGoalType == Goal.Type.REFILL_PAINT || currentGoalType == Goal.Type.GET_BACKUP) && 
        !MapData.isFriendlyTower(GoalManager.current().target)) {
      GoalManager.replaceTopGoal(currentGoalType, MapData.closestFriendlyTower());
    }
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