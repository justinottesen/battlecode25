package sprint_1.robots;

import battlecode.common.*;
import sprint_1.managers.MicroManager;
import sprint_1.managers.PaintManager;
import sprint_1.utils.Explore;
import sprint_1.utils.Pathfinding;

public class Mopper extends Robot {

  MicroManager microManager;
  Pathfinding pathfinding;
  Explore explore;

  public Mopper(RobotController rc) {
    super(rc);

    microManager = new MicroManager(rc);
    pathfinding = new Pathfinding();
    explore = new Explore();
  }

  @Override
  public void run() throws GameActionException {
    if(Robot.rc.getPaint()<PaintManager.PAINT_THRESHOLD){   //refill if we need it
      MapLocation homeTower = mapData.getClosestTower();
      PaintManager.refill(homeTower);
      if(homeTower!=null && Robot.rc.getLocation().distanceSquaredTo(homeTower)>2){
          pathfinding.moveTo(homeTower);
      }
      //rc.setIndicatorString("Refilling to "+homeTower);
    }
    microManager.doMicro();

    if(Clock.getBytecodesLeft()>5000){ //assuming we move like every other turn or micro doesn't always run, we can update mapdata
      mapData.checkAll();
      //this is so moppers don't overflow on bytecode too easily
    }

    if(rc.isMovementReady()){
      pathfinding.moveTo(explore.getExploreTarget());
    }

    if (rc.isActionReady()){
      Direction mopSwingDirection = pickMopSwingDirection();
      if(mopSwingDirection!=Direction.CENTER && rc.canMopSwing(mopSwingDirection)) rc.mopSwing(mopSwingDirection);
    }

    // Mop tiles if possible
    if (rc.isActionReady()) {
      MapInfo[] mapInfos = rc.senseNearbyMapInfos(GameConstants.MARK_RADIUS_SQUARED); 
      for (MapInfo info : mapInfos) {
        if (info.getPaint() == PaintType.ENEMY_PRIMARY || info.getPaint() == PaintType.ENEMY_SECONDARY) {
          rc.attack(info.getMapLocation());
          break;
        }
      }
    }

    // Transfer paint if possible (and don't kill itself)
    if (rc.isActionReady() && rc.getPaint()>50) {
      RobotInfo[] robotInfos = rc.senseNearbyRobots(GameConstants.PAINT_TRANSFER_RADIUS_SQUARED);
      for (RobotInfo robot : robotInfos) {
        // Don't transfer to other moppers or towers
        if (robot.type == UnitType.MOPPER || robot.getType().isTowerType()) { continue; }

        // Transfer the max possible
        int transfer_amt = Math.min(robot.getType().paintCapacity - robot.getPaintAmount(), rc.getPaint()-50);
        if (rc.canTransferPaint(robot.getLocation(), transfer_amt)) {
          rc.transferPaint(robot.getLocation(), transfer_amt);
          break;
        }
      }
    }
  }

  private Direction pickMopSwingDirection() throws GameActionException{
    RobotInfo[] enemyRobotsWithinMopSwingRange = rc.senseNearbyRobots(2,rc.getTeam().opponent());
    if(enemyRobotsWithinMopSwingRange.length==0)  return Direction.CENTER;
    Direction[] cardinaDirections = Direction.cardinalDirections();

    int mostEnemiesHit = 0;
    Direction chosenDirection = Direction.CENTER;
    for(Direction d : cardinaDirections){
      int directionEnemiesHit = 0;
      for(RobotInfo enemy : enemyRobotsWithinMopSwingRange){
        Direction dirToEnemy = rc.getLocation().directionTo(enemy.getLocation());
        if(dirToEnemy.dx==d.dx || dirToEnemy.dy==d.dy) ++directionEnemiesHit;
      }
      if(directionEnemiesHit>mostEnemiesHit){
        mostEnemiesHit=directionEnemiesHit;
        chosenDirection=d;
      }
    }
    return chosenDirection;
  }

};