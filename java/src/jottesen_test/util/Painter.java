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
    boolean useSecondary = mapData.useSecondaryPaint(loc);
    if (!shouldPaint(loc, useSecondary)) { return false; }
    rc.attack(loc, useSecondary);
    return true;
  }

  /**
   * Checks whether a square should (and can) be painted with the given color.
   * @param loc The location to paint
   * @param useSecondary Whether to use secondary paint or not
   * @return Whether the square should be painted or not
   */
  public boolean shouldPaint(MapLocation loc, boolean useSecondary) throws GameActionException {
    if (!rc.canPaint(loc) || !rc.canAttack(loc)) { return false; }
    PaintType current = rc.senseMapInfo(loc).getPaint();
    return !current.isAlly() || current.isSecondary() != useSecondary;
  }

  /**
   * Checks whether a square should (and can) be painted
   * @param loc The location to paint
   * @return Whether the square should be painted or not
   */
  public boolean shouldPaint(MapLocation loc) throws GameActionException {
    boolean useSecondary = mapData.useSecondaryPaint(loc);
    return shouldPaint(loc, useSecondary);
  }

  /**
   * Handles the logic for mopping (or attacking) a specific square
   * @param loc The location to mop
   * @return Whether we actually mopped the space or not
   */
  public boolean mop(MapLocation loc) throws GameActionException {
    if (shouldMop(loc)) { rc.attack(loc); return true; }
    return false;
  }

  /**
   * Checks whether a square should (and can) be mopped.
   * @param loc The location to mop
   * @return Whether the square should be mopped or not
   */
  public boolean shouldMop(MapLocation loc) throws GameActionException {
    return rc.canAttack(loc) &&  // Can attack AND (
      (rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).getTeam() != rc.getTeam() || // Can sense enemy robot
      rc.senseMapInfo(loc).getPaint().isEnemy()); // OR enemy paint )
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
      if (paint(robot.getLocation())) {
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
   * Takes care of robot mopping logic when no specific target is in mind
   * 
   * TODO: Make this not suck
   * 
   * @return Whether we actually mopped or not
   */
  public boolean mop() throws GameActionException {
    if (!rc.isActionReady()) { return false; }

    // Mop under self (if enemy paint)
    if (mop(rc.getLocation())) {
      return true;
    }

    // Try to attack someone
    RobotInfo[] robots = rc.senseNearbyRobots(ACTION_RADIUS_SQ, rc.getTeam().opponent());
    for (RobotInfo robot : robots) {
      if (mop(robot.getLocation())) {
        return true;
      }
    }

    // Mop elsewhere
    MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), ACTION_RADIUS_SQ);
    for (MapLocation loc : locs) {
      if (mop(loc)) {
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
  public void paintFight(RobotInfo enemy, Pathfinding pathfinding) throws GameActionException {
    MapLocation enemyLoc = enemy.getLocation();
    int distance_sq = rc.getLocation().distanceSquaredTo(enemyLoc);
    int enemy_range_sq = enemy.getType().actionRadiusSquared;

    // If we can't attack move in
    if (distance_sq > ACTION_RADIUS_SQ && rc.isMovementReady()) {
      Direction moveIn = pathfinding.getGreedyMove(rc.getLocation(), enemyLoc, true, Pathfinding.Mode.NO_ENEMY);
      // But only move in range of enemy if we are ready to attack
      if (moveIn != null && rc.canMove(moveIn) && (rc.getLocation().add(moveIn).distanceSquaredTo(enemyLoc) > enemy_range_sq || rc.isActionReady())) {
        mapData.move(moveIn);
      }
    }

    // Attack enemy
    if (rc.canAttack(enemyLoc)) { paint(enemyLoc); }

    // If enemy can see us, back up
    if (distance_sq <= enemy_range_sq && rc.isMovementReady()) {
      Direction backup = pathfinding.getGreedyMove(rc.getLocation(), enemyLoc.directionTo(rc.getLocation()), true, Pathfinding.Mode.ANY);
      if (backup != null && rc.canMove(backup)) { mapData.move(backup); }
    }

    // Whatever square we end on, try to paint it
    if (rc.canPaint(rc.getLocation())) { paint(rc.getLocation()); }
  }

  /**
   * Handles the logic for fighting (mopping) an enemy
   * @param enemy The enemy to target
   * @param pathfinding The class to help with movement
   * @throws GameActionException
   */
  public void mopFight(RobotInfo enemy, Pathfinding pathfinding) throws GameActionException {
    MapLocation enemyLoc = enemy.getLocation();
    int distance_sq = rc.getLocation().distanceSquaredTo(enemyLoc);
    int enemy_range_sq = enemy.getType().actionRadiusSquared;

    // If we can't attack move in
    if (distance_sq > ACTION_RADIUS_SQ && rc.isMovementReady()) {
      Direction moveIn = pathfinding.getGreedyMove(rc.getLocation(), enemyLoc, true, Pathfinding.Mode.ALLY_ONLY);
      // But only move in range of enemy if we are ready to attack
      if (moveIn != null && rc.canMove(moveIn) && (rc.getLocation().add(moveIn).distanceSquaredTo(enemyLoc) > enemy_range_sq || rc.isActionReady())) {
        mapData.move(moveIn);
      }
    }

    // Attack enemy
    if (rc.canAttack(enemyLoc)) { mop(enemyLoc); }

    // If enemy can see us, back up
    if (distance_sq <= enemy_range_sq && rc.isMovementReady()) {
      Direction backup = pathfinding.getGreedyMove(rc.getLocation(), enemyLoc.directionTo(rc.getLocation()), true, Pathfinding.Mode.ANY);
      if (backup != null && rc.canMove(backup)) { mapData.move(backup); }
    }

    // Whatever square we end on, try to paint it
    if (rc.canAttack(rc.getLocation())) { mop(rc.getLocation()); }
  }

  /**
   * Handles the logic for capturing a ruin
   * @param pathfinding The class to help with movement, with the ruin set as the target
   * @return Whether the ruin was successfully captured
   * @throws GameActionException
   */
  public boolean paintCapture(Pathfinding pathfinding) throws GameActionException {
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
        my_y_offset >= 0 && my_y_offset < GameConstants.PATTERN_SIZE) {
      paint(current);
    }
  
    // Try painting the rest of the ruin
    if (rc.isActionReady()) {
      for (MapLocation loc : paintCache) {
        rc.setIndicatorDot(loc, 0, 0, 255);
        // Only interested in ally, empty, or unknown paint
        if (loc.equals(cacheLoc) || rc.canSenseLocation(loc) && (rc.senseMapInfo(loc).getPaint().isEnemy() || rc.senseMapInfo(loc).getPaint().isAlly() && rc.senseMapInfo(loc).getPaint().isSecondary() == mapData.useSecondaryPaint(loc))) { continue; }

        // If we can't reach it, move towards it
        rc.setIndicatorDot(loc, 0, 255, 0);
        if (rc.isMovementReady() && current.distanceSquaredTo(loc) > ACTION_RADIUS_SQ) {
          rc.setIndicatorDot(loc, 255, 0, 0);
          Direction dir = pathfinding.getGreedyMove(current, loc, true, rc.isActionReady() ? Pathfinding.Mode.ANY : Pathfinding.Mode.NO_ENEMY);
          if (dir == null || !rc.canMove(dir)) { continue; }
          rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dir), 255, 255, 255);
          mapData.move(dir);
          current = rc.getLocation();
          // Check if there is paint under our feet
          if (paint(current)) { break; }
        }
        if (!shouldPaint(loc)) { continue; }
        if (paint(loc)) { break; }
      }
    }

    // Try to complete the ruin
    if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, cacheLoc)) {
      rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, cacheLoc);
      mapData.updateData(rc.senseMapInfo(cacheLoc));
      cacheLoc = null;
      return true;
    }
      
    return false;
  }

  /**
   * Handles the logic for defending a ruin by cleaning enemy paint
   */
  public boolean mopCapture(Pathfinding pathfinding) throws GameActionException {
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
        my_y_offset >= 0 && my_y_offset < GameConstants.PATTERN_SIZE) {
      mop(current);
    }
  
    // Try cleaning the rest of the ruin
    if (rc.isActionReady()) {
      for (MapLocation loc : paintCache) {
        // Only interested in enemy (or unknown) paint
        if (loc.equals(cacheLoc) || rc.canSenseLocation(loc) && !rc.senseMapInfo(loc).getPaint().isEnemy()) { continue; }

        // If we can't reach it, move towards it
        if (rc.isMovementReady() && current.distanceSquaredTo(loc) > ACTION_RADIUS_SQ) {
          Direction dir = pathfinding.getGreedyMove(current, loc, true, Pathfinding.Mode.ALLY_ONLY);
          if (dir == null || !rc.canMove(dir)) { continue; }
          mapData.move(dir);
          current = rc.getLocation();
          // Check if there is enemy paint under our feet
          if (mop(current)) { break; }
        }
        if (!shouldMop(loc)) { continue; }
        if (mop(loc)) { break; }
      }
    }

    // Try to complete the ruin
    if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, pathfinding.getTarget())) {
      rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, pathfinding.getTarget());
      mapData.updateData(rc.senseMapInfo(pathfinding.getTarget()));
      cacheLoc = null;
      return true;
    }
      
    return false;
  }

}
