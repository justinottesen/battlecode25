package jottesen_test;

import battlecode.common.*;
import jottesen_test.util.*;

public final class Mopper extends Robot {

  // Utility Classes
  private final Pathfinding pathfinding;
  private final Painter painter;

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD / 2;

  // Possible goal values, ordered by priority number (higher is more important)
  private enum Goal {
    EXPLORE(0),
    CAPTURE_RUIN(1),
    REFILL_PAINT(2);

    public final int val;

    Goal(int val_) {
      val = val_;
    }
  }
  private Goal goal;
  
  public Mopper(RobotController rc_) throws GameActionException {
    super(rc_);

    pathfinding = new Pathfinding(rc, mapData);
    painter = new Painter(rc, mapData);
    goal = Goal.EXPLORE;
    pathfinding.setTarget(mapData.MAP_CENTER);
  }

  protected void doMicro() throws GameActionException {
    rc.setIndicatorString("GOAL - " + switch (goal) {
      case EXPLORE -> "EXPLORE"; 
      case CAPTURE_RUIN -> "CAPTURE_RUIN";
      case REFILL_PAINT -> "REFILL_PAINT";
    });
    if (pathfinding.getTarget() != null) {
      rc.setIndicatorLine(rc.getLocation(), pathfinding.getTarget(), 255, 0, 255);
    }

    // UPDATE GOAL ------------------------------------------------------------

    // Check if someone else finished the current ruin
    if (goal == Goal.CAPTURE_RUIN) {
      if (rc.canSenseRobotAtLocation(pathfinding.getTarget())) {
        goal = Goal.REFILL_PAINT;
        pathfinding.setTarget(mapData.closestFriendlyTower());
      }
    }

    // If low on paint, set goal to refill
    if (goal != Goal.REFILL_PAINT && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = Goal.REFILL_PAINT;
      pathfinding.setTarget(mapData.closestFriendlyTower());
    }

    // Look for nearby ruins if we aren't already refilling paint
    if (goal.val < Goal.REFILL_PAINT.val) {
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        RobotInfo info = rc.senseRobotAtLocation(ruin);
        if (info == null) { // Unclaimed Ruin
          if (goal.val >= Goal.CAPTURE_RUIN.val) { continue; }
          goal = Goal.CAPTURE_RUIN;
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
          goal = Goal.REFILL_PAINT;
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
            goal = Goal.EXPLORE;
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