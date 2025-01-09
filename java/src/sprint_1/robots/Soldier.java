package sprint_1.robots;

//import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import sprint_1.utils.Pathfinding;
import sprint_1.managers.CaptureManager;
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
    if(moneyTowerRequest!=null){

    }else if(ruin==null){
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