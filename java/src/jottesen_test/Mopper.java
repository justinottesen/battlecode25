package jottesen_test;

import battlecode.common.*;
import battlecode.server.*;
import jottesen_test.util.*;

public final class Mopper extends Robot {

  // Utility Classes
  private final Pathfinding pathfinding;

  // Member data
  private int goal;

  // Possible goal values, ordered by priority number (higher is more important)
  private final int IDLE = 0;
  private final int REFILL_PAINT = 1;
  private RobotInfo goalTower;

  public Mopper(RobotController rc_) throws GameActionException {
    super(rc_);

    pathfinding = new Pathfinding(rc, mapData);
    goal = IDLE;
    pathfinding.setTarget(mapData.MAP_CENTER);
  }

  protected void doMicro() throws GameActionException {
    rc.setIndicatorString("GOAL - " + switch (goal) {
      case IDLE -> "IDLE"; 
      case REFILL_PAINT -> "REFILL_PAINT";
      default -> "UNKNOWN";
    });

    // Can't do anything, no point
    if (!rc.isMovementReady() && !rc.isActionReady()) { return; }

    // Look for nearby enemy paint 
    // TODO: Prioritize enemies
    if (rc.isActionReady()) {
      for (MapInfo info : rc.senseNearbyMapInfos(rc.getType().actionRadiusSquared)) {
        if (info.getPaint().isEnemy() && rc.canAttack(info.getMapLocation())) {
          rc.attack(info.getMapLocation());
          break;
        }
      }
    }

    // Transfer paint if possible
    if (rc.isActionReady() && rc.getPaint() > GameConstants.INCREASED_COOLDOWN_THRESHOLD) {
      for (RobotInfo robot : rc.senseNearbyRobots(GameConstants.PAINT_TRANSFER_RADIUS_SQUARED)) {
        // Don't transfer to other moppers or towers, or if they have enough paint
        if (robot.type == UnitType.MOPPER || robot.getType().isTowerType() || robot.getPaintAmount() > GameConstants.INCREASED_COOLDOWN_THRESHOLD) { continue; }

        // Transfer the max possible
        int transfer_amt = Math.min(robot.getType().paintCapacity - robot.getPaintAmount(), rc.getPaint() - GameConstants.INCREASED_COOLDOWN_THRESHOLD);
        if (rc.canTransferPaint(robot.getLocation(), transfer_amt)) {
          rc.transferPaint(robot.getLocation(), transfer_amt);
          break;
        }
      }
    }

    // If we find a tower while refilling paint, refill and set back idle
    if (goal == REFILL_PAINT) {
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
    } else {
      // Look for enemy paint to move towards (and attack potentially)
      if (rc.isMovementReady()) {
        for (MapInfo info : rc.senseNearbyMapInfos(GameConstants.VISION_RADIUS_SQUARED)) {
          if (info.getPaint().isEnemy() && !rc.getLocation().isWithinDistanceSquared(info.getMapLocation(), rc.getType().actionRadiusSquared)) {
            Direction dir = pathfinding.getGreedyMove(rc.getLocation(), info.getMapLocation(), true, Pathfinding.Mode.NO_ENEMY);
            if (dir != null) {
              mapData.move(dir);
              if (rc.canAttack(info.getMapLocation())) {
                rc.attack(info.getMapLocation());
              }
              break;
            }
          }
        }
      }

      // Look for friendly soldiers to move towards
      if (rc.isMovementReady()) {
        for (RobotInfo info : rc.senseNearbyRobots(-1, TEAM)) {
          if (info.type == UnitType.MOPPER || info.getType().isTowerType() || info.getPaintAmount() > GameConstants.INCREASED_COOLDOWN_THRESHOLD) { continue; }
          if (rc.getLocation().distanceSquaredTo(info.getLocation()) > GameConstants.PAINT_TRANSFER_RADIUS_SQUARED) {
            Direction dir = pathfinding.getGreedyMove(rc.getLocation(), info.getLocation(), true, Pathfinding.Mode.NO_ENEMY);
            if (dir != null) {
              mapData.move(dir);
              // Transfer the max possible
              int transfer_amt = Math.min(info.getType().paintCapacity - info.getPaintAmount(), rc.getPaint() - GameConstants.INCREASED_COOLDOWN_THRESHOLD);
              if (rc.canTransferPaint(info.getLocation(), transfer_amt)) {
                rc.transferPaint(info.getLocation(), transfer_amt);
                break;
              }
            }
          }
        }
      }
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