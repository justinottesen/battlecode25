package sprint_2;

import battlecode.common.*;
import sprint_2.util.*;

public final class Soldier extends Robot {

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD;

  // Other goal helpers
  RobotInfo goalTower;

  //variables for hard coding the first 2 soldiers from each starting tower
  private boolean initialSoldiers; //true if this soldier was the 1st or 2nd soldier spawned from the starting paint/money towers
  private int followID; //2nd soldier spawned from the starting paint/money towers stores the 1st soldier's id (1st soldier store -1 here)
  private RobotInfo spawnTower; //true if this soldier is spawned from the starting paint tower


  public Soldier(RobotController rc_) throws GameActionException {
    super(rc_);
    
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
    rc.setIndicatorString("GOAL - " + GoalManager.current());
    if (Pathfinding.getTarget() != null) {
      rc.setIndicatorLine(rc.getLocation(), Pathfinding.getTarget(), 255, 0, 255);
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
        MapData.updateData(rc.senseMapInfo(ruin));
      }
    }

    // Check for a suicide message, if received this is priority number 1
    Message[] messages = rc.readMessages(rc.getRoundNum() - 1);
    for (Message m : messages) {
      if (Communication.getMessageType(m.getBytes()) == Communication.SUICIDE) {
        System.out.println("Received suicide message");
        GoalManager.pushGoal(Goal.Type.CAPTURE_RUIN, Communication.getCoordinates(m.getBytes()));
        Painter.paintCaptureRuin();
      }
    }

    // Check if someone else finished the current ruin
    // TODO: Should this check stay here? Duplicated in Painter
    if (GoalManager.current().type == Goal.Type.CAPTURE_RUIN) {
      if (rc.canSenseRobotAtLocation(Pathfinding.getTarget())) {
        MapData.updateData(rc.senseMapInfo(Pathfinding.getTarget()));
        GoalManager.setNewGoal(Goal.Type.REFILL_PAINT, Pathfinding.getTarget());
      }
    }

    // Check if someone else finished the current SRP
    // TODO: Should this check stay here? Duplicated in Painter
    if (GoalManager.current().type == Goal.Type.CAPTURE_SRP) {
      MapLocation target = Pathfinding.getTarget();
      if (rc.canSenseLocation(target) && rc.senseMapInfo(target).isResourcePatternCenter()) {
        MapData.updateData(rc.senseMapInfo(target));
        GoalManager.popGoal();
      }
    }

    // If received paint transfer from mopper, update goal
    if (GoalManager.current().type == Goal.Type.REFILL_PAINT && rc.getPaint() > REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      GoalManager.popGoal();
    }

    // TODO: High and low watermark for paint refill so we don't get stuck in a loop
    
    // If low on paint, set goal to refill
    if (GoalManager.current().type != Goal.Type.REFILL_PAINT && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      GoalManager.pushGoal(Goal.Type.REFILL_PAINT, Pathfinding.getTarget());
    }

    // Look for nearby ruins if we aren't already fighting a tower
    if (GoalManager.current().type.v < Goal.Type.FIGHT_TOWER.v) {
      boolean setGoal = false;
      MapLocation[] ruins = rc.senseNearbyRuins(-1);
      for (MapLocation ruin : ruins) {
        RobotInfo info = rc.senseRobotAtLocation(ruin);
        if (info == null) { // Unclaimed Ruin
          if (GoalManager.current().type.v >= Goal.Type.CAPTURE_RUIN.v) { continue; }
          GoalManager.pushGoal(Goal.Type.CAPTURE_RUIN, ruin);
          setGoal = true;
          continue;
        }
        if (info.getTeam() == OPPONENT) { // Enemy Tower
          if (setGoal) {
            GoalManager.replaceTopGoal(Goal.Type.FIGHT_TOWER, ruin);
          } else {
            GoalManager.pushGoal(Goal.Type.FIGHT_TOWER, ruin);
          }
          goalTower = info;
          break;
        }
      }
    }

