package sprint_2;

import battlecode.common.*;
import sprint_2.util.*;

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
  }

  protected void doMicro() throws GameActionException {
    rc.setIndicatorString("GOAL - " + GoalManager.current());
    if (Pathfinding.getTarget() != null) {
      rc.setIndicatorLine(rc.getLocation(), Pathfinding.getTarget(), 255, 0, 255);
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
      if (rc.canSenseRobotAtLocation(Pathfinding.getTarget())) {
        MapData.updateData(rc.senseMapInfo(Pathfinding.getTarget()));
        GoalManager.setNewGoal(Goal.Type.REFILL_PAINT, Pathfinding.getTarget());
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
          if(rc.senseRobotAtLocation(Pathfinding.getTarget())==null){
            //ruin has no more enemy paint around it, start exploring
            completedRuinJob = Pathfinding.getTarget();
            GoalManager.popGoal();
          }else{
            //mopper built the tower, refill from the tower
            GoalManager.replaceTopGoal(Goal.Type.REFILL_PAINT, Pathfinding.getTarget());
          }
        }
        break;
      case CAPTURE_SRP:
        if (Painter.mopCaptureSRP()) {
          completedRuinJob = Pathfinding.getTarget();
          GoalManager.popGoal();
        }
        break;
      case REFILL_PAINT:
        if (rc.getLocation().isWithinDistanceSquared(Pathfinding.getTarget(), GameConstants.PAINT_TRANSFER_RADIUS_SQUARED)) {
          RobotInfo tower = rc.senseRobotAtLocation(Pathfinding.getTarget());
          if (tower == null) {
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, Pathfinding.getTarget())) {
              rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, Pathfinding.getTarget());
              tower = rc.senseRobotAtLocation(Pathfinding.getTarget());
            } else {
              GoalManager.replaceTopGoal(Goal.Type.REFILL_PAINT, MapData.closestFriendlyTower());
              return;
            }
          }
          int paintAmount = rc.getType().paintCapacity - rc.getPaint();
          if (tower.getPaintAmount() < paintAmount) { paintAmount = tower.getPaintAmount(); }
          if (rc.canTransferPaint(Pathfinding.getTarget(), -paintAmount)) {
            rc.transferPaint(Pathfinding.getTarget(), -paintAmount);
            GoalManager.popGoal();
          }
        }
        break;
      case EXPLORE:
        if (Pathfinding.getTarget()==null || rc.getLocation().isWithinDistanceSquared(Pathfinding.getTarget(), GameConstants.VISION_RADIUS_SQUARED)) {
          //MapData.updateData(rc.senseMapInfo(Pathfinding.getTarget()));
          Pathfinding.setTarget(MapData.getExploreTarget());
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
      Direction dir = Pathfinding.getMove(Pathfinding.Mode.NO_ENEMY);
      if (dir == null) {
        System.out.println("Pathfinding returned null dir");
      } else if (rc.canMove(dir)) {
        MapData.move(dir);
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