package sprint_2;

import battlecode.common.*;
import sprint_2.util.*;

public final class Soldier extends Robot {

  // Utility Classes
  private final Pathfinding pathfinding;
  private final Painter painter;

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD;

  // Possible goal values, ordered by priority number (higher is more important)
  public enum Goal {
    EXPLORE(0),
    CAPTURE_SRP(1),
    CAPTURE_RUIN(2),
    FIGHT_TOWER(3),
    REFILL_PAINT(4);
    
    public final int val;
    
    Goal(int val_) {
      val = val_;
    }
    
  }
  private Goal goal;

  // Other goal helpers
  RobotInfo goalTower;

  //variables for hard coding the first 2 soldiers from each starting tower
  private boolean initialSoldiers; //true if this soldier was the 1st or 2nd soldier spawned from the starting paint/money towers
  private int followID; //2nd soldier spawned from the starting paint/money towers stores the 1st soldier's id (1st soldier store -1 here)
  private RobotInfo spawnTower; //true if this soldier is spawned from the starting paint tower


  public Soldier(RobotController rc_) throws GameActionException {
    super(rc_);
    
    painter = new Painter(rc, mapData);
    pathfinding = new Pathfinding(rc, mapData);
    goal = Goal.EXPLORE;
    pathfinding.setTarget(mapData.getExploreTarget());

    //set variables for hard coding the first 2 soldiers (initializeSoldiers, followID, spawnedFromPaintTower)
    initialSoldiers = (rc.getRoundNum()<10);
    followID=-1;
    if(initialSoldiers){
      RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
      for(RobotInfo robot : nearbyRobots){
        if(robot.getType().isTowerType() && robot.getLocation().distanceSquaredTo(rc.getLocation())< GameConstants.BUILD_ROBOT_RADIUS_SQUARED){
          spawnTower = robot;
        }else if(rc.getRoundNum()>2&&robot.getType()==UnitType.SOLDIER){  //TODO: change the roundNum threshold once we don't overflow on bytecode turn 1
          followID = robot.getID();
        }
      }
    }
  }

  protected void doMicro() throws GameActionException {
    //hard code the opening (ignores the rest of the function for now)
    if(initialSoldiers){
      rc.setIndicatorString("INITIAL SOLDIERS");
      opening();
      return;
    }
    rc.setIndicatorString("GOAL - " + switch (goal) {
      case Goal.EXPLORE -> "EXPLORE";
      case Goal.CAPTURE_SRP -> "CAPTURE_SRP";
      case Goal.CAPTURE_RUIN -> "CAPTURE_RUIN"; 
      case Goal.FIGHT_TOWER -> "FIGHT_TOWER"; 
      case Goal.REFILL_PAINT -> "REFILL_PAINT";
    });
    if (pathfinding.getTarget() != null) {
      rc.setIndicatorLine(rc.getLocation(), pathfinding.getTarget(), 255, 0, 255);
    }

    // TODO: TAKE THIS OUT AND DEAL WITH CROWDING lol
    // If surrounded, kill yourself
    if (rc.senseNearbyRobots(9, TEAM).length > 24 /* Out of 28 */) {
      rc.disintegrate();
      return;
    }

    // UPDATE GOAL ------------------------------------------------------------

    // Update any close ruins sites (skip the first few rounds to save bytecode)
    if (rc.getRoundNum() > 10) {
      for (MapLocation ruin : rc.senseNearbyRuins(-1)) {
        mapData.updateData(rc.senseMapInfo(ruin));
      }
    }

    // Check for a suicide message, if received this is priority number 1
    Message[] messages = rc.readMessages(rc.getRoundNum() - 1);
    for (Message m : messages) {
      if (comms.getMessageType(m.getBytes()) == comms.SUICIDE) {
        System.out.println("Received suicide message");
        goal = Goal.CAPTURE_RUIN;
        pathfinding.setTarget(comms.getCoordinates(m.getBytes()));
        painter.paintCaptureRuin(pathfinding);
      }
    }

    // Check if someone else finished the current ruin
    // TODO: Should this check stay here? Duplicated in painter
    if (goal == Goal.CAPTURE_RUIN) {
      if (rc.canSenseRobotAtLocation(pathfinding.getTarget())) {
        mapData.updateData(rc.senseMapInfo(pathfinding.getTarget()));
        goal = Goal.REFILL_PAINT;
        pathfinding.setTarget(mapData.closestFriendlyTower());
      }
    }

    // Check if someone else finished the current SRP
    // TODO: Should this check stay here? Duplicated in painter
    if (goal == Goal.CAPTURE_SRP) {
      MapLocation target = pathfinding.getTarget();
      if (rc.canSenseLocation(target) && rc.senseMapInfo(target).isResourcePatternCenter()) {
        mapData.updateData(rc.senseMapInfo(target));
        goal = Goal.EXPLORE;
        pathfinding.setTarget(mapData.getExploreTarget());
      }
    }

    // If received paint transfer from mopper, update goal
    if (goal == Goal.REFILL_PAINT && rc.getPaint() > REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = Goal.EXPLORE;
      pathfinding.setTarget(mapData.getExploreTarget());
    }
    
    // If low on paint, set goal to refill
    if (goal != Goal.REFILL_PAINT && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      goal = Goal.REFILL_PAINT;
      pathfinding.setTarget(mapData.closestFriendlyTower());
    }

    // Look for nearby ruins if we aren't already fighting a tower
    if (goal.val < Goal.FIGHT_TOWER.val) {
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        RobotInfo info = rc.senseRobotAtLocation(ruin);
        if (info == null) { // Unclaimed Ruin
          if (goal.val >= Goal.CAPTURE_RUIN.val) { continue; }
          goal = Goal.CAPTURE_RUIN;
          pathfinding.setTarget(ruin);
          continue;
        }
        if (info.getTeam() == OPPONENT) { // Enemy Tower
          goal = Goal.FIGHT_TOWER;
          goalTower = info;
          pathfinding.setTarget(ruin);
          break;
        }
      }
    }

