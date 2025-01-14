package jottesen_test;

import battlecode.common.*;
import jottesen_test.util.*;

public final class Soldier extends Robot {

  // Utility Classes
  private final Pathfinding pathfinding;
  private final Painter painter;

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD / 2;

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
  }

  protected void doMicro() throws GameActionException {
    rc.setIndicatorString("GOAL - " + switch (goal) {
      case IDLE -> "IDLE"; 
      case CAPTURE_RUIN -> "CAPTURE_RUIN"; 
      case FIGHT_TOWER -> "FIGHT_TOWER"; 
      case REFILL_PAINT -> "REFILL_PAINT";
      default -> "UNKNOWN";
    });
    if (pathfinding.getTarget() != null) {
      rc.setIndicatorLine(rc.getLocation(), pathfinding.getTarget(), 255, 0, 255);
    }

    // UPDATE GOAL ------------------------------------------------------------

    // If received paint transfer from mopper, update goal
    if (goal == REFILL_PAINT && rc.getPaint() > REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = IDLE;
      pathfinding.setTarget(mapData.MAP_CENTER);
    }
    
    // If low on paint, set goal to refill
    if (goal != REFILL_PAINT && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = REFILL_PAINT;
      pathfinding.setTarget(mapData.closestFriendlyTower());
    }

    // Look for nearby ruins if we aren't already fighting a tower
    if (goal < FIGHT_TOWER) {
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        RobotInfo info = rc.senseRobotAtLocation(ruin);
        if (info == null) { // Unclaimed Ruin
          if (goal >= CAPTURE_RUIN) { continue; }
          goal = CAPTURE_RUIN;
          pathfinding.setTarget(ruin);
          continue;
        }
        if (info.getTeam() == OPPONENT) { // Enemy Tower
          goal = FIGHT_TOWER;
          goalTower = info;
          pathfinding.setTarget(ruin);
          break;
        }
      }
    }

    // DO THINGS --------------------------------------------------------------

    // Can't do anything, no point
    if (!rc.isMovementReady() && !rc.isActionReady()) { return; }

    // Can't move, might as well try and paint
    if (!rc.isMovementReady() && rc.isActionReady()) { painter.paint(); return; }

    switch (goal) {
      case FIGHT_TOWER:
        painter.paintFight(goalTower, pathfinding);
        break;
      case CAPTURE_RUIN:
        if (painter.paintCapture(pathfinding)) {
          goal = REFILL_PAINT;
          pathfinding.setTarget(mapData.closestFriendlyTower());
        }
        break;
      case REFILL_PAINT:
        if (rc.getLocation().isWithinDistanceSquared(pathfinding.getTarget(), GameConstants.PAINT_TRANSFER_RADIUS_SQUARED)) {
          RobotInfo tower = rc.senseRobotAtLocation(pathfinding.getTarget());
          if (tower == null) {
            pathfinding.setTarget(mapData.closestFriendlyTower());
            return;
          }
          int paintAmount = rc.getType().paintCapacity - rc.getPaint();
          if (tower.getPaintAmount() < paintAmount) { paintAmount = tower.getPaintAmount(); }
          if (rc.canTransferPaint(pathfinding.getTarget(), -paintAmount)) {
            rc.transferPaint(pathfinding.getTarget(), -paintAmount);
            goal = IDLE;
            pathfinding.setTarget(mapData.MAP_CENTER);
          }
        }
        break;
      default: break;
    }
  }

  protected void doMacro() throws GameActionException {
    if (rc.isMovementReady()) {
      Direction dir = pathfinding.getMove();
      if (dir == null) {
        System.out.println("Pathfinding returned null dir");
      } else if (rc.canMove(dir)) {
        mapData.move(dir);
      }
    }
    painter.paint();
  }
}
