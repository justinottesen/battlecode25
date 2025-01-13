package jottesen_test.util;

import battlecode.common.*;

public class Pathfinding {

  private final MapData mapData;
  private final RobotController rc;

  private Stack<MapLocation> targets;
  private Queue<MapLocation> stepCache;

  private final int MAX_TARGETS = 3; // Recursive calls using Bug Navigation
  private final int MAX_PATH_LENGTH = 10; // Number of steps to current target

  public Pathfinding(RobotController rc_, MapData mapData_) {
    rc = rc_;
    mapData = mapData_;
    targets = new Stack<MapLocation>(MAX_TARGETS);
    stepCache = new Queue<MapLocation>(MAX_PATH_LENGTH);
  }

  /**
   * Sets the target destination for pathfinding
   * 
   * @param target The target destination for the robot to reach
   * @return Whether this is changes the target location
   */
  public boolean setTarget(MapLocation target) {

    // Same as previous target
    if ((target != null && target.equals(targets.bottom())) 
        || (target == null && targets.empty())) {
      return false;
    }

    // New target
    targets.clear();
    if (target != null) { targets.push(target); }
    stepCache.clear();
    return true;
  }

  /**
   * Clears the current target
   */
  public void clearTarget() { targets.clear(); stepCache.clear(); }

  /**
   * Gets the target destination for pathfinding
   * 
   * @return The current target `MapLocation`
   */
  public MapLocation getTarget() { return targets.bottom(); }

  /**
   * Updates the target to the provided and returns the move that should be
   * made to reach that target
   * 
   * @param target The new target for pathfinding
   * @return The move that should be made to reach the target
   */
  public Direction getMove(MapLocation target) throws GameActionException { setTarget(target); return getMove(); }
  
  /**
   * Gets the next move that should be made to reach the target
   * 
   * TODO: Add a limit to the number of calculations per round? Or a bytecode stopping point?
   * 
   * @return The direction the robot should take to reach the target
   */
  public Direction getMove() throws GameActionException {
    final MapLocation current = rc.getLocation();

    // Can't move to a nonexistant target (or if we don't exist somehow)
    if (targets.empty() || current == null) { return null; }

    // If at target or intermediate target is found to be impassable, get rid of it
    while (current.equals(targets.top()) || (targets.used() > 1 && !mapData.passable(targets.top()))) { targets.pop(); }

    // No need to move if at the target
    if (targets.empty()) { return Direction.CENTER; }

    // Check the cache to see if we have calculated this before
    MapLocation loc = current;
    MapLocation prev = null;
    while (!stepCache.empty() && !current.equals(stepCache.front())) { stepCache.dequeue(); }
    if (current.equals(stepCache.front())) {
      loc = stepCache.back();
      stepCache.dequeue();

      // If we can't move in that direction anymore (body blocker), clear the cache and recalculate
      if (rc.isMovementReady() && rc.senseRobotAtLocation(stepCache.front()) != null) {
        loc = current;
        stepCache.clear();
      }
    }

    // Try greedily moving towards target
    Direction prevMove = null;
    while (loc != null && !targets.top().equals(loc) && mapData.known(loc) && stepCache.used() < stepCache.capacity()) {
      prev = loc;
      Direction move = getGreedyMove(loc, targets.top(), current.equals(loc));
      if (move == null) {
        loc = null;
      } else {
        loc = loc.add(move);
        // Don't add unknowns to the queue
        if (mapData.known(loc)) {
          // Combine cardinal directions into one move
          if (prevMove != null && !prevMove.equals(move) && (prevMove.dx == 0 ^ prevMove.dy == 0) && (move.dx == 0 ^ move.dy == 0)) {
            stepCache.popBack();
          }
          stepCache.enqueue(loc);
        }
      }
      prevMove = move;
    }

    // If we got to the target or an unknown square, return the move
    if (loc != null && (targets.top().equals(loc) || !mapData.known(loc) || stepCache.used() == stepCache.capacity())) {
      Direction move = current.directionTo(stepCache.front());
      return move;
    }
    loc = prev;
  
    // If we hit an obstacle, bugnav around it
    // Bugnav Left  
    Direction leftBugDir = closestAvailableLeftDirection(loc, loc.directionTo(targets.top()));
    MapLocation newLeftTarget = null;
    if (leftBugDir != null) {
      int turnIndex = 0; // If +2, we have rounded a corner
      newLeftTarget = loc.add(leftBugDir);
      while (rc.onTheMap(newLeftTarget) && mapData.known(newLeftTarget) && turnIndex < 2) {
        // Try to turn towards target
        leftBugDir = leftBugDir.rotateRight().rotateRight();
        turnIndex += 2;
        MapLocation candidate = newLeftTarget.add(leftBugDir);
        // Turn away until a valid move is found
        while (!rc.onTheMap(candidate) || !mapData.passable(candidate)) {
          leftBugDir = leftBugDir.rotateLeft();
          --turnIndex;
          candidate = newLeftTarget.add(leftBugDir);
        }
        // Suitable candidate has been found
        newLeftTarget = candidate;
      }
      if (!rc.onTheMap(newLeftTarget)) { newLeftTarget = null; }
    }

    // Bugnav right
    Direction rightBugDir = closestAvailableRightDirection(loc, loc.directionTo(targets.top()));
    MapLocation newRightTarget = null;
    if (rightBugDir != null && !rightBugDir.equals(leftBugDir)) {
      int turnIndex = 0; // If +2, we have rounded a corner
      newRightTarget = loc.add(rightBugDir);
      while (rc.onTheMap(newRightTarget) && mapData.known(newRightTarget) && turnIndex < 2) {
        // Try to turn towards target
        rightBugDir = rightBugDir.rotateLeft().rotateLeft();
        turnIndex += 2;
        MapLocation candidate = newRightTarget.add(rightBugDir);
        // Turn away until a valid move is found
        while (!rc.onTheMap(candidate) || !mapData.passable(candidate)) {
          rightBugDir = rightBugDir.rotateRight();
          --turnIndex;
          candidate = newRightTarget.add(rightBugDir);
        }
        // Suitable candidate has been found
        newRightTarget = candidate;
      }
      if (!rc.onTheMap(newRightTarget)) { newRightTarget = null; }
    }

    // Pick a new intermediate target
    if (newLeftTarget == null && newRightTarget == null) { return null; }
    int leftDistHeuristic = newLeftTarget == null ? mapData.MAX_DISTANCE_SQ : current.distanceSquaredTo(newLeftTarget) + newLeftTarget.distanceSquaredTo(targets.top());
    int rightDistanceHeuristic = newRightTarget == null ? mapData.MAX_DISTANCE_SQ : current.distanceSquaredTo(newRightTarget) + newRightTarget.distanceSquaredTo(targets.top());
    if (targets.push(leftDistHeuristic < rightDistanceHeuristic ? newLeftTarget : newRightTarget)) {
      stepCache.clear();
      return getMove();
    } else {
      System.out.println("No more pathfinding stack space");
      return closestAvailableDirection(current, loc.directionTo(targets.top()));
    }
  }

