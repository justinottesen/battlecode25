package jottesen_test.util;

import battlecode.common.*;

public class Painter {

  private final MapData mapData;
  private final RobotController rc;

  final int ACTION_RADIUS_SQ;

  private final MapLocation[] paintCache;
  private MapLocation cacheLoc;

  public Painter(RobotController rc_, MapData mapData_) {
    rc = rc_;
    mapData = mapData_;
    paintCache = new MapLocation[GameConstants.PATTERN_SIZE * GameConstants.PATTERN_SIZE];

    ACTION_RADIUS_SQ = rc.getType().actionRadiusSquared;
  }

  /**
   * Handles the logic for painting (or attacking) a specific square
   * @param loc The location to paint
   * @return Whether we actually painted the space or not
   */
  public boolean paint(MapLocation loc) throws GameActionException {
    if (!rc.canPaint(loc) || !rc.canAttack(loc)) { return false; }
    // Only paint if it isn't the color we already want
    boolean useSecondary = mapData.useSecondaryPaint(loc);
    PaintType current = rc.senseMapInfo(loc).getPaint();
    if (!current.isAlly() || current.isSecondary() != useSecondary) {
      rc.attack(loc, useSecondary);
      return true;
    }
    return false;
  }

  /**
   * Takes care of robot painting logic when no specific target is in mind
   * 
   * TODO: Make this not suck
   * 
   * @return Whether we actually painted or not
   */
  public boolean paint() throws GameActionException {
    if (!rc.isActionReady()) { return false; }
    
    // Paint under self
    if (paint(rc.getLocation())) {
      return true;
    }

    // Try to attack someone
    RobotInfo[] robots = rc.senseNearbyRobots(ACTION_RADIUS_SQ, rc.getTeam().opponent());
    for (RobotInfo robot : robots) {
      if (rc.canAttack(robot.getLocation()) && paint(robot.getLocation())) {
        return true;
      }
    }

    // Paint elsewhere
    MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), ACTION_RADIUS_SQ);
    for (MapLocation loc : locs) {
      if (paint(loc)) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Handles the logic for fighting (painting) an enemy
   * @param enemy The enemy to target
   * @param pathfinding The class to help with movement
   * @throws GameActionException
   */
  public void fight(RobotInfo enemy, Pathfinding pathfinding) throws GameActionException {
    MapLocation enemyLoc = enemy.getLocation();
    int distance_sq = rc.getLocation().distanceSquaredTo(enemyLoc);
    int enemy_range_sq = enemy.getType().actionRadiusSquared;

    // If we can't attack move in
    if (distance_sq > ACTION_RADIUS_SQ && rc.isMovementReady()) {
      Direction moveIn = pathfinding.getGreedyMove(rc.getLocation(), enemyLoc, true, true);
      // But only move in range of enemy if we are ready to attack
      if (moveIn != null && rc.canMove(moveIn) && (rc.getLocation().add(moveIn).distanceSquaredTo(enemyLoc) > enemy_range_sq || rc.isActionReady())) {
        rc.move(moveIn);
      }
    }

    // Attack enemy
    if (rc.canAttack(enemyLoc)) { paint(enemyLoc); }

    // If enemy can see us, back up
    if (distance_sq <= enemy_range_sq && rc.isMovementReady()) {
      Direction backup = pathfinding.getGreedyMove(rc.getLocation(), enemyLoc.directionTo(rc.getLocation()), true, true);
      if (backup != null && rc.canMove(backup)) { rc.move(backup); }
    }

    // Whatever square we end on, try to paint it
    if (rc.canPaint(rc.getLocation())) { paint(rc.getLocation()); }
  }

  /**
   * Handles the logic for capturing a ruin
   * @param pathfinding The class to help with movement, with the ruin set as the target
   * @throws GameActionException
   */
  public void capture(Pathfinding pathfinding) throws GameActionException {
    MapLocation current = rc.getLocation();
    int low_x = pathfinding.getTarget().x - (GameConstants.PATTERN_SIZE / 2);
    int low_y = pathfinding.getTarget().y - (GameConstants.PATTERN_SIZE / 2);
    
    // Check the cache to see if we are capturing same ruin
    if (!pathfinding.getTarget().equals(cacheLoc)) {
      // If not, build the cache
      cacheLoc = pathfinding.getTarget();
      // TODO: UNROLL THESE LOOPS TO SAVE BYTECODE?
      for (int x_offset = 0; x_offset < GameConstants.PATTERN_SIZE; ++x_offset) {
        for (int y_offset = 0; y_offset < GameConstants.PATTERN_SIZE; ++y_offset) {
          paintCache[x_offset * GameConstants.PATTERN_SIZE + y_offset] = new MapLocation(low_x + x_offset, low_y + y_offset);
        }
      }
    }

    // If we are standing in the ruin, prioritize paint under our feet
    int my_x_offset = current.x - low_x;
    int my_y_offset = current.y - low_y;
    if (my_x_offset >= 0 && my_x_offset < GameConstants.PATTERN_SIZE &&
        my_y_offset >= 0 && my_y_offset < GameConstants.PATTERN_SIZE && paint(current)) {
      return;
    }
  
    // Try painting the rest of the ruin
    if (rc.isActionReady()) {
      for (MapLocation loc : paintCache) {
        if (paint(loc)) { break; }
        // If we couldn't reach it, move towards it and try again
        if (rc.isMovementReady() && current.distanceSquaredTo(loc) > ACTION_RADIUS_SQ) {
          Direction dir = pathfinding.getGreedyMove(current, loc, true, !rc.isActionReady());
          if (dir == null || rc.canMove(dir)) { continue; }
          rc.move(dir);
          current = rc.getLocation();
          // Check if there is paint under our feet
          if (paint(current)) { break; }
          // Try to paint again
          if (paint(loc)) { break; }
        }
      }
    }
      
  }

}
