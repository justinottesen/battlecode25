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
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        currentRuin = ruin; break;
      }
    }
    rc.setIndicatorDot(MAP_CENTER, 255, 255, 255);
    if (rc.isMovementReady()) {
      Direction dir = pathfinding.getMove(touchedCenter ? currentRuin : MAP_CENTER);
      if (dir == null) {
        System.out.println("Pathfinding returned null dir");
        return;
      }
      if (rc.canMove(dir)) {
        rc.move(dir);
        mapdata.updateAllVisible();
        if (rc.getLocation().equals(MAP_CENTER)) {
          touchedCenter = true;
        }
      }
    }
  }
}
