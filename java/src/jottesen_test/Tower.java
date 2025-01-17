package jottesen_test;

import battlecode.common.*;
import jottesen_test.util.MapData;
import jottesen_test.util.Communication;

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
        CREATED_ROUND > 5 && //not one of the original towers
        comms.tryBroadcastMessage( // We successfully sent the message to an adjacent bot
          comms.addCoordinates(comms.SUICIDE, LOCATION), rc.senseNearbyRobots(2, TEAM))) {
      System.out.println("Sent suicide message");
      rc.disintegrate();
      return;
    }
    communicateTowerTypes();
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
   * Broadcasts own tower type, then reads incoming broadcasts to count other tower types
   * 
   * @throws GameActionException
   */
   private void communicateTowerTypes() throws GameActionException{
    //First, get the appropriate comm message type based on what tower we are
    int commType = -1;
    switch(rc.getType().getBaseType()){ //note that getBaseType should return the level-one type of the tower regardless of tower upgrades (I'm not sure about this since it's very badly documented)
      case(UnitType.LEVEL_ONE_MONEY_TOWER):
        commType=Communication.SELF_TOWER_TYPE_IS_MONEY;
        break;
      case(UnitType.LEVEL_ONE_PAINT_TOWER):
        commType=Communication.SELF_TOWER_TYPE_IS_PAINT;
        break;
      case(UnitType.LEVEL_ONE_DEFENSE_TOWER):
        commType=Communication.SELF_TOWER_TYPE_IS_DEFENSE;
        break;
      default:
        return;
    }

    //broadcast our type and location
    comms.tryBroadcastMessage(comms.addCoordinates(commType, LOCATION));
    //this method in theory returns a success boolean, but idk what to do with it

    
    if(rc.getRoundNum()==1) return; //no messages to read

    //we clear the entire mapdata of towers so we can re-add them all
    //that way, any towers that aren't re-added we know are dead
    mapData.resetTowersAndRuins();
    paintTowers = 0;
    moneyTowers = 0;
    defenseTowers = 0;
    //add self to the count:
    switch(rc.getType().getBaseType()){ //note that getBaseType should return the level-one type of the tower regardless of tower upgrades (I'm not sure about this since it's very badly documented)
      case(UnitType.LEVEL_ONE_MONEY_TOWER):
        ++moneyTowers;
        break;
      case(UnitType.LEVEL_ONE_PAINT_TOWER):
        ++paintTowers;
        break;
      case(UnitType.LEVEL_ONE_DEFENSE_TOWER):
        ++defenseTowers;
        break;
      default:
        return;
    }
    //now read broadcast messages from last round only
    int lastRound = rc.getRoundNum()-1;
    Message[] lastRoundMessages = rc.readMessages(lastRound);
    for(Message m:lastRoundMessages){
      MapLocation towerLocation = comms.getCoordinates(m.getBytes());
      switch(comms.getMessageType(m.getBytes())){
        case(Communication.SELF_TOWER_TYPE_IS_PAINT):
          //increment the number of paint towers in count
          ++paintTowers;
          //add the paint tower to mapdata
          mapData.setTowerManually(MapData.PAINT_TOWER, towerLocation);
          break;
        case(Communication.SELF_TOWER_TYPE_IS_MONEY):
          //increment the number of money towers in count
          ++moneyTowers;
          //add the money tower to mapdata
          mapData.setTowerManually(MapData.MONEY_TOWER, towerLocation);
          break;
        case(Communication.SELF_TOWER_TYPE_IS_DEFENSE):
          //increment the number of defense towers in count
          ++defenseTowers;
          //add the defense tower to mapdata
          mapData.setTowerManually(MapData.DEFENSE_TOWER, towerLocation);
          break;
      }
    }
    rc.setIndicatorString("paintTowers: "+paintTowers+", moneyTowers: "+moneyTowers+", defenseTowers: "+defenseTowers);
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