package jottesen_test;

import battlecode.common.*;

public final class Tower extends Robot {

  private final int TOWER_ATTACK_RADIUS;

  private final MapLocation LOCATION;

  private final int CREATED_ROUND;

  public Tower(RobotController rc_) throws GameActionException {
    super(rc_);

    TOWER_ATTACK_RADIUS = rc.getType().actionRadiusSquared; // NOTE: This will have to change if upgrades modify radius

    LOCATION = rc.getLocation();

    CREATED_ROUND = rc.getRoundNum();
  }

  protected void doMicro() throws GameActionException {
    attackEnemies();

    // Suicide for paint if worth it
    if (rc.getPaint() == 0 && // No more paint
        rc.senseNearbyRobots(-1, OPPONENT).length == 0 && // No visible enemies
        rc.getChips() > rc.getType().moneyCost * 2 && // Enough chips (we hope)
        comms.tryBroadcastMessage( // We successfully sent the message to an adjacent bot
          comms.addCoordinates(comms.SUICIDE, LOCATION), rc.senseNearbyRobots(2, TEAM))) {
      System.out.println("Sent suicide message");
      rc.disintegrate();
      return;
    }
  }

  protected void doMacro() throws GameActionException {
    spawnRobots();
  }

  /**
   * Attacks all enemies with AoE, and targets the first enemy found which can
   * be killed in one shot. Otherwise, targets the lowest health enemy.
   * 
   * @throws GameActionException
   */
  private void attackEnemies() throws GameActionException {
    // Find nearby enemies
    RobotInfo[] enemies = rc.senseNearbyRobots(TOWER_ATTACK_RADIUS, OPPONENT);
    if (enemies.length == 0) { return; }
    
    // Perform AoE attack
    rc.attack(null);

    // Find weakest or one-shot enemy
    RobotInfo target = enemies[0];
    if (target.getHealth() > rc.getType().attackStrength) {
      for (RobotInfo enemy : enemies) {
        if (enemy.getHealth() < target.getHealth()) {
          target = enemy;
          if (target.getHealth() < rc.getType().attackStrength) {
            break;
          }
        }
      }
    }

    // Attack enemy
    if (rc.canAttack(target.getLocation())) {
      rc.attack(target.getLocation());
    }
  }

  /**
   * Handles the logic of spawning robots.
   * 
   * Each tower simply spawns a single soldier at the start of the game.
   * The location of the spawned soldier is in the closest available direction
   * to the middle of the map.
   * 
   * @throws GameActionException
   */
  private void spawnRobots() throws GameActionException {
    switch (rc.getRoundNum() - CREATED_ROUND + 1) {
      case 1: spawnRound1(); return;
      case 2: spawnRound2(); return;
      default: break;
    }

    // Spawn more if we got hella chips
    if (rc.getChips() > rc.getType().moneyCost * 2) {
      switch (rc.getRoundNum() % 5) {
        case 0: trySpawn(UnitType.MOPPER, mapData.MAP_CENTER); break;
        default: trySpawn(UnitType.SOLDIER, mapData.MAP_CENTER); break;
      }
    }
  }

  /**
   * Finds the closest available spawn location to the provided target
   * @param type The type of robot to be build (idk if this is even necessary)
   * @param target The location to get close to
   * @return The closest available location
   */
  private MapLocation getSpawnLoc(UnitType type, MapLocation target) throws GameActionException {
    if (rc.canBuildRobot(UnitType.SOLDIER, target)) { return target; }

    MapLocation closest = null;
    int closest_dist = mapData.MAX_DISTANCE_SQ;
    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(LOCATION, GameConstants.BUILD_ROBOT_RADIUS_SQUARED)) {
      rc.setIndicatorDot(loc, 255, 255, 255);
      int dist = loc.distanceSquaredTo(target);
      if (rc.canBuildRobot(UnitType.SOLDIER, loc) && dist < closest_dist) {
        closest = loc;
        closest_dist = dist;
        if (closest_dist < 3) { break; }
      }
    }

    return closest;
  }

  /**
   * Tries to spawn a robot at the closest available location to the target
   * @param type The type of the robot to spawn
   * @param target The location to get close to
   * @param skipTimer Whether or not to skip the spawn cooldown
   * @return Whether the robot was spawned or not
   * @throws GameActionException
   */
  private boolean trySpawn(UnitType type, MapLocation target) throws GameActionException {
    MapLocation loc = getSpawnLoc(type, target);
    if (loc != null) {
      rc.buildRobot(type, loc);
      return true;
    }
    return false;
  }

  /**
   * Handles robot spawning for round 1
   * 
   * @throws GameActionException
   */
  private void spawnRound1() throws GameActionException {
    trySpawn(UnitType.SOLDIER, mapData.MAP_CENTER);
  }

  /**
   * Handles robot spawning for round 2
   * 
   * @throws GameActionException
   */
  private void spawnRound2() throws GameActionException {
    trySpawn(UnitType.MOPPER, mapData.MAP_CENTER);
  }
}