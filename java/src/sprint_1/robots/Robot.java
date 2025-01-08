package sprint_1.robots;

import java.util.Map;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import sprint_1.utils.MapData;

public abstract class Robot {

  public static RobotController rc;
  public static MapData mapData;

  public Robot(RobotController rc_) {
    rc = rc_;
    mapData = new MapData();
  };

  public void run() throws GameActionException{
    mapData.checkAll();
  }
}