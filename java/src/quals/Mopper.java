package quals;

import battlecode.common.*;
import quals.util.*;

public final class Mopper extends Robot {

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD / 2;

  private MapLocation completedRuinJob = null;
  
  public Mopper(RobotController rc_) throws GameActionException {
    super(rc_);

    // Read incoming messages
    for (Message m : rc.readMessages(-1)) {
      switch (m.getBytes() & Communication.MESSAGE_TYPE_BITMASK) {
        case Communication.REQUEST_MOPPER:
          GoalManager.replaceTopGoal(Goal.Type.CAPTURE_RUIN, Communication.getCoordinates(m.getBytes()));
          break;
        default:
          System.out.println("RECEIVED UNKNOWN MESSAGE: " + m);
      }
    }
    MovementManager.setMode(MovementManager.Mode.NO_ENEMY);
  }

  protected void doMicro() throws GameActionException {
    rc.setIndicatorString("GOAL - " + GoalManager.current());

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
        MapData.updateData(rc.senseMapInfo(ruin));
      }
    }

    // Check for a suicide message, if received this is priority number 1
    Message[] messages = rc.readMessages(rc.getRoundNum() - 1);
    for (Message m : messages) {
      if (Communication.getMessageType(m.getBytes()) == Communication.SUICIDE) {
        System.out.println("Received suicide message");
        GoalManager.pushGoal(Goal.Type.CAPTURE_RUIN, Communication.getCoordinates(m.getBytes()));
        Painter.mopCaptureRuin();
      }
    }

    // Check if someone else finished the current ruin
    if (GoalManager.current().type == Goal.Type.CAPTURE_RUIN) {
      if (rc.canSenseRobotAtLocation(GoalManager.current().target)) {
        MapData.updateData(rc.senseMapInfo(GoalManager.current().target));
        GoalManager.setNewGoal(Goal.Type.REFILL_PAINT, GoalManager.current().target);
      }
    }

    // If low on paint, set goal to refill
    // TODO: refill paint has bug, robots sometimes sit near tower with refill paint goal
    if (GoalManager.current().type != Goal.Type.REFILL_PAINT && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      GoalManager.pushGoal(Goal.Type.REFILL_PAINT, MapData.closestFriendlyTower());
    }

    // Look for nearby ruins if we aren't already refilling paint
    if (GoalManager.current().type.v < Goal.Type.REFILL_PAINT.v) {
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        if(ruin.equals(completedRuinJob)) continue; //completedRuinJob is the last ruin that we know we don't need a mopper for 
        RobotInfo info = rc.senseRobotAtLocation(ruin);
        if (info == null) { // Unclaimed Ruin
          if (GoalManager.current().type.v >= Goal.Type.CAPTURE_RUIN.v) { continue; }
          GoalManager.pushGoal(Goal.Type.CAPTURE_RUIN, ruin);
          break;
        }
      }
    }

    // Look for SRP if we are a lower priority
    if (GoalManager.current().type.v < Goal.Type.CAPTURE_SRP.v && MapData.foundSRP != null && !MapData.foundSRP.equals(completedRuinJob)) {
      GoalManager.pushGoal(Goal.Type.CAPTURE_SRP, MapData.foundSRP);
    }

    // DO THINGS --------------------------------------------------------------

    // Can't do anything, no point
    if (!rc.isMovementReady() && !rc.isActionReady()) { return; }

    // Can't move, might as well try and mop
    if (!rc.isMovementReady() && rc.isActionReady()) { Painter.mop(); return; }

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

    switch (GoalManager.current().type) {
      case CAPTURE_RUIN:
        if (Painter.mopCaptureRuin()) { 
          if(rc.senseRobotAtLocation(GoalManager.current().target)==null){
            //ruin has no more enemy paint around it, start exploring
            completedRuinJob = GoalManager.current().target;
            GoalManager.popGoal();
          }else{
            //mopper built the tower, refill from the tower
            GoalManager.replaceTopGoal(Goal.Type.REFILL_PAINT, GoalManager.current().target);
          }
        }
        break;
      case CAPTURE_SRP:
        if (Painter.mopCaptureSRP()) {
          completedRuinJob = GoalManager.current().target;
          GoalManager.popGoal();
        }
        break;
      case REFILL_PAINT:
        if (rc.getLocation().isWithinDistanceSquared(GoalManager.current().target, GameConstants.PAINT_TRANSFER_RADIUS_SQUARED)) {
          RobotInfo tower = rc.senseRobotAtLocation(GoalManager.current().target);
          if (tower == null) {
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, GoalManager.current().target)) {
              rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, GoalManager.current().target);
              tower = rc.senseRobotAtLocation(GoalManager.current().target);
            } else {
              GoalManager.replaceTopGoal(Goal.Type.REFILL_PAINT, MapData.closestFriendlyTower());
              return;
            }
          }
          int paintAmount = rc.getType().paintCapacity - rc.getPaint();
          if (tower.getPaintAmount() < paintAmount) { paintAmount = tower.getPaintAmount(); }
          if (rc.canTransferPaint(GoalManager.current().target, -paintAmount)) {
            rc.transferPaint(GoalManager.current().target, -paintAmount);
            GoalManager.popGoal();
          }
        }
        break;
      case EXPLORE:
        if (rc.getLocation().isWithinDistanceSquared(GoalManager.current().target, GameConstants.VISION_RADIUS_SQUARED)) {
          MapData.updateData(rc.senseMapInfo(GoalManager.current().target));
          GoalManager.replaceTopGoal(Goal.Type.EXPLORE,MapData.getExploreTarget());
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
    Pathfinding.moveTo(GoalManager.current().target); //note that Mopper is currently set to NO_ENEMY by the constructor, and is never changed
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