  /**
   * Finds the best move towards the target location.
   * 
   * @param loc The current position to move from
   * @return The direction to take to move towards the target 
   */
  public Direction getGreedyMove(MapLocation loc) throws GameActionException { return getGreedyMove(loc, targets.top()); }

  /**
   * Finds the best move towards the `goal` location.
   * 
   * @param loc The current position to move from
   * @param goal The goal position to move towards
   * @return The direction to take to move towards the target 
   */
  public Direction getGreedyMove(MapLocation loc, MapLocation goal) throws GameActionException { return getGreedyMove(loc, loc.directionTo(goal), false); }

  /**
   * Finds the closest legal move to the specified direction
   * @param loc The current position to move from
   * @param dir The goal direction to ty to match
   * @param checkCanMove Whether to check if the robot can move to a location
   * @return The direction to take to move towards the target 
   */
  public Direction getGreedyMove(MapLocation loc, MapLocation goal, boolean checkCanMove) throws GameActionException { return getGreedyMove(loc, loc.directionTo(goal), checkCanMove); }

  /**
   * Finds the closest legal move to the specified direction
   * @param loc The current position to move from
   * @param dir The goal direction to ty to match
   * @param checkCanMove Whether to check if the robot can move to a location
   * @return The direction to take to move towards the target 
   */
  public Direction getGreedyMove(MapLocation loc, Direction dir, boolean checkCanMove) throws GameActionException { return getGreedyMove(loc, dir, checkCanMove, false); }

  /**
   * Finds the closest legal move to the specified direction
   * @param loc The current position to move from
   * @param dir The goal direction to ty to match
   * @param checkCanMove Whether to check if the robot can move to a location
   * @param onlyAllyPaint Whether to only allow moves onto ally paint
   * @return The direction to take to move towards the target 
   */
  public Direction getGreedyMove(MapLocation loc, MapLocation goal, boolean checkCanMove, boolean onlyAllyPaint) throws GameActionException { return getGreedyMove(loc, loc.directionTo(goal), checkCanMove, onlyAllyPaint); }

