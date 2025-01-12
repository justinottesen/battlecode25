package jottesen_test;

import battlecode.common.*;

public class Tower extends Robot {

  private final int TOWER_ATTACK_RADIUS;

  private final MapLocation LOCATION;

  public Tower(RobotController rc_) throws GameActionException {
    super(rc_);

    TOWER_ATTACK_RADIUS = rc.getType().actionRadiusSquared; // NOTE: This will have to change if upgrades modify radius

    LOCATION = rc.getLocation();
  }

  public void run() throws GameActionException {
    // Attack enemies
    attackEnemies();
    
    // Spawn Robots
    spawnRobots();

    // TODO: Add communication logic for the map symmetry type
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
    // Early Game Special Cases
    if (rc.getRoundNum() == 1) {
      Direction r_dir = LOCATION.directionTo(MAP_CENTER);
      if (rc.canBuildRobot(UnitType.SOLDIER, LOCATION.add(r_dir))) {
        rc.buildRobot(UnitType.SOLDIER, LOCATION.add(r_dir));
        return;
      }
      Direction l_dir = r_dir.rotateLeft();
      if (rc.canBuildRobot(UnitType.SOLDIER, LOCATION.add(l_dir))) {
        rc.buildRobot(UnitType.SOLDIER, LOCATION.add(l_dir));
        return;
      }
      r_dir = r_dir.rotateRight();
      if (rc.canBuildRobot(UnitType.SOLDIER, LOCATION.add(r_dir))) {
        rc.buildRobot(UnitType.SOLDIER, LOCATION.add(r_dir));
        return;
      }
      l_dir = l_dir.rotateLeft();
      if (rc.canBuildRobot(UnitType.SOLDIER, LOCATION.add(l_dir))) {
        rc.buildRobot(UnitType.SOLDIER, LOCATION.add(l_dir));
        return;
      }
      r_dir = r_dir.rotateRight();
      if (rc.canBuildRobot(UnitType.SOLDIER, LOCATION.add(r_dir))) {
        rc.buildRobot(UnitType.SOLDIER, LOCATION.add(r_dir));
        return;
      }
      l_dir = l_dir.rotateLeft();
      if (rc.canBuildRobot(UnitType.SOLDIER, LOCATION.add(l_dir))) {
        rc.buildRobot(UnitType.SOLDIER, LOCATION.add(l_dir));
        return;
      }
      r_dir = r_dir.rotateRight();
      if (rc.canBuildRobot(UnitType.SOLDIER, LOCATION.add(r_dir))) {
        rc.buildRobot(UnitType.SOLDIER, LOCATION.add(r_dir));
        return;
      }
      l_dir = l_dir.rotateLeft();
      if (rc.canBuildRobot(UnitType.SOLDIER, LOCATION.add(l_dir))) {
        rc.buildRobot(UnitType.SOLDIER, LOCATION.add(l_dir));
        return;
      }
      return;
    }
  }
}