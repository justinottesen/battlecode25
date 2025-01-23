package micro.util;

import battlecode.common.*;
import micro.*;

class Micro {
  static private MicroInfo[] infos;

  static private int visibleFriendlySoldiers;
  static private int currentDistanceToEnemyTower;

  static public Direction getMove() throws GameActionException {
    if (!Robot.rc.isMovementReady()) { return Direction.CENTER; }

    // Reset variables
    infos = new MicroInfo[9];
    infos[0] = new MicroInfo(Direction.NORTH);
    infos[1] = new MicroInfo(Direction.NORTHEAST);
    infos[2] = new MicroInfo(Direction.EAST);
    infos[3] = new MicroInfo(Direction.SOUTHEAST);
    infos[4] = new MicroInfo(Direction.SOUTH);
    infos[5] = new MicroInfo(Direction.SOUTHWEST);
    infos[6] = new MicroInfo(Direction.WEST);
    infos[7] = new MicroInfo(Direction.NORTHWEST);
    infos[8] = new MicroInfo(Direction.CENTER);

    visibleFriendlySoldiers = 0;
    currentDistanceToEnemyTower = MapData.MAX_DISTANCE_SQ;

    // Check nearby map info
    for (MapInfo info : Robot.rc.senseNearbyMapInfos(2)) {
      infos[0].updateMapInfo(info);
      infos[1].updateMapInfo(info);
      infos[2].updateMapInfo(info);
      infos[3].updateMapInfo(info);
      infos[4].updateMapInfo(info);
      infos[5].updateMapInfo(info);
      infos[6].updateMapInfo(info);
      infos[7].updateMapInfo(info);
      infos[8].updateMapInfo(info);
    }

    // Check nearby map info
    for (RobotInfo info : Robot.rc.senseNearbyRobots()) {
      if (info.getTeam() == Robot.TEAM && info.getType() == UnitType.SOLDIER) {
        ++visibleFriendlySoldiers;
      }
      if (info.getTeam() == Robot.OPPONENT && info.getType().isTowerType() && 
          Robot.rc.getLocation().isWithinDistanceSquared(info.getLocation(), currentDistanceToEnemyTower)) {
        currentDistanceToEnemyTower = Robot.rc.getLocation().distanceSquaredTo(info.getLocation());
      }
      infos[0].updateRobotInfo(info);
      infos[1].updateRobotInfo(info);
      infos[2].updateRobotInfo(info);
      infos[3].updateRobotInfo(info);
      infos[4].updateRobotInfo(info);
      infos[5].updateRobotInfo(info);
      infos[6].updateRobotInfo(info);
      infos[7].updateRobotInfo(info);
      infos[8].updateRobotInfo(info);
    }

    // Choose the best square
    MicroInfo bestMicro = infos[8];
    if (infos[0].isBetterThan(bestMicro)) { bestMicro = infos[0]; }
    if (infos[1].isBetterThan(bestMicro)) { bestMicro = infos[1]; }
    if (infos[2].isBetterThan(bestMicro)) { bestMicro = infos[2]; }
    if (infos[3].isBetterThan(bestMicro)) { bestMicro = infos[3]; }
    if (infos[4].isBetterThan(bestMicro)) { bestMicro = infos[4]; }
    if (infos[5].isBetterThan(bestMicro)) { bestMicro = infos[5]; }
    if (infos[6].isBetterThan(bestMicro)) { bestMicro = infos[6]; }
    if (infos[7].isBetterThan(bestMicro)) { bestMicro = infos[7]; }

    Robot.rc.setIndicatorLine(Robot.rc.getLocation(), Robot.rc.getLocation().add(bestMicro.dir), 0, 0, 0);

    return bestMicro.dir;
  }

  private static class MicroInfo {
    // Initialized on construction
    Direction dir;
    MapLocation location;
    boolean canMove;

    // Set by updateMapInfo
    PaintType paint;

    // Set by updateRobotInfo
    int enemyTowersCanAttack = 0;
    int adjacentFriendlyUnits = 0;
    int distanceToClosestEnemyTower = MapData.MAX_DISTANCE_SQ;

    private MicroInfo(Direction dir) {
      this.dir = dir;
      this.location = Robot.rc.getLocation().add(dir);
      this.canMove = (dir == Direction.CENTER || Robot.rc.canMove(dir));
    }

    private void updateMapInfo(MapInfo info) {
      if (!canMove) { return; }

      this.paint = info.getPaint();
    }

    private void updateRobotInfo(RobotInfo info) {
      if (!canMove) { return; }

      if (info.getType().isTowerType() && info.getTeam() == Robot.OPPONENT) {
        if (location.isWithinDistanceSquared(info.getLocation(), info.getType().actionRadiusSquared)) {
          ++this.enemyTowersCanAttack;
        }
        if (location.isWithinDistanceSquared(info.getLocation(), distanceToClosestEnemyTower)) {
          distanceToClosestEnemyTower = location.distanceSquaredTo(info.getLocation());
        }
      }

      if (info.getTeam() == Robot.TEAM) {
        if (location.isAdjacentTo(info.getLocation())) { ++this.adjacentFriendlyUnits; }
      }
    }

    private boolean isBetterThan(MicroInfo other) throws GameActionException {
      /**
       * BY DEFAULT, WE ASSUME OTHER IS BETTER BASED ON HOW IT IS CALLED IN `Micro.getMove()`
       * 
       * Otherwise we do unnecessary swapping
       */

      // Need to be able to move to the square
      if (!this.canMove) { return false; }
      if (!other.canMove) { return true; }

      // Don't go adjacent to a tower
      if (other.distanceToClosestEnemyTower < 3) { return true; }
      if (this.distanceToClosestEnemyTower < 3) { return false; }

      // Soldier Attacking tower, want to be in range of a single tower
      if (Robot.rc.getType() == UnitType.SOLDIER &&  // We are a soldier
          GoalManager.current().type == Goal.Type.FIGHT_TOWER &&  // We are fighting a tower
          Robot.rc.isActionReady() && // We are ready to attack
          (currentDistanceToEnemyTower <= UnitType.SOLDIER.actionRadiusSquared || // Already in range
           (Robot.rc.getRoundNum() % 2 == 0 && (visibleFriendlySoldiers > 1 || Robot.rc.getHealth() > 30))) // or an even round number and with friend
         ) {
        /*
           IMPORTANT: This is more complicated if action radius is different

           Since this is soldier, we don't have to worry about it
         */
        // If we are in range and can attack, DON'T MOVE TO ANOTHER SPOT IN RANGE
        if (currentDistanceToEnemyTower <= UnitType.SOLDIER.actionRadiusSquared) {
          if (this.dir == Direction.CENTER) { return true; }
          if (other.dir == Direction.CENTER) { return false; }
        }
        if (this.enemyTowersCanAttack == 0 && other.enemyTowersCanAttack == 1) { return false; } 
        if (other.enemyTowersCanAttack == 0 && this.enemyTowersCanAttack == 1) { return true; }
      } 

      // In general we want to avoid towers
      if (this.enemyTowersCanAttack < other.enemyTowersCanAttack) { return true; }
      if (other.enemyTowersCanAttack < this.enemyTowersCanAttack) { return false; }

      // If we are attacking but still out of range, we want to get close to the tower
      if (Robot.rc.getType() == UnitType.SOLDIER && // We are a soldier
          GoalManager.current().type == Goal.Type.FIGHT_TOWER && // We are fighting a tower
          enemyTowersCanAttack == 0 // Neither choice can be attacked
         ) {
        if (this.distanceToClosestEnemyTower < other.distanceToClosestEnemyTower) { return true; }
        if (other.distanceToClosestEnemyTower < this.distanceToClosestEnemyTower) { return false; }
      }

      // Minimize paint penalty
      int thisPenalty = getPaintPenalty();
      int otherPenalty = other.getPaintPenalty();
      if (thisPenalty < otherPenalty) { return true; }
      if (otherPenalty < thisPenalty) { return false; }

      // Maximize distance from tower
      if (this.distanceToClosestEnemyTower < other.distanceToClosestEnemyTower) { return false; }
      if (other.distanceToClosestEnemyTower < this.distanceToClosestEnemyTower) { return true; }

      // Default to the current one
      return false;
    }

    /**
     * Robots lose 1 paint on neutral, 2 on enemy, plus 1 for each acjacent friend
     */
    private int getPaintPenalty() {
      return (this.paint.isAlly() ? 0 : 1) + (this.paint.isEnemy() ? 1 : 0) + adjacentFriendlyUnits;
    }
  };
};