  /**
   * Finds the closest legal move to the specified direction
   * @param loc The current position to move from
   * @param dir The goal direction to ty to match
   * @param checkCanMove Whether to check if the robot can move to a location
   * @param onlyAllyPaint Whether to only allow moves onto ally paint
   * @return The direction to take to move towards the target 
   */
  public Direction getGreedyMove(MapLocation loc, Direction dir, boolean checkCanMove, boolean onlyAllyPaint) throws GameActionException {
    // Try going towards it
    MapLocation next = loc.add(dir);
    if (mapData.passable(next) && 
        (!checkCanMove || rc.canMove(dir)) &&
        (!onlyAllyPaint || rc.senseMapInfo(next).getPaint().isAlly())) { return dir; }
    
    // Try turning left
    Direction leftDir = dir.rotateLeft();
    next = loc.add(leftDir);
    if (rc.onTheMap(next) && mapData.passable(next) &&
        (!checkCanMove || rc.canMove(leftDir)) &&
        (!onlyAllyPaint || rc.senseMapInfo(next).getPaint().isAlly())) { return leftDir; }

    // Try turning right
    dir = dir.rotateRight();
    next = loc.add(dir);
    if (rc.onTheMap(next) && mapData.passable(next) &&
    (!checkCanMove || rc.canMove(dir)) &&
    (!onlyAllyPaint || rc.senseMapInfo(next).getPaint().isAlly())) { return dir; }

    // Give up if none work
    return null;
  }

  /**
   * Gets the closest direction to a target direction. This may result in moving
   * backwards. In most scenarios, `getGreedyMove` is a better choice.
   * 
   * @param loc The starting location
   * @param dir The goal direction
   * @return The closest available move to take
   */
  public Direction closestAvailableDirection(MapLocation loc, Direction dir) {
    if (mapData.passable(loc.add(dir))) { return dir; } // 0

    Direction leftDir = dir.rotateLeft();
    if (mapData.passable(loc.add(leftDir))) { return leftDir; } // 45

    dir = dir.rotateRight();
    if (mapData.passable(loc.add(dir))) { return dir; } // -45

    leftDir = dir.rotateLeft();
    if (mapData.passable(loc.add(leftDir))) { return leftDir; } // 90

    dir = dir.rotateRight();
    if (mapData.passable(loc.add(dir))) { return dir; } // -90
    
    leftDir = dir.rotateLeft();
    if (mapData.passable(loc.add(leftDir))) { return leftDir; } // 135

    dir = dir.rotateRight();
    if (mapData.passable(loc.add(dir))) { return dir; } // -135

    leftDir = dir.rotateLeft();
    if (mapData.passable(loc.add(leftDir))) { return leftDir; } // 180

    return null;
  }

  /**
   * Gets the closest left direction to a target direction. This may result in moving
   * backwards. In most scenarios, `getGreedyMove` is a better choice.
   * 
   * @param loc The starting location
   * @param dir The goal direction
   * @return The closest available move to take
   */
  public Direction closestAvailableLeftDirection(MapLocation loc, Direction dir) {
    if (mapData.passable(loc.add(dir))) { return dir; } // 0

    dir = dir.rotateLeft();
    if (mapData.passable(loc.add(dir))) { return dir; } // 45

    dir = dir.rotateLeft();
    if (mapData.passable(loc.add(dir))) { return dir; } // 90

    dir = dir.rotateLeft();
    if (mapData.passable(loc.add(dir))) { return dir; } // 135

    dir = dir.rotateLeft();
    if (mapData.passable(loc.add(dir))) { return dir; } // 180

    return null;
  }

  /**
   * Gets the closest right direction to a target direction. This may result in moving
   * backwards. In most scenarios, `getGreedyMove` is a better choice.
   * 
   * @param loc The starting location
   * @param dir The goal direction
   * @return The closest available move to take
   */
  public Direction closestAvailableRightDirection(MapLocation loc, Direction dir) {
    if (mapData.passable(loc.add(dir))) { return dir; } // 0

    dir = dir.rotateRight();
    if (mapData.passable(loc.add(dir))) { return dir; } // 45

    dir = dir.rotateRight();
    if (mapData.passable(loc.add(dir))) { return dir; } // 90

    dir = dir.rotateRight();
    if (mapData.passable(loc.add(dir))) { return dir; } // 135

    dir = dir.rotateRight();
    if (mapData.passable(loc.add(dir))) { return dir; } // 180

    return null;
  }
}