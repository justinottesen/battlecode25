package sprint_1.robots;

import java.util.Random;

import sprint_1.utils.*;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MapInfo;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;
import battlecode.common.PaintType;
import battlecode.common.UnitType;

public class Tower extends Robot {


  static final Random rng = new Random(6147);
  static boolean spawnOne = true;
  static boolean spawnMopper = true;
  int soldierSpawned = 1;
  int mopperSpawned = 1;
  int firstTurn = -1;



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
    firstTurn = rc.getRoundNum();
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
    if (!spawnMopper && !spawnOne){
      if (rc.getRoundNum() < 200){
        if (soldierSpawned % 4 == 0){
          dir = directions[rng.nextInt(directions.length)];
          nextLoc = rc.getLocation().add(dir);
          if (rc.canBuildRobot(UnitType.MOPPER, nextLoc)){
            rc.buildRobot(UnitType.MOPPER, nextLoc);
            mopperSpawned++;
          }
        } else {
          dir = directions[rng.nextInt(directions.length)];
          nextLoc = rc.getLocation().add(dir);
          if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            soldierSpawned++;
          }
        }
      } else if(rc.getRoundNum() >= 200 && rc.getMoney() > 1250){
        dir = directions[rng.nextInt(directions.length)];
        nextLoc = rc.getLocation().add(dir);
        if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
          rc.buildRobot(UnitType.SOLDIER, nextLoc);
        }
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
    if (firstTurn > 2 && rc.getType() == UnitType.LEVEL_ONE_MONEY_TOWER){
      if (rc.getPaint() < 20 && rc.getMoney() > 1000){
        boolean correctMark = true;
        for (MapInfo patternTile : rc.senseNearbyMapInfos(rc.getLocation(), 8)){
          if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
              correctMark = false;
          }
      }
      if (correctMark){
          if (Comm.requestMoneyTowerReplacement()){
            rc.disintegrate();
          }
        } else {
          Comm.requestPatternHelp();
        }
      }
    }
  }


};