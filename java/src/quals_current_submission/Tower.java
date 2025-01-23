package quals_current_submission;

import quals_current_submission.util.*;

import battlecode.common.*;

public final class Tower extends Robot {

  private final int TOWER_ATTACK_RADIUS;

  private final MapLocation LOCATION;

  int lastSeenEnemyRound = -1;
  private final int LAST_SEEN_SUICIDE_THRESHOLD = 50;

  private final int PAINT_SUICIDE_THRESHOLD = 50;

  public Tower(RobotController rc_) throws GameActionException {
    super(rc_);

    lastSeenEnemyRound = rc.getRoundNum(); // Pretend we just saw an enemy when we spawn in

    TOWER_ATTACK_RADIUS = rc.getType().actionRadiusSquared; // NOTE: This will have to change if upgrades modify radius

    LOCATION = rc.getLocation();
  }

  protected void doMicro() throws GameActionException {
    attackEnemies();

    // Check for new enemies
    if (lastSeenEnemyRound < rc.getRoundNum() && rc.senseNearbyRobots(-1, OPPONENT).length != 0) {
      lastSeenEnemyRound = rc.getRoundNum();
    }

    // If no enemies in a while, spend paint and try to suicide for more
    if (rc.getRoundNum() - lastSeenEnemyRound > LAST_SEEN_SUICIDE_THRESHOLD && // In a safe location
        rc.getPaint() >= 100 && // Leftover paint
        rc.getChips() > rc.getType().moneyCost * 10 && // HELLA chips
        towerPatternComplete(UnitType.LEVEL_ONE_MONEY_TOWER)) {
      System.out.println("SAFE TOWER - USING RESOURCES");
      MapLocation spawnLoc = getSpawnLoc(null);
      if (spawnLoc != null) {
        if (rc.getPaint() >= UnitType.SPLASHER.paintCost) {
          trySpawn(UnitType.SPLASHER, spawnLoc);
        } else if (rc.getPaint() >= UnitType.SOLDIER.paintCost) {
          trySpawn(UnitType.SOLDIER, spawnLoc);
        } else {
          trySpawn(UnitType.MOPPER, spawnLoc);
        }
      }
    }

    // Suicide for paint if worth it
    if ((rc.getPaint() < PAINT_SUICIDE_THRESHOLD) && // No more paint
        //rc.senseNearbyRobots(-1, OPPONENT).length == 0 && // No visible enemies
        rc.getChips() > rc.getType().moneyCost * 2 && // Enough chips (we hope)
        towerPatternComplete(UnitType.LEVEL_ONE_MONEY_TOWER) &&
        Communication.trySendAllMessage( // We successfully sent the message to an adjacent bot
          Communication.addCoordinates(Communication.SUICIDE, LOCATION), rc.senseNearbyRobots(GameConstants.MESSAGE_RADIUS_SQUARED, TEAM))) {
      rc.setIndicatorString("Sent suicide message");
      rc.disintegrate();
      return;
    }

    // Read incoming messages
    for (Message m : rc.readMessages(-1)) {
      switch (m.getBytes() & Communication.MESSAGE_TYPE_BITMASK) {
        case Communication.REQUEST_MOPPER:
          MapLocation spawnLoc = trySpawn(UnitType.MOPPER, Communication.getCoordinates(m.getBytes()));
          if (spawnLoc != null) {
            Communication.trySendMessage(m.getBytes(), spawnLoc);
          }
          break;
        default:
          System.out.println("RECEIVED UNKNOWN MESSAGE: " + m);
      }
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
  
    lastSeenEnemyRound = rc.getRoundNum();
    
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
    switch (rc.getRoundNum()) {
      case 1: spawnRound1(); return;
      case 2: spawnRound1(); return;
      default: break;
    }

    // Spawn more if we got hella chips
    if (rc.getChips() > rc.getType().moneyCost * 2) {
      switch (rc.getRoundNum() % 4) {
        case 0: trySpawn(UnitType.MOPPER, MapData.MAP_CENTER); break;
        case 1: trySpawn(UnitType.SPLASHER, MapData.MAP_CENTER); break;
        default: trySpawn(UnitType.SOLDIER, MapData.MAP_CENTER); break;
      }
    }
  }

  /**
   * Finds the closest available spawn location to the provided target
   * @param target The location to get close to
   * @return The closest available location
   */
  private MapLocation getSpawnLoc(MapLocation target) throws GameActionException {
    // Check if we can build mopper, the cheapest unit
    if (target != null && rc.canBuildRobot(UnitType.MOPPER, target)) { return target; }

    MapLocation closest = null;
    int closest_dist = MapData.MAX_DISTANCE_SQ;
    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(LOCATION, GameConstants.BUILD_ROBOT_RADIUS_SQUARED)) {
      if (!rc.canBuildRobot(UnitType.MOPPER, loc)) { continue; }
      if (target == null) { return loc; }
      int dist = loc.distanceSquaredTo(target);
      if (dist < closest_dist) {
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
   * @return The location the robot was spawned at, if successful
   * @throws GameActionException
   */
  private MapLocation trySpawn(UnitType type, MapLocation target) throws GameActionException {
    MapLocation loc = getSpawnLoc(target);
    if (loc != null && rc.canBuildRobot(type, loc)) { rc.buildRobot(type, loc); return loc; }
    return null;
  }

  /**
   * Handles robot spawning for round 1
   * 
   * @throws GameActionException
   */
  private void spawnRound1() throws GameActionException {
    trySpawn(UnitType.SOLDIER, MapData.MAP_CENTER);
  }

  /**
   * Handles robot spawning for round 2
   * 
   * @throws GameActionException
   */
  private void spawnRound2() throws GameActionException {
    trySpawn(UnitType.MOPPER, MapData.MAP_CENTER);
  }

  private boolean towerPatternComplete(UnitType type) throws GameActionException {
    if (!type.isTowerType()) { return false; }
    boolean[][] pattern = rc.getTowerPattern(type);

    int x = LOCATION.x - (GameConstants.PATTERN_SIZE / 2);
    int y = LOCATION.y - (GameConstants.PATTERN_SIZE / 2);

    for (MapInfo tile : rc.senseNearbyMapInfos(rc.getLocation(), 8)){
      MapLocation loc = tile.getMapLocation();
      if (loc.equals(LOCATION)) { continue; }
      PaintType paint = tile.getPaint();
      if (!paint.isAlly() || pattern[loc.x - x][loc.y - y] != paint.isSecondary()) { return false; }
    }

    return true;
  }
}