    // Look for SRP if we are a lower priority
    // TODO: Make this process more intelligent. Pack better with other SRPs, etc
    if (goal.val < Goal.CAPTURE_SRP.val) {
      for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), GameConstants.MARK_RADIUS_SQUARED)) {
        if (mapData.tryMarkSRP(loc)) {
          goal = Goal.CAPTURE_SRP;
          pathfinding.setTarget(loc);
          break;
        }
      }
    }

    // DO THINGS --------------------------------------------------------------

    // Can't do anything, no point
    if (!rc.isMovementReady() && !rc.isActionReady()) { return; }

    // Can't move, might as well try and paint
    if (!rc.isMovementReady() && rc.isActionReady()) { painter.paint(); return; }

    switch (goal) {
      case FIGHT_TOWER:
        painter.paintFight(goalTower, pathfinding);
        break;
      case CAPTURE_SRP:
        if (painter.paintCaptureSRP(pathfinding)) {
          goal = Goal.EXPLORE;
          pathfinding.setTarget(mapData.getExploreTarget());
        }
        break;
      case CAPTURE_RUIN:
        if (painter.paintCaptureRuin(pathfinding)) {
          goal = Goal.REFILL_PAINT;
        }
        // Pathfinding target is the tower which was just built, should have paint
        break;
      case REFILL_PAINT:
        if (rc.getLocation().isWithinDistanceSquared(pathfinding.getTarget(), GameConstants.PAINT_TRANSFER_RADIUS_SQUARED)) {
          RobotInfo tower = rc.senseRobotAtLocation(pathfinding.getTarget());
          if (tower == null) {
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, pathfinding.getTarget())) {
              rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, pathfinding.getTarget());
              tower = rc.senseRobotAtLocation(pathfinding.getTarget());
            } else {
              pathfinding.setTarget(mapData.closestFriendlyTower());
              return;
            }
          }
          int paintAmount = rc.getType().paintCapacity - rc.getPaint();
          if (tower.getPaintAmount() < paintAmount) { paintAmount = tower.getPaintAmount(); }
          if (rc.canTransferPaint(pathfinding.getTarget(), -paintAmount)) {
            rc.transferPaint(pathfinding.getTarget(), -paintAmount);
            goal = Goal.EXPLORE;
            pathfinding.setTarget(mapData.getExploreTarget());
          }
        }
        break;
      case EXPLORE: // TODO: Address clumping of units
        if (rc.getLocation().isWithinDistanceSquared(pathfinding.getTarget(), GameConstants.VISION_RADIUS_SQUARED)) {
          mapData.updateData(rc.senseMapInfo(pathfinding.getTarget()));
          pathfinding.setTarget(mapData.getExploreTarget());
        }
        break;
      default: break;
    }
  }

  protected void doMacro() throws GameActionException {
    if (rc.isMovementReady()) {
      Direction dir = pathfinding.getMove();
      if (dir == null) {
        System.out.println("Pathfinding returned null dir");
      } else if (rc.canMove(dir)) {
        mapData.move(dir);
        if (mapData.foundSRP != null && goal.val < Goal.CAPTURE_SRP.val) {
          goal = Goal.CAPTURE_SRP;
          pathfinding.setTarget(mapData.foundSRP);
        }
      }
    }
    if(!initialSoldiers){  //initial soldier shouldn't waste paint
      painter.paint();
    }
  }

  private void opening() throws GameActionException {
<<<<<<< HEAD
=======
    //combat takes first priority

>>>>>>> 371ecebfa88c8294e3f16264cb83dcd29bbb4d0f
    MapLocation closestRuin = null;
    // Update any close ruins sites
    for (MapLocation ruin : rc.senseNearbyRuins(-1)) {
      mapData.updateData(rc.senseMapInfo(ruin));
<<<<<<< HEAD
      RobotInfo tower = rc.senseRobotAtLocation(ruin);
      if(tower!=null){
        if(tower.getTeam()==rc.getTeam().opponent()){
          //enemy tower, attack!
          painter.paintFight(tower, pathfinding);
          rc.setIndicatorDot(ruin,255,0,0);
          return;
        }else{
          //ally tower (probably the spawn tower), we don't care
          continue;
        }
      }
      if(mapData.isContested(ruin)) continue; //ignore any ruins with enemy paint that we've already seen
=======
      if(mapData.isContested(ruin)||rc.senseRobotAtLocation(ruin)!=null) continue;  //ignore any ruins with enemy paint that we've already seen (also ignores our starting tower)
>>>>>>> 371ecebfa88c8294e3f16264cb83dcd29bbb4d0f
      if(closestRuin == null || rc.getLocation().distanceSquaredTo(ruin)<rc.getLocation().distanceSquaredTo(closestRuin)){
        closestRuin = ruin;
      }
    }

    boolean ruinGood = (closestRuin != null);
    // if we have a ruin in sight, look for enemy paint around it
    if(closestRuin != null){
      MapInfo[] towerPatternTiles = rc.senseNearbyMapInfos(closestRuin,8);
      for(MapInfo m : towerPatternTiles){
        if(m.getPaint().isEnemy()){
          mapData.setContested(closestRuin);
          ruinGood = false;
          break;
        }
      }
    }

    //ruinGood is only true if there exists a closest ruin and the closest ruin doesn't have any enemy paint visible
    if(ruinGood){
      pathfinding.setTarget(closestRuin);
      if (painter.paintCaptureRuin(pathfinding)) {
        initialSoldiers=false;  //we can be done with the opening if we successfully capture the first tower
        goal = Goal.REFILL_PAINT;
        pathfinding.setTarget(mapData.closestFriendlyTower());
      }
    }else{
      //roam if there's no ruin in sight
      if(followID!=-1 && rc.canSenseRobot(followID)){ //second soldier
        RobotInfo firstSoldier = rc.senseRobot(followID);
        pathfinding.setTarget(firstSoldier.getLocation());
        rc.setIndicatorString("second soldier, followid: "+followID);
      }else{
        MapLocation closestEnemyTower = mapData.closestEnemyTower();
        if(closestEnemyTower!=null){
          pathfinding.setTarget(closestEnemyTower);
        }else{
          int[] symmetryPriority = new int[3];
          //we want the soldiers from each tower to assume different symmetries
          if(spawnTower.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER){
            symmetryPriority[0] = 0b010;  //horizontal
            symmetryPriority[1] = 0b001;  //rotational
            symmetryPriority[2] = 0b100;  //vertical
          }else{
            symmetryPriority[0] = 0b100;  //vertical
            symmetryPriority[1] = 0b001;  //rotational
            symmetryPriority[2] = 0b010;  //horizontal
          }

          //Guess the location of the enemy tower and choose it as our target
          MapLocation guessEnemyTower = mapData.symmetryLoc(spawnTower.getLocation(),symmetryPriority[0]);

          if(mapData.known(guessEnemyTower)){
            guessEnemyTower = mapData.symmetryLoc(spawnTower.getLocation(),symmetryPriority[1]);
            if(mapData.known(guessEnemyTower)){
              guessEnemyTower = mapData.symmetryLoc(spawnTower.getLocation(),symmetryPriority[2]);
              if(mapData.known(guessEnemyTower)){
                guessEnemyTower = null;
                
              }
            }
          }

          if(guessEnemyTower!=null){
            pathfinding.setTarget(guessEnemyTower);
          }else{
            //this should never run, since it rules out all 3 symmetries, but if it does, default to normal exploration
            System.out.println("Initial soldiers ruled out all 3 symmetries???");
            pathfinding.setTarget(mapData.getExploreTarget());
          }
        }
      }
      
    }
  }
}
