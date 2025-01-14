package jottesen_test;

import battlecode.common.*;
import jottesen_test.util.*;

public final class Mopper extends Robot {

  // Utility Classes
  private final Pathfinding pathfinding;
  private final Painter painter;

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD / 2;

  // Member data
  private int goal;

  // Possible goal values, ordered by priority number (higher is more important)
  private final int IDLE = 0;
  private final int CAPTURE_RUIN = 1;
  private final int REFILL_PAINT = 2;
  private RobotInfo goalTower;

  public Mopper(RobotController rc_) throws GameActionException {
    super(rc_);

    pathfinding = new Pathfinding(rc, mapData);
    painter = new Painter(rc, mapData);
    goal = IDLE;
    pathfinding.setTarget(mapData.MAP_CENTER);
  }

  protected void doMicro() throws GameActionException {
    rc.setIndicatorString("GOAL - " + switch (goal) {
      case IDLE -> "IDLE"; 
      case CAPTURE_RUIN -> "CAPTURE_RUIN";
      case REFILL_PAINT -> "REFILL_PAINT";
      default -> "UNKNOWN";
    });
    if (pathfinding.getTarget() != null) {
      rc.setIndicatorLine(rc.getLocation(), pathfinding.getTarget(), 255, 0, 255);
    }

    // UPDATE GOAL ------------------------------------------------------------

    // Check if someone else finished the current ruin
    if (goal == CAPTURE_RUIN) {
      if (rc.canSenseRobotAtLocation(pathfinding.getTarget())) {
        goal = REFILL_PAINT;
        pathfinding.setTarget(mapData.closestFriendlyTower());
      }
    }

    // If low on paint, set goal to refill
    if (goal != REFILL_PAINT && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = REFILL_PAINT;
      pathfinding.setTarget(mapData.closestFriendlyTower());
    }

    // Look for nearby ruins if we aren't already refilling paint
    if (goal < REFILL_PAINT) {
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        RobotInfo info = rc.senseRobotAtLocation(ruin);
        if (info == null) { // Unclaimed Ruin
          if (goal >= CAPTURE_RUIN) { continue; }
          goal = CAPTURE_RUIN;
          pathfinding.setTarget(ruin);
          break;
        }
      }
    }

    // DO THINGS --------------------------------------------------------------

    // Can't do anything, no point
    if (!rc.isMovementReady() && !rc.isActionReady()) { return; }

    // Can't move, might as well try and mop
    if (!rc.isMovementReady() && rc.isActionReady()) { painter.mop(); return; }

    // Transfer paint if possible
    if (rc.isActionReady() && rc.getPaint() > REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      for (RobotInfo robot : rc.senseNearbyRobots(GameConstants.PAINT_TRANSFER_RADIUS_SQUARED, rc.getTeam())) {
        // Don't transfer to other moppers or towers, or if they have enough paint
        if (robot.type == UnitType.MOPPER || robot.getType().isTowerType() || robot.getPaintAmount() > REFILL_PAINT_THRESHOLD) { continue; }

        // Transfer the max possible
        int transfer_amt = Math.min(robot.getType().paintCapacity - robot.getPaintAmount(), rc.getPaint() - REFILL_PAINT_THRESHOLD);
        if (rc.canTransferPaint(robot.getLocation(), transfer_amt)) {
          rc.transferPaint(robot.getLocation(), transfer_amt);
          break;
        }
      }
    }

    switch (goal) {
      case CAPTURE_RUIN:
        if (painter.mopCapture(pathfinding)) { 
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
      default: break;
    }
  }

  protected void doMacro() throws GameActionException {
    if (rc.isMovementReady()) {
      Direction dir = pathfinding.getMove(Pathfinding.Mode.NO_ENEMY);
      if (dir == null) {
        System.out.println("Pathfinding returned null dir");
      } else if (rc.canMove(dir)) {
        mapData.move(dir);
      }
    }
  }
}