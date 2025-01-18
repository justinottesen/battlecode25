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
  private MapLocation completedRuinJob;
  
  public Mopper(RobotController rc_) throws GameActionException {
    super(rc_);

    pathfinding = new Pathfinding(rc, mapData);
    painter = new Painter(rc, mapData);
    goal = Goal.EXPLORE;
    pathfinding.setTarget(mapData.getExploreTarget());
    completedRuinJob = null;
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

    // If low on paint, set goal to refill
    if (goal != Goal.REFILL_PAINT && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = Goal.REFILL_PAINT;
      pathfinding.setTarget(mapData.closestFriendlyTower());
    }

    // Look for nearby ruins if we aren't already refilling paint
    if (goal.val < Goal.REFILL_PAINT.val) {
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        if(ruin.equals(completedRuinJob)) continue; //completedRuinJob is the last ruin that we know we don't need a mopper for 
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
          if(rc.senseRobotAtLocation(pathfinding.getTarget())==null){
            //ruin has no more enemy paint around it, start exploring
            completedRuinJob = pathfinding.getTarget();
            goal = Goal.EXPLORE;
            pathfinding.setTarget(mapData.getExploreTarget());
          }else{
            //mopper built the tower, refill from the tower
            goal = Goal.REFILL_PAINT;
            pathfinding.setTarget(mapData.closestFriendlyTower());
          }
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

    //mop swing
    if (rc.isActionReady()){
      Direction mopSwingDirection = pickMopSwingDirection();
      if(mopSwingDirection!=Direction.CENTER && rc.canMopSwing(mopSwingDirection)) rc.mopSwing(mopSwingDirection);
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

  private Direction pickMopSwingDirection() throws GameActionException{
    RobotInfo[] enemyRobotsWithinMopSwingRange = rc.senseNearbyRobots(8,rc.getTeam().opponent());
    if(enemyRobotsWithinMopSwingRange.length==0)  return Direction.CENTER;
    Direction[] cardinaDirections = Direction.cardinalDirections();

    int mostEnemiesHit = 0;
    Direction chosenDirection = Direction.CENTER;
    for(Direction d : cardinaDirections){
      int directionEnemiesHit = 0;
      for(RobotInfo enemy : enemyRobotsWithinMopSwingRange){
        Direction dirToEnemy = rc.getLocation().directionTo(enemy.getLocation());
        if(dirToEnemy.dx==d.dx || dirToEnemy.dy==d.dy) ++directionEnemiesHit;
      }
      if(directionEnemiesHit>mostEnemiesHit){
        mostEnemiesHit=directionEnemiesHit;
        chosenDirection=d;
      }
    }
    return chosenDirection;
  }
}