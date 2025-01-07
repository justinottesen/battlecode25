package sprint_1.robots;

import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;

<<<<<<< HEAD:java/src/sprint_1/robots/Splasher.java
public class Splasher extends Robot {

  public Splasher(RobotController rc) {
=======
public class Soldier extends Robot {
  static final Random rng = new Random(6147);
  MapLocation goal;
  Pathfinding pathfinding;

  public Soldier(RobotController rc) {
>>>>>>> 3b37d7733f35aa9b2f36001683475b43cdb3492a:java/src/sprint_1/Soldier.java
    super(rc);
    goal = new MapLocation(rng.nextInt(rc.getMapWidth()),rng.nextInt(rc.getMapHeight()));
    pathfinding=new Pathfinding();
  }

  @Override
  public void run() throws GameActionException {
    pathfinding.moveTo(goal);
    rc.setIndicatorString("Moving to "+goal);
  }

};