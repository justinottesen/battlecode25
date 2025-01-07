package sprint_1.robots;

import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;

import sprint_1.utils.Pathfinding;
import sprint_1.managers.CaptureManager;
import sprint_1.utils.Explore;
import sprint_1.utils.MapData;

public class Soldier extends Robot {
  static final Random rng = new Random(6147);
  MapLocation goal;
  Pathfinding pathfinding;
  CaptureManager captureManager;
  MapData mapData;
  Explore explore;



  public Soldier(RobotController rc) {
    super(rc);
    //goal = new MapLocation(rng.nextInt(rc.getMapWidth()),rng.nextInt(rc.getMapHeight()));
    pathfinding=new Pathfinding();
    captureManager = new CaptureManager();
    mapData = new MapData();
    explore = new Explore(mapData);
  }

  @Override
  public void run() throws GameActionException {
    mapData.checkAll();
    MapLocation ruin = mapData.getClosestRuin();
    if(ruin==null){
      goal = explore.getExploreTarget();
      //rc.setIndicatorString("Moving to exploration "+goal);
    }else{
      goal = ruin;
      //rc.setIndicatorString("Moving to ruin "+goal);
    }
    pathfinding.moveTo(goal);
  }
}