package sprint_1.robots;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public class Tower extends Robot {


  static final Random rng = new Random(6147);
  static boolean spawnOne = true;
  static boolean spawnMopper = true;



    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
  public Tower(RobotController rc) {
    super(rc);

  }

  @Override
  public void run() throws GameActionException {
    Direction dir;
    MapLocation nextLoc;
    if (spawnOne){
      dir = directions[rng.nextInt(directions.length)];
      nextLoc = rc.getLocation().add(dir);
      if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
          rc.buildRobot(UnitType.SOLDIER, nextLoc);
          spawnOne = false;
      }
    }

    if (spawnMopper){
      dir = directions[rng.nextInt(directions.length)];
      nextLoc = rc.getLocation().add(dir);
      if (rc.canBuildRobot(UnitType.MOPPER, nextLoc)) {
        rc.buildRobot(UnitType.MOPPER, nextLoc);
        spawnMopper = false;
      }
    }


    // Pick a direction to build in.



    // If we are losing money as fast or faster than last turn keep spending
    // If we are gaining money, we are saving so don't produce
    if(rc.getRoundNum() > 500 && rc.getMoney() > 1250){
      dir = directions[rng.nextInt(directions.length)];
      nextLoc = rc.getLocation().add(dir);
      if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
        rc.buildRobot(UnitType.SOLDIER, nextLoc);
      }
    }


    // Only programmed in towers without defense boost as current gameplan does not involve defense
    RobotInfo[] enemyRobots = rc.senseNearbyRobots(rc.getType().actionRadiusSquared , rc.getTeam().opponent());
    
    if (enemyRobots.length > 0){
      RobotInfo lowestHealth = enemyRobots[0];
      rc.attack(null);
    // Check if there is an enemy you can one shot 
      for (RobotInfo enemy : enemyRobots){
        if (enemy.getHealth() < lowestHealth.getHealth() && enemy.getHealth() >= rc.getType().attackStrength){
          lowestHealth = enemy;
        }
      }
      rc.attack(lowestHealth.location);
    }
  }



};