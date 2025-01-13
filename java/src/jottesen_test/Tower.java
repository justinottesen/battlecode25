package jottesen_test;

import battlecode.common.*;

public final class Tower extends Robot {

  private final int TOWER_ATTACK_RADIUS;

  private final MapLocation LOCATION;

  public Tower(RobotController rc_) throws GameActionException {
    super(rc_);

    TOWER_ATTACK_RADIUS = rc.getType().actionRadiusSquared; // NOTE: This will have to change if upgrades modify radius

    LOCATION = rc.getLocation();
  }

  protected void doMicro() throws GameActionException {
    attackEnemies();
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
    switch (rc.getRoundNum()) {
      case 1: spawnRound1(); break;
      case 2: spawnRound2(); break;
      default: break;
    }
  }

  /**
   * Handles robot spawning for round 1
   * 
   * @throws GameActionException
   */
  private void spawnRound1() throws GameActionException {
    Direction r_dir = LOCATION.directionTo(mapData.MAP_CENTER);
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

  /**
   * Handles robot spawning for round 2
   * 
   * @throws GameActionException
   */
  private void spawnRound2() throws GameActionException {
    Direction r_dir = LOCATION.directionTo(mapData.MAP_CENTER);
    if (rc.canBuildRobot(UnitType.MOPPER, LOCATION.add(r_dir))) {
      rc.buildRobot(UnitType.MOPPER, LOCATION.add(r_dir));
      return;
    }
    Direction l_dir = r_dir.rotateLeft();
    if (rc.canBuildRobot(UnitType.MOPPER, LOCATION.add(l_dir))) {
      rc.buildRobot(UnitType.MOPPER, LOCATION.add(l_dir));
      return;
    }
    r_dir = r_dir.rotateRight();
    if (rc.canBuildRobot(UnitType.MOPPER, LOCATION.add(r_dir))) {
      rc.buildRobot(UnitType.MOPPER, LOCATION.add(r_dir));
      return;
    }
    l_dir = l_dir.rotateLeft();
    if (rc.canBuildRobot(UnitType.MOPPER, LOCATION.add(l_dir))) {
      rc.buildRobot(UnitType.MOPPER, LOCATION.add(l_dir));
      return;
    }
    r_dir = r_dir.rotateRight();
    if (rc.canBuildRobot(UnitType.MOPPER, LOCATION.add(r_dir))) {
      rc.buildRobot(UnitType.MOPPER, LOCATION.add(r_dir));
      return;
    }
    l_dir = l_dir.rotateLeft();
    if (rc.canBuildRobot(UnitType.MOPPER, LOCATION.add(l_dir))) {
      rc.buildRobot(UnitType.MOPPER, LOCATION.add(l_dir));
      return;
    }
    r_dir = r_dir.rotateRight();
    if (rc.canBuildRobot(UnitType.MOPPER, LOCATION.add(r_dir))) {
      rc.buildRobot(UnitType.MOPPER, LOCATION.add(r_dir));
      return;
    }
    l_dir = l_dir.rotateLeft();
    if (rc.canBuildRobot(UnitType.MOPPER, LOCATION.add(l_dir))) {
      rc.buildRobot(UnitType.MOPPER, LOCATION.add(l_dir));
      return;
    }
    return;
  }
}