package sprint_2.util;

import sprint_2.*;

import battlecode.common.*;

public class Pathfinding {

  private static final int MAX_TARGETS = 3; // Recursive calls using Bug Navigation
  private static final int MAX_PATH_LENGTH = 10; // Number of steps to current target

  private static Stack<MapLocation> targets = new Stack<MapLocation>(MAX_TARGETS);
  private static Queue<MapLocation> stepCache = new Queue<MapLocation>(MAX_PATH_LENGTH);

  public static enum Mode {
    ANY,
    NO_ENEMY,
    ALLY_ONLY
  }

  /**
   * Sets the pathfinding target to a new explore target
   * @return The new target
   */
  public static MapLocation setExplore() { 
    setTarget(MapData.getExploreTarget());
    return getTarget();
  }

  /**
   * Sets the target destination for pathfinding
   * 
   * @param goal The goal object containing the pathfinding target
   * @return Whether this is changes the target location
   */
  public static boolean setTarget(Goal goal) {
    return setTarget(goal.target);
  }

  /**
   * Sets the target destination for pathfinding
   * 
   * @param target The target destination for the robot to reach
   * @return Whether this is changes the target location
   */
  public static boolean setTarget(MapLocation target) {

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
  public static void clearTarget() { targets.clear(); stepCache.clear(); }

  /**
   * Gets the target destination for pathfinding
   * 
   * @return The current target `MapLocation`
   */
  public static MapLocation getTarget() { return targets.bottom(); }

  /**
   * @param target The new target for pathfinding
   * @return The move that should be made to reach the target
   */
  public static Direction getMove(MapLocation target) throws GameActionException { setTarget(target); return getMove(); }

  /**
   * Updates the target to the provided and returns the move that should be
   * made to reach that target
   * 
   * @param target The new target for pathfinding
   * @param mode The mode to use for generated moves
   * @return The move that should be made to reach the target
   */
  public static Direction getMove(MapLocation target, Mode mode) throws GameActionException { setTarget(target); return getMove(mode); }

   /**
   * Gets the next move that should be made to reach the target
   * 
   * TODO: Add a limit to the number of calculations per round? Or a bytecode stopping point?
   * 
   * @return The direction the robot should take to reach the target
   */
  public static Direction getMove() throws GameActionException { return getMove(Mode.ANY); }

  /**
   * Gets the next move that should be made to reach the target
   * 
   * @param the mode to use for generated moves
   * 
   * TODO: Add a limit to the number of calculations per round? Or a bytecode stopping point?
   * 
   * @return The direction the robot should take to reach the target
   */
  public static Direction getMove(Mode mode) throws GameActionException {
    final MapLocation current = Robot.rc.getLocation();

    // Can't move to a nonexistant target (or if we don't exist somehow)
    if (targets.empty() || current == null) { return null; }

    // If at target or intermediate target is found to be impassable, get rid of it
    while (targets.used() > 1 && (current.equals(targets.top()) || (targets.used() > 1 && !MapData.passable(targets.top())))) { targets.pop(); }

    // No need to move if at the target (or adjacent to it)
    if (Robot.rc.getLocation().isAdjacentTo(targets.bottom())) { return Direction.CENTER; }

    // Check the cache to see if we have calculated this before
    MapLocation loc = current;
    MapLocation prev = null;
    while (!stepCache.empty() && !current.equals(stepCache.front())) { stepCache.dequeue(); }
    if (current.equals(stepCache.front())) {
      loc = stepCache.back();
      stepCache.dequeue();

      // If we can't move in that direction anymore (body blocker / paint), clear the cache and recalculate
      if (!stepCache.empty() && Robot.rc.isMovementReady() && 
        (Robot.rc.senseRobotAtLocation(stepCache.front()) != null || 
          switch (mode) { 
          case Mode.ANY -> false;
          case Mode.ALLY_ONLY -> !Robot.rc.senseMapInfo(stepCache.front()).getPaint().isAlly(); 
          case Mode.NO_ENEMY -> Robot.rc.senseMapInfo(stepCache.front()).getPaint().isEnemy(); 
          })) {
        loc = current;
        stepCache.clear();
      }
    }

    // Try greedily moving towards target
    Direction prevMove = null;
    while (loc != null && !targets.top().equals(loc) && MapData.known(loc) && stepCache.used() < stepCache.capacity()) {
      prev = loc;
      // Reduce distance for greedy since extra moves can take you out of vision range
      Direction move = getGreedyMove(loc, targets.top(), Robot.rc.getLocation().equals(loc), Robot.rc.getLocation().isWithinDistanceSquared(loc, GameConstants.VISION_RADIUS_SQUARED / 2) ? mode : Mode.ANY);
      if (move == null) {
        loc = null;
      } else {
        loc = loc.add(move);
        // Don't add unknowns to the queue
        if (MapData.known(loc)) {
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
    if (loc != null && (targets.top().equals(loc) || !MapData.known(loc) || stepCache.used() == stepCache.capacity())) {
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
      while (Robot.rc.onTheMap(newLeftTarget) && MapData.known(newLeftTarget) && turnIndex < 2) {
        // Try to turn towards target
        leftBugDir = leftBugDir.rotateRight().rotateRight();
        turnIndex += 2;
        MapLocation candidate = newLeftTarget.add(leftBugDir);
        // Turn away until a valid move is found
        while (!Robot.rc.onTheMap(candidate) || !MapData.passable(candidate)) {
          leftBugDir = leftBugDir.rotateLeft();
          --turnIndex;
          candidate = newLeftTarget.add(leftBugDir);
        }
        // Suitable candidate has been found
        newLeftTarget = candidate;
      }
      if (!Robot.rc.onTheMap(newLeftTarget)) { newLeftTarget = null; }
    }

    // Bugnav right
    Direction rightBugDir = closestAvailableRightDirection(loc, loc.directionTo(targets.top()));
    MapLocation newRightTarget = null;
    if (rightBugDir != null && !rightBugDir.equals(leftBugDir)) {
      int turnIndex = 0; // If +2, we have rounded a corner
      newRightTarget = loc.add(rightBugDir);
      while (Robot.rc.onTheMap(newRightTarget) && MapData.known(newRightTarget) && turnIndex < 2) {
        // Try to turn towards target
        rightBugDir = rightBugDir.rotateLeft().rotateLeft();
        turnIndex += 2;
        MapLocation candidate = newRightTarget.add(rightBugDir);
        // Turn away until a valid move is found
        while (!Robot.rc.onTheMap(candidate) || !MapData.passable(candidate)) {
          rightBugDir = rightBugDir.rotateRight();
          --turnIndex;
          candidate = newRightTarget.add(rightBugDir);
        }
        // Suitable candidate has been found
        newRightTarget = candidate;
      }
      if (!Robot.rc.onTheMap(newRightTarget)) { newRightTarget = null; }
    }

    // Pick a new intermediate target
    if (newLeftTarget == null && newRightTarget == null) { return null; }
    int leftDistHeuristic = newLeftTarget == null ? MapData.MAX_DISTANCE_SQ : current.distanceSquaredTo(newLeftTarget) + newLeftTarget.distanceSquaredTo(targets.top());
    int rightDistanceHeuristic = newRightTarget == null ? MapData.MAX_DISTANCE_SQ : current.distanceSquaredTo(newRightTarget) + newRightTarget.distanceSquaredTo(targets.top());
    if (targets.push(leftDistHeuristic < rightDistanceHeuristic ? newLeftTarget : newRightTarget)) {
      stepCache.clear();
      return getMove(mode);
    } else {
      // TODO: Instead, take next move to a target
      return closestAvailableDirection(current, loc.directionTo(targets.top()));
    }
  }

  /**
   * Finds the best move towards the target location.
   * 
   * @param loc The current position to move from
   * @return The direction to take to move towards the target 
   */
  public static Direction getGreedyMove(MapLocation loc) throws GameActionException { return getGreedyMove(loc, targets.top()); }

  /**
   * Finds the best move towards the `goal` location.
   * 
   * @param loc The current position to move from
   * @param goal The goal position to move towards
   * @return The direction to take to move towards the target 
   */
  public static Direction getGreedyMove(MapLocation loc, MapLocation goal) throws GameActionException { return getGreedyMove(loc, goal, false); }

  /**
   * Finds the closest legal move to the specified direction
   * @param loc The current position to move from
   * @param dir The goal direction to ty to match
   * @param checkCanMove Whether to check if the robot can move to a location
   * @return The direction to take to move towards the target 
   */
  public static Direction getGreedyMove(MapLocation loc, MapLocation goal, boolean checkCanMove) throws GameActionException { return getGreedyMove(loc, goal, checkCanMove, Mode.ANY); }

  /**
   * Finds the closest legal move to the specified direction
   * @param loc The current position to move from
   * @param dir The goal direction to ty to match
   * @param checkCanMove Whether to check if the robot can move to a location
   * @return The direction to take to move towards the target 
   */
  public static Direction getGreedyMove(MapLocation loc, Direction dir, boolean checkCanMove) throws GameActionException { return getGreedyMove(loc, dir, checkCanMove, Mode.ANY); }

  /**
   * Finds the closest legal move to the specified direction
   * @param loc The current position to move from
   * @param dir The goal direction to ty to match
   * @param checkCanMove Whether to check if the robot can move to a location
   * @param onlyAllyPaint Whether to only allow moves onto ally paint
   * @return The direction to take to move towards the target 
   */
  public static Direction getGreedyMove(MapLocation loc, MapLocation goal, boolean checkCanMove, Mode mode) throws GameActionException {
    // Try going towards it
    Direction l_dir = loc.directionTo(goal);
    MapLocation l_next = loc.add(l_dir);
    if (MapData.passable(l_next) && 
        (!checkCanMove || Robot.rc.canMove(l_dir)) &&
        (switch (mode) { 
          case Mode.ANY -> true;
          case Mode.ALLY_ONLY -> Robot.rc.senseMapInfo(l_next).getPaint().isAlly(); 
          case Mode.NO_ENEMY -> !Robot.rc.senseMapInfo(l_next).getPaint().isEnemy(); 
        })) { return l_dir; }
    
    // Figure out which direction is better
    Direction r_dir = l_dir.rotateRight();
    MapLocation r_next = loc.add(r_dir);
    l_dir = l_dir.rotateLeft();
    l_next = loc.add(l_dir);
    if (l_next.distanceSquaredTo(goal) < r_next.distanceSquaredTo(goal)) {
      // Try turning left
      if (Robot.rc.onTheMap(l_next) && MapData.passable(l_next) &&
          (!checkCanMove || Robot.rc.canMove(l_dir)) &&
          (switch (mode) { 
            case Mode.ANY -> true;
            case Mode.ALLY_ONLY -> Robot.rc.senseMapInfo(l_next).getPaint().isAlly(); 
            case Mode.NO_ENEMY -> !Robot.rc.senseMapInfo(l_next).getPaint().isEnemy(); 
          })) { return l_dir; }

      // Try turning right
      if (Robot.rc.onTheMap(r_next) && MapData.passable(r_next) &&
          (!checkCanMove || Robot.rc.canMove(r_dir)) &&
          (switch (mode) { 
            case Mode.ANY -> true;
            case Mode.ALLY_ONLY -> Robot.rc.senseMapInfo(r_next).getPaint().isAlly(); 
            case Mode.NO_ENEMY -> !Robot.rc.senseMapInfo(r_next).getPaint().isEnemy(); 
          })) { return r_dir; }
      
    } else {
      // Try turning right
      if (Robot.rc.onTheMap(r_next) && MapData.passable(r_next) &&
          (!checkCanMove || Robot.rc.canMove(r_dir)) &&
          (switch (mode) { 
            case Mode.ANY -> true;
            case Mode.ALLY_ONLY -> Robot.rc.senseMapInfo(r_next).getPaint().isAlly(); 
            case Mode.NO_ENEMY -> !Robot.rc.senseMapInfo(r_next).getPaint().isEnemy(); 
          })) { return r_dir; }

      // Try turning left
      if (Robot.rc.onTheMap(l_next) && MapData.passable(l_next) &&
          (!checkCanMove || Robot.rc.canMove(l_dir)) &&
          (switch (mode) { 
            case Mode.ANY -> true;
            case Mode.ALLY_ONLY -> Robot.rc.senseMapInfo(l_next).getPaint().isAlly(); 
            case Mode.NO_ENEMY -> !Robot.rc.senseMapInfo(l_next).getPaint().isEnemy(); 
          })) { return l_dir; }
    }
    return null;
  }

  /**
   * Finds the closest legal move to the specified direction
   * @param loc The current position to move from
   * @param dir The goal direction to ty to match
   * @param checkCanMove Whether to check if the robot can move to a location
   * @param onlyAllyPaint Whether to only allow moves onto ally paint
   * @return The direction to take to move towards the target 
   */
  public static Direction getGreedyMove(MapLocation loc, Direction dir, boolean checkCanMove, Mode mode) throws GameActionException {
    // Try going towards it
    MapLocation next = loc.add(dir);
    if (MapData.passable(next) && 
        (!checkCanMove || Robot.rc.canMove(dir)) &&
        (switch (mode) { 
          case Mode.ANY -> true;
          case Mode.ALLY_ONLY -> Robot.rc.senseMapInfo(next).getPaint().isAlly(); 
          case Mode.NO_ENEMY -> !Robot.rc.senseMapInfo(next).getPaint().isEnemy(); 
        })) { return dir; }
    
    // Try turning left
    Direction leftDir = dir.rotateLeft();
    next = loc.add(leftDir);
    if (Robot.rc.onTheMap(next) && MapData.passable(next) &&
        (!checkCanMove || Robot.rc.canMove(leftDir)) &&
        (switch (mode) { 
          case Mode.ANY -> true;
          case Mode.ALLY_ONLY -> Robot.rc.senseMapInfo(next).getPaint().isAlly(); 
          case Mode.NO_ENEMY -> !Robot.rc.senseMapInfo(next).getPaint().isEnemy(); 
        })) { return leftDir; }

    // Try turning right
    dir = dir.rotateRight();
    next = loc.add(dir);
    if (Robot.rc.onTheMap(next) && MapData.passable(next) &&
        (!checkCanMove || Robot.rc.canMove(dir)) &&
        (switch (mode) { 
          case Mode.ANY -> true;
          case Mode.ALLY_ONLY -> Robot.rc.senseMapInfo(next).getPaint().isAlly(); 
          case Mode.NO_ENEMY -> !Robot.rc.senseMapInfo(next).getPaint().isEnemy(); 
        })) { return dir; }

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
  public static Direction closestAvailableDirection(MapLocation loc, Direction dir) {
    MapLocation resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 0

    Direction leftDir = dir.rotateLeft();
    resultLoc = loc.add(leftDir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return leftDir; } // 45

    dir = dir.rotateRight();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // -45

    leftDir = dir.rotateLeft();
    resultLoc = loc.add(leftDir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return leftDir; } // 90

    dir = dir.rotateRight();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // -90
    
    leftDir = dir.rotateLeft();
    resultLoc = loc.add(leftDir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return leftDir; } // 135

    dir = dir.rotateRight();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // -135

    leftDir = dir.rotateLeft();
    resultLoc = loc.add(leftDir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return leftDir; } // 180

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
  public static Direction closestAvailableLeftDirection(MapLocation loc, Direction dir) {
    MapLocation resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 0

    dir = dir.rotateLeft();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 45

    dir = dir.rotateLeft();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 90

    dir = dir.rotateLeft();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 135

    dir = dir.rotateLeft();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 180

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
  public static Direction closestAvailableRightDirection(MapLocation loc, Direction dir) {
    MapLocation resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 0

    dir = dir.rotateRight();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 45

    dir = dir.rotateRight();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 90

    dir = dir.rotateRight();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 135

    dir = dir.rotateRight();
    resultLoc = loc.add(dir);
    if (Robot.rc.onTheMap(resultLoc) && MapData.passable(resultLoc)) { return dir; } // 180

    return null;
  }
}