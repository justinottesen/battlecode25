package sprint_1.robots;

import battlecode.common.*;
import sprint_1.managers.MicroManager;

public class Mopper extends Robot {

  MicroManager microManager;

  public Mopper(RobotController rc) {
    super(rc);

    microManager = new MicroManager(rc);
  }

  @Override
  public void run() throws GameActionException {
    microManager.doMicro();

    // Mop tiles if possible
    if (rc.isActionReady()) {
      MapInfo[] mapInfos = rc.senseNearbyMapInfos(GameConstants.MARK_RADIUS_SQUARED); 
      for (MapInfo info : mapInfos) {
        if (info.getPaint() == PaintType.ENEMY_PRIMARY || info.getPaint() == PaintType.ENEMY_SECONDARY) {
          rc.attack(info.getMapLocation());
          break;
        }
      }
    }
  }

};