package sprint_2;

import battlecode.common.*;
import sprint_2.util.*;

public final class Soldier extends Robot {

  // Utility Classes
  private final Pathfinding pathfinding;
  private final Painter painter;

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD;

  // Possible goal values, ordered by priority number (higher is more important)
  public enum Goal {
    EXPLORE(0),
    CAPTURE_SRP(1),
    CAPTURE_RUIN(2),
    FIGHT_TOWER(3),
    REFILL_PAINT(4);
    
    public final int val;
    
    Goal(int val_) {
      val = val_;
    }
    
  }
  private Goal goal;
  private boolean initialSoldiers; //true if this soldier was the 1st or 2nd soldier spawned from the starting paint/money towers

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
      case Goal.CAPTURE_SRP -> "CAPTURE_SRP";
      case Goal.CAPTURE_RUIN -> "CAPTURE_RUIN"; 
      case Goal.FIGHT_TOWER -> "FIGHT_TOWER"; 
      case Goal.REFILL_PAINT -> "REFILL_PAINT";
    });
    if (pathfinding.getTarget() != null) {
      rc.setIndicatorLine(rc.getLocation(), pathfinding.getTarget(), 255, 0, 255);
    }

    // TODO: TAKE THIS OUT AND DEAL WITH CROWDING lol
    // If surrounded, kill yourself
    if (rc.senseNearbyRobots(9, TEAM).length > 24 /* Out of 28 */) {
      rc.disintegrate();
      return;
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
        painter.paintCaptureRuin(pathfinding);
      }
    }

    // Check if someone else finished the current ruin
    // TODO: Should this check stay here? Duplicated in painter
    if (goal == Goal.CAPTURE_RUIN) {
      if (rc.canSenseRobotAtLocation(pathfinding.getTarget())) {
        mapData.updateData(rc.senseMapInfo(pathfinding.getTarget()));
        goal = Goal.REFILL_PAINT;
        pathfinding.setTarget(mapData.closestFriendlyTower());
      }
    }

    // Check if someone else finished the current SRP
    // TODO: Should this check stay here? Duplicated in painter
    if (goal == Goal.CAPTURE_SRP) {
      MapLocation target = pathfinding.getTarget();
      if (rc.canSenseLocation(target) && rc.senseMapInfo(target).isResourcePatternCenter()) {
        mapData.updateData(rc.senseMapInfo(target));
        goal = Goal.EXPLORE;
        pathfinding.setTarget(mapData.getExploreTarget());
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

    // Look for SRP if we are a lower priority
    // TODO: Make this process more intelligent. Pack better with other SRPs, etc
    if (goal.val < Goal.CAPTURE_SRP.val) {
      for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), GameConstants.MARK_RADIUS_SQUARED)) {
        if (mapData.tryMarkSRP(loc)) {
          goal = Goal.CAPTURE_SRP;
          pathfinding.setTarget(rc.getLocation());
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
      case CAPTURE_SRP:
        if (painter.paintCaptureSRP(pathfinding)) {
          goal = Goal.EXPLORE;
          pathfinding.setTarget(mapData.getExploreTarget());
        }
        break;
      case CAPTURE_RUIN:
        if (painter.paintCaptureRuin(pathfinding)) {
          goal = Goal.REFILL_PAINT;
        }
        // Pathfinding target is the tower which was just built, should have paint
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
      case EXPLORE: // TODO: Address clumping of units
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
        if (mapData.foundSRP != null && goal.val < Goal.CAPTURE_SRP.val) {
          goal = Goal.CAPTURE_SRP;
          pathfinding.setTarget(mapData.foundSRP);
        }
      }
    }
    painter.paint();
  }
}
