package sprint_1.robots;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

public class Tower extends Robot {


  static final Random rng = new Random(6147);
  static boolean spawnOne = true;
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
    // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc) && spawnOne){
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            spawnOne=false;
        }
  }

};