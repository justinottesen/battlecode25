package sprint_1;

import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;

public class Soldier extends Robot {
  static final Random rng = new Random(6147);
  MapLocation goal;
  Pathfinding pathfinding;

  public Soldier(RobotController rc) {
    super(rc);
    goal = new MapLocation(rng.nextInt(rc.getMapWidth()),rng.nextInt(rc.getMapHeight()));
    pathfinding=new Pathfinding();
  }

  @Override
  public void run() throws GameActionException {
    pathfinding.moveTo(goal);
    rc.setIndicatorString("Moving to "+goal);
  }
}