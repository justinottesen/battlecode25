package jottesen_test;

import battlecode.common.*;
import jottesen_test.util.*;

public final class Soldier extends Robot {

  // Utility Classes
  private final Pathfinding pathfinding;
  private final Painter painter;

  // Member Data
  private int goal;

  // Possible goal values, ordered by priority number (higher is more important)
  private final int IDLE = 0;
  private final int CAPTURE_RUIN = 1;
  private final int FIGHT_TOWER = 2;
  private final int REFILL_PAINT = 3;
  private RobotInfo goalTower;

  public Soldier(RobotController rc_) throws GameActionException {
    super(rc_);
    
    painter = new Painter(rc, mapData);
    pathfinding = new Pathfinding(rc, mapData);
    goal = IDLE;
    pathfinding.setTarget(mapData.MAP_CENTER);
    rc.setIndicatorString("GOAL - IDLE");
  }

  protected void doMicro() throws GameActionException {
    // Can't do anything, no point
    if (!rc.isMovementReady() && !rc.isActionReady()) { return; }

    // Can't move, might as well try and paint
    if (!rc.isMovementReady() && rc.isActionReady()) { painter.paint(); return; }

    // Look for nearby ruins if we aren't already fighting a tower
    if (goal < FIGHT_TOWER) {
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        RobotInfo info = rc.senseRobotAtLocation(ruin);
        if (info == null) { // Unclaimed Ruin
          if (goal >= CAPTURE_RUIN) { continue; }
          rc.setIndicatorString("GOAL - CAPTURE_RUIN: " + ruin.toString());
          goal = CAPTURE_RUIN;
          pathfinding.setTarget(ruin);
          continue;
        }
        if (info.getTeam() == OPPONENT) { // Enemy Tower
          rc.setIndicatorString("GOAL - FIGHT_TOWER " + ruin.toString());
          goal = FIGHT_TOWER;
          goalTower = info;
          pathfinding.setTarget(ruin);
          break;
        }
      }
    }

    // Fight tower
    if (goal == FIGHT_TOWER) {
      painter.fight(goalTower, pathfinding);
    } else if (goal == CAPTURE_RUIN) {
      painter.capture(pathfinding);
    }

    return;
  }

  protected void doMacro() throws GameActionException {
    if (rc.isMovementReady()) {
      Direction dir = pathfinding.getMove();
      if (dir == null) {
        System.out.println("Pathfinding returned null dir");
      } else if (rc.canMove(dir)) {
        rc.move(dir);
        mapData.updateNewlyVisible(dir); // TODO: Make a `move` method which groups this with the rc.move call
      }
    }
    painter.paint();
  }
}
