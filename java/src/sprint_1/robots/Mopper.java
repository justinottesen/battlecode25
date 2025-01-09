package sprint_1.robots;

import battlecode.common.*;
import battlecode.schema.RobotType;
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

    // Transfer paint if possible
    if (rc.isActionReady()) {
      RobotInfo[] robotInfos = rc.senseNearbyRobots(GameConstants.PAINT_TRANSFER_RADIUS_SQUARED);
      for (RobotInfo robot : robotInfos) {
        // Don't transfer to other moppers or towers
        if (robot.type == UnitType.MOPPER || robot.getType().isTowerType()) { continue; }

        // Transfer the max possible
        int transfer_amt = Math.min(robot.getType().paintCapacity - robot.getPaintAmount(), rc.getPaint());
        if (rc.canTransferPaint(robot.getLocation(), transfer_amt)) {
          rc.transferPaint(robot.getLocation(), transfer_amt);
          break;
        }
      }
    }
  }

};