    // Look for SRP if we are a lower priority
    // TODO: Make this process more intelligent. Pack better with other SRPs, etc
    if (GoalManager.current().type.v < Goal.Type.CAPTURE_SRP.v) {
      for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), GameConstants.MARK_RADIUS_SQUARED)) {
        if (MapData.tryMarkSRP(loc)) {
          GoalManager.pushGoal(Goal.Type.CAPTURE_SRP, loc);
          break;
        }
      }
    }

    // DO THINGS --------------------------------------------------------------

    // Can't do anything, no point
    if (!rc.isMovementReady() && !rc.isActionReady()) { return; }

    // Can't move, might as well try and paint
    if (!rc.isMovementReady() && rc.isActionReady()) { Painter.paint(); return; }

    switch (GoalManager.current().type) {
      case FIGHT_TOWER:
        Painter.paintFight(goalTower);
        break;
      case CAPTURE_SRP:
        if (Painter.paintCaptureSRP()) {
          GoalManager.popGoal();
        }
        break;
      case CAPTURE_RUIN:
        if (Painter.paintCaptureRuin()) {
          GoalManager.replaceTopGoal(Goal.Type.REFILL_PAINT, Pathfinding.getTarget());
        }
        // Pathfinding target is the tower which was just built, should have paint
        break;
      case REFILL_PAINT:
        if (rc.getLocation().isWithinDistanceSquared(Pathfinding.getTarget(), GameConstants.PAINT_TRANSFER_RADIUS_SQUARED)) {
          RobotInfo tower = rc.senseRobotAtLocation(Pathfinding.getTarget());
          if (tower == null) {
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, Pathfinding.getTarget())) {
              rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, Pathfinding.getTarget());
              tower = rc.senseRobotAtLocation(Pathfinding.getTarget());
            } else {
              Pathfinding.setTarget(MapData.closestFriendlyTower());
              return;
            }
          }
          int paintAmount = rc.getType().paintCapacity - rc.getPaint();
          if (tower.getPaintAmount() < paintAmount) { paintAmount = tower.getPaintAmount(); }
          if (rc.canTransferPaint(Pathfinding.getTarget(), -paintAmount)) {
            rc.transferPaint(Pathfinding.getTarget(), -paintAmount);
            GoalManager.popGoal();
          }
        }
        break;
      case EXPLORE: // TODO: Address clumping of units
        if (rc.getLocation().isWithinDistanceSquared(Pathfinding.getTarget(), GameConstants.VISION_RADIUS_SQUARED)) {
          MapData.updateData(rc.senseMapInfo(Pathfinding.getTarget()));
          Pathfinding.setTarget(MapData.getExploreTarget());
        }
        break;
      default: break;
    }
  }

  protected void doMacro() throws GameActionException {
    if (rc.isMovementReady()) {
      Direction dir = Pathfinding.getMove();
      if (dir == null) {
        System.out.println("Pathfinding returned null dir");
      } else if (rc.canMove(dir)) {
        MapData.move(dir);
        if (MapData.foundSRP != null && GoalManager.current().type.v < Goal.Type.CAPTURE_SRP.v) {
          GoalManager.setNewGoal(Goal.Type.CAPTURE_SRP, MapData.foundSRP);
        }
      }
    }
    if(!initialSoldiers){  //initial soldier shouldn't waste paint
      Painter.paint();
    }
  }

  private void opening() throws GameActionException {
    // Combat takes first priority
    MapLocation closestRuin = null;
    // Update any close ruins sites
    for (MapLocation ruin : rc.senseNearbyRuins(-1)) {
      MapData.updateData(rc.senseMapInfo(ruin));
      RobotInfo tower = rc.senseRobotAtLocation(ruin);
      if(tower!=null){
        if(tower.getTeam()==rc.getTeam().opponent()){
          //enemy tower, attack!
          Painter.paintFight(tower);
          rc.setIndicatorDot(ruin,255,0,0);
          return;
        }else{
          //ally tower (probably the spawn tower), we don't care
          continue;
        }
      }
      if(MapData.isContested(ruin)) continue; //ignore any ruins with enemy paint that we've already seen
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
          MapData.setContested(closestRuin);
          ruinGood = false;
          break;
        }
      }
    }

    //ruinGood is only true if there exists a closest ruin and the closest ruin doesn't have any enemy paint visible
    if(ruinGood){
      Pathfinding.setTarget(closestRuin);
      if (Painter.paintCaptureRuin()) {
        initialSoldiers=false;  //we can be done with the opening if we successfully capture the first tower
        GoalManager.setNewGoal(Goal.Type.REFILL_PAINT, Pathfinding.getTarget());
      }
    }else{
      //roam if there's no ruin in sight
      if(followID!=-1 && rc.canSenseRobot(followID)){ //second soldier
        RobotInfo firstSoldier = rc.senseRobot(followID);
        Pathfinding.setTarget(firstSoldier.getLocation());
        rc.setIndicatorString("second soldier, followid: "+followID);
      }else{
        MapLocation closestEnemyTower = MapData.closestEnemyTower();
        if(closestEnemyTower!=null){
          Pathfinding.setTarget(closestEnemyTower);
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
          MapLocation guessEnemyTower = MapData.symmetryLoc(spawnTower.getLocation(),symmetryPriority[0]);

          if(MapData.known(guessEnemyTower)){
            guessEnemyTower = MapData.symmetryLoc(spawnTower.getLocation(),symmetryPriority[1]);
            if(MapData.known(guessEnemyTower)){
              guessEnemyTower = MapData.symmetryLoc(spawnTower.getLocation(),symmetryPriority[2]);
              if(MapData.known(guessEnemyTower)){
                guessEnemyTower = null;
                
              }
            }
          }

          if(guessEnemyTower!=null){
            Pathfinding.setTarget(guessEnemyTower);
          }else{
            //this should never run, since it rules out all 3 symmetries, but if it does, default to normal exploration
            System.out.println("Initial soldiers ruled out all 3 symmetries???");
            Pathfinding.setTarget(MapData.getExploreTarget());
          }
        }
      }
      
    }
  }
}
