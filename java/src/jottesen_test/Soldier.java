package jottesen_test;

import battlecode.common.*;
import jottesen_test.util.*;

public final class Soldier extends Robot {

  // Utility Classes
  private final Pathfinding pathfinding;
  private final Painter painter;

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD / 2;

  // Possible goal values, ordered by priority number (higher is more important)
  public enum Goal {
    EXPLORE(0),
    CAPTURE_RUIN(1),
    FIGHT_TOWER(2),
    REFILL_PAINT(3);
    
    public final int val;
    
    Goal(int val_) {
      val = val_;
    }
    
  }
  private Goal goal;

  // Other goal helpers
  RobotInfo goalTower;

  public Soldier(RobotController rc_) throws GameActionException {
    super(rc_);
    
    painter = new Painter(rc, mapData);
    pathfinding = new Pathfinding(rc, mapData);
    goal = Goal.EXPLORE;
    pathfinding.setTarget(mapData.getExploreTarget());
  }

  protected void doMicro() throws GameActionException {
    rc.setIndicatorString("GOAL - " + switch (goal) {
      case Goal.EXPLORE -> "EXPLORE"; 
      case Goal.CAPTURE_RUIN -> "CAPTURE_RUIN"; 
      case Goal.FIGHT_TOWER -> "FIGHT_TOWER"; 
      case Goal.REFILL_PAINT -> "REFILL_PAINT";
    });
    if (pathfinding.getTarget() != null) {
      rc.setIndicatorLine(rc.getLocation(), pathfinding.getTarget(), 255, 0, 255);
    }

    // UPDATE GOAL ------------------------------------------------------------

    // Update any close ruins sites (skip the first few rounds to save bytecode)
    if (rc.getRoundNum() > 10) {
      for (MapLocation ruin : rc.senseNearbyRuins(-1)) {
        mapData.updateData(rc.senseMapInfo(ruin));
      }
    }

    // Check for a suicide message, if received this is priority number 1
    Message[] messages = rc.readMessages(rc.getRoundNum() - 1);
    for (Message m : messages) {
      if (comms.getMessageType(m.getBytes()) == comms.SUICIDE) {
        System.out.println("Received suicide message");
        goal = Goal.CAPTURE_RUIN;
        pathfinding.setTarget(comms.getCoordinates(m.getBytes()));
        painter.paintCapture(pathfinding);
      }
    }

    // Check if someone else finished the current ruin
    if (goal == Goal.CAPTURE_RUIN) {
      if (rc.canSenseRobotAtLocation(pathfinding.getTarget())) {
        goal = Goal.REFILL_PAINT;
        pathfinding.setTarget(mapData.closestFriendlyTower());
      }
    }

    // If received paint transfer from mopper, update goal
    if (goal == Goal.REFILL_PAINT && rc.getPaint() > REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = Goal.EXPLORE;
      pathfinding.setTarget(mapData.getExploreTarget());
    }
    
    // If low on paint, set goal to refill
    if (goal != Goal.REFILL_PAINT && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = Goal.REFILL_PAINT;
      pathfinding.setTarget(mapData.closestFriendlyTower());
    }

    // Look for nearby ruins if we aren't already fighting a tower
    if (goal.val < Goal.FIGHT_TOWER.val) {
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        RobotInfo info = rc.senseRobotAtLocation(ruin);
        if (info == null) { // Unclaimed Ruin
          if (goal.val >= Goal.CAPTURE_RUIN.val) { continue; }
          goal = Goal.CAPTURE_RUIN;
          pathfinding.setTarget(ruin);
          continue;
        }
        if (info.getTeam() == OPPONENT) { // Enemy Tower
          goal = Goal.FIGHT_TOWER;
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
          goal = Goal.REFILL_PAINT;
          pathfinding.setTarget(mapData.closestFriendlyTower());
        }
        break;
      case REFILL_PAINT:
        if (rc.getLocation().isWithinDistanceSquared(pathfinding.getTarget(), GameConstants.PAINT_TRANSFER_RADIUS_SQUARED)) {
          RobotInfo tower = rc.senseRobotAtLocation(pathfinding.getTarget());
          if (tower == null) {
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, pathfinding.getTarget())) {
              rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, pathfinding.getTarget());
              tower = rc.senseRobotAtLocation(pathfinding.getTarget());
            } else {
              pathfinding.setTarget(mapData.closestFriendlyTower());
              return;
            }
          }
          int paintAmount = rc.getType().paintCapacity - rc.getPaint();
          if (tower.getPaintAmount() < paintAmount) { paintAmount = tower.getPaintAmount(); }
          if (rc.canTransferPaint(pathfinding.getTarget(), -paintAmount)) {
            rc.transferPaint(pathfinding.getTarget(), -paintAmount);
            goal = Goal.EXPLORE;
            pathfinding.setTarget(mapData.getExploreTarget());
          }
        }
        break;
      case EXPLORE:
        if (rc.getLocation().isWithinDistanceSquared(pathfinding.getTarget(), GameConstants.VISION_RADIUS_SQUARED)) {
          mapData.updateData(rc.senseMapInfo(pathfinding.getTarget()));
          pathfinding.setTarget(mapData.getExploreTarget());
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
