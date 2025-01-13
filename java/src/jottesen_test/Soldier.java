package jottesen_test;

import jottesen_test.util.*;
import battlecode.common.*;

public class Soldier extends Robot {

  private MapLocation currentRuin;
  private final MapData mapdata;
  private final Pathfinding pathfinding;

  boolean touchedCenter = false;

  public Soldier(RobotController rc_) throws GameActionException {
    super(rc_);

    mapdata = new MapData(rc);
    mapdata.updateAllVisible();
    pathfinding = new Pathfinding(rc, mapdata);
  }

  @Override
  public void run() throws GameActionException {
    if (currentRuin == null) {
      currentRuin = mapdata.closestUnclaimedRuin();
      if (currentRuin != null) {
        System.out.println(currentRuin.toString());
      }
    }
    if (rc.isMovementReady()) {
      Direction dir = pathfinding.getMove(currentRuin == null ? MAP_CENTER : currentRuin);
      if (dir == null) {
        System.out.println("Pathfinding returned null dir");
        return;
      }
      if (rc.canMove(dir)) {
        rc.move(dir);
        mapdata.updateNewlyVisible(dir);
        if (rc.getLocation().equals(MAP_CENTER)) {
          touchedCenter = true;
        }
      }
    }
  }
}
