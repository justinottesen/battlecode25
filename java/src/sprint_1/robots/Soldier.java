package sprint_1.robots;

//import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import sprint_1.utils.Pathfinding;
import sprint_1.managers.CaptureManager;
import sprint_1.managers.PaintManager;
import sprint_1.utils.Comm;
import sprint_1.utils.Explore;
//import sprint_1.utils.MapData;

public class Soldier extends Robot {
  MapLocation goal;
  Pathfinding pathfinding;
  CaptureManager captureManager;
  Explore explore;



  public Soldier(RobotController rc) {
    super(rc);
    //goal = new MapLocation(rng.nextInt(rc.getMapWidth()),rng.nextInt(rc.getMapHeight()));
    pathfinding=new Pathfinding();
    captureManager = new CaptureManager();
    explore = new Explore();
  }

  @Override
  public void run() throws GameActionException {
    super.run();  //updates mapdata here

    MapLocation ruin = mapData.getClosestRuin();
    MapLocation moneyTowerRequest = Comm.receiveMoneyTowerRequest();
    if(moneyTowerRequest!=null) captureManager.setRebuildLocation(moneyTowerRequest);
    if(captureManager.getRebuildLocation()!=null){
      captureManager.rebuildTower();
      Robot.rc.setIndicatorString("Attempting to rebuild tower at "+captureManager.getRebuildLocation());
    }else if(Robot.rc.getPaint()<PaintManager.PAINT_THRESHOLD){   //refill if we need it
      MapLocation homeTower = mapData.getClosestTower();
      if(homeTower!=null){
        PaintManager.refill(homeTower);
        if(Robot.rc.getLocation().distanceSquaredTo(homeTower)>2){
            pathfinding.moveTo(homeTower);
        }
      }
    }else if(ruin==null || (captureManager.isInIgnoreList(ruin)&&captureManager.betterBuilderAvailable(ruin))){
      goal = explore.getExploreTarget();
      pathfinding.moveTo(goal);
      //rc.setIndicatorString("Moving to exploration "+goal);
      if(Robot.rc.senseMapInfo(Robot.rc.getLocation()).getPaint()==PaintType.EMPTY && Robot.rc.canAttack(Robot.rc.getLocation())){
        Robot.rc.attack(Robot.rc.getLocation());
      }
    }else{
      //goal = ruin;
      captureManager.captureTower();
      //rc.setIndicatorString("Capturing ruin "+ruin);
    }

    //attack a tower if possible
    attackTower();
  }

  private void attackTower() throws GameActionException{
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    for(RobotInfo enemy : enemies){
      if(enemy.getType().isTowerType() && rc.canAttack(enemy.getLocation())){
        rc.attack(enemy.getLocation());
      }
    }
  }
}