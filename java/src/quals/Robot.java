package quals;

import java.util.*;

import battlecode.common.*;
import quals.util.*;

public abstract class Robot {
  
  public static RobotController rc;
  
  public static Random rng;

  public static Team TEAM;
  public static Team OPPONENT;
  
  public static int CREATED_ROUND;

  public static int turnNum = 1;

  public static MapLocation spawnTower;

  public static String emptyTowers = "";
  
  public Robot(RobotController rc_) throws GameActionException {
    rc = rc_;

    rng = new Random(rc.getID());
    
    TEAM = rc.getTeam();
    OPPONENT = TEAM.opponent();

    CREATED_ROUND = rc.getRoundNum();
    
    // THIS ORDER OF INITIALIZATION IS IMPORTANT
    MapData.init();
    MapData.updateAllVisible();
    spawnTower = MapData.closestFriendlyTower(); 
    if (spawnTower == null) { // If the tower suicides, might be a ruin now
      spawnTower = MapData.closestRuin();
    }
    GoalManager.init();
  };

  final public void run() throws GameActionException {
    if (turnNum == 2) {
      BugPath.init();
    }
    MovementManager.update();
    if (turnNum != 1) {
      MapData.updateVisibleRuins();
    }
    Goal.Type currentGoalType = GoalManager.current().type;
    doMicro(); // Act based on immediate surroundings
    doMacro(); // Secondarily act to achieve big picture goal
    ++turnNum;
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