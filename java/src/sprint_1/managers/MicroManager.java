package sprint_1.managers;

import battlecode.common.*;

public class MicroManager {

  RobotController rc;

  MicroInfo[] microInfos;
  
  public MicroManager(RobotController rc_) {
    rc = rc_;
  }

  /*
   * Makes an informed decision about movement
   */
  public boolean doMicro() throws GameActionException {
    // Can't do anything if on cooldown
    if (!rc.isMovementReady()) { return false; }

    RobotInfo[] units = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    if (units.length == 0) return false;
    
    // Allocate info for 3x3 grid
    microInfos = new MicroInfo[9];
    microInfos[0] = new MicroInfo(Direction.NORTH);
    microInfos[1] = new MicroInfo(Direction.NORTHEAST);
    microInfos[2] = new MicroInfo(Direction.EAST);
    microInfos[3] = new MicroInfo(Direction.SOUTHEAST);
    microInfos[4] = new MicroInfo(Direction.SOUTH);
    microInfos[5] = new MicroInfo(Direction.SOUTHWEST);
    microInfos[6] = new MicroInfo(Direction.WEST);
    microInfos[7] = new MicroInfo(Direction.NORTHWEST);
    microInfos[8] = new MicroInfo(Direction.CENTER);

    // Check for enemies and allies
    RobotInfo[] robots = rc.senseNearbyRobots();
    for (RobotInfo robot : robots) {
      microInfos[0].updateRobot(robot);
      microInfos[1].updateRobot(robot);
      microInfos[2].updateRobot(robot);
      microInfos[3].updateRobot(robot);
      microInfos[4].updateRobot(robot);
      microInfos[5].updateRobot(robot);
      microInfos[6].updateRobot(robot);
      microInfos[7].updateRobot(robot);
      microInfos[8].updateRobot(robot);
    }

    // Check nearby map info
    MapInfo[] mapInfos = rc.senseNearbyMapInfos();
    for (MapInfo mapInfo : mapInfos) {
      microInfos[0].updateMapInfo(mapInfo);
      microInfos[1].updateMapInfo(mapInfo);
      microInfos[2].updateMapInfo(mapInfo);
      microInfos[3].updateMapInfo(mapInfo);
      microInfos[4].updateMapInfo(mapInfo);
      microInfos[5].updateMapInfo(mapInfo);
      microInfos[6].updateMapInfo(mapInfo);
      microInfos[7].updateMapInfo(mapInfo);
      microInfos[8].updateMapInfo(mapInfo);
    }
    
    // Pick best direction
    MicroInfo bestMicro = microInfos[0];
    if (microInfos[1].isBetterThan(bestMicro)) { bestMicro = microInfos[1]; }
    if (microInfos[2].isBetterThan(bestMicro)) { bestMicro = microInfos[2]; }
    if (microInfos[3].isBetterThan(bestMicro)) { bestMicro = microInfos[3]; }
    if (microInfos[4].isBetterThan(bestMicro)) { bestMicro = microInfos[4]; }
    if (microInfos[5].isBetterThan(bestMicro)) { bestMicro = microInfos[5]; }
    if (microInfos[6].isBetterThan(bestMicro)) { bestMicro = microInfos[6]; }
    if (microInfos[7].isBetterThan(bestMicro)) { bestMicro = microInfos[7]; }
    if (microInfos[8].isBetterThan(bestMicro)) { bestMicro = microInfos[8]; }

    if (bestMicro.dir == Direction.CENTER) { return true; }
    
    if (bestMicro.canMove) {  
      rc.move(bestMicro.dir);
      return true;
    }

    return false;
  }

  class MicroInfo {
    Direction dir;
    MapLocation location;
    boolean canMove;
    PaintType paintType;

    int minEnemyDist = Math.max(GameConstants.MAP_MAX_HEIGHT * GameConstants.MAP_MAX_HEIGHT, GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_WIDTH);
    int minAllyDist = minEnemyDist;

    int minEnemyPaintDist = minEnemyDist;

    public MicroInfo(Direction dir) throws GameActionException {
      this.dir = dir;
      this.location = rc.getLocation().add(dir);
      this.canMove = !(dir != Direction.CENTER && !rc.canMove(dir));
      MapInfo info = rc.senseMapInfo(this.location);
      paintType = info.getPaint();
    }

    void updateRobot(RobotInfo robot) {
      int dist = robot.getLocation().distanceSquaredTo(location);
      // Ignore Towers for now
      if (robot.getType().isTowerType()) { return; }
      if (robot.getTeam() == rc.getTeam()) {
        if (dist < minAllyDist) { minAllyDist = dist; }
      } else {
        if (dist < minEnemyDist) { minEnemyDist = dist; }
      }
    }

    void updateMapInfo(MapInfo mapInfo) {
      // Only care about enemy paint for now
      if (mapInfo.getPaint().isAlly() || mapInfo.getPaint() == PaintType.EMPTY) { return; }
      int dist = mapInfo.getMapLocation().distanceSquaredTo(location);
      if (dist < minEnemyPaintDist) { minEnemyPaintDist = dist; }
    }

    boolean isBetterThan(MicroInfo other) {

      // Prefer spaces which can actually be moved to
      if (this.canMove && !other.canMove) { return true; }
      if (!this.canMove && other.canMove) { return false; }

      // Move towards enemy paint (but not onto it)
      if (this.minEnemyPaintDist < other.minEnemyPaintDist && this.minEnemyPaintDist > 0) { return true; }
      if (this.minEnemyPaintDist > other.minEnemyPaintDist && other.minEnemyPaintDist > 0) { return false; }

      // Move towards friends
      if (this.minAllyDist < other.minAllyDist) { return true; }
      if (this.minAllyDist > other.minAllyDist) { return false; }
      
      // Paint Type
      if (!paintType.equals(other.paintType)) {
        // Prefer Friendly paint over others
        if (this.paintType.isAlly() && !other.paintType.isAlly()) { return true; }
        if (!this.paintType.isAlly() && other.paintType.isAlly()) { return false; }
        
        // Prefer Neutral paint over enemy
        return this.paintType.equals(PaintType.EMPTY);
      }
      
      // Prefer not moving over moving
      if (this.dir == Direction.CENTER) { return true; }
      if (other.dir == Direction.CENTER) { return false; }

      return false;
    }
  }
}
