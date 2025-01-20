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

      //find the home tower (might have trouble if soldier is in spawn-range of both starting tower)
      for(RobotInfo robot : nearbyRobots){
        if(robot.getType().isTowerType() && robot.getLocation().distanceSquaredTo(rc.getLocation())<= GameConstants.BUILD_ROBOT_RADIUS_SQUARED){
          spawnTower = robot;
        }
      }
      MapLocation correctFirstSoldier = null;
      for(RobotInfo robot : nearbyRobots){
        if(rc.getRoundNum()>2&&robot.getType()==UnitType.SOLDIER && //TODO: change the roundNum threshold once we don't overflow on bytecode turn 1
        (correctFirstSoldier==null || spawnTower.getLocation().distanceSquaredTo(robot.getLocation()) < spawnTower.getLocation().distanceSquaredTo(correctFirstSoldier))){
          followID = robot.getID();
          correctFirstSoldier = robot.getLocation();
        }
      }
    }
  }

  protected void doMicro() throws GameActionException {
    //surviving takes all precendent over everything
    if(GoalManager.current().type == Goal.Type.SURVIVE){
      survive();
      rc.setIndicatorString("SURVIVE");
      return;
    }
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
    if(!initialSoldiers || GoalManager.current().type == Goal.Type.SURVIVE){  //initial soldier and SURVIVE soldiers shouldn't waste paint
      Painter.paint();
    }
  }

  private void opening() throws GameActionException {
    boolean isExploring = GoalManager.current().type == Goal.Type.CAPTURE_RUIN; //note that the for loop can set the GoalManager, I want to remember what it had before we enter the for loop
    MapLocation closestRuin = null;
    // Update any close ruins sites
    for (MapLocation ruin : rc.senseNearbyRuins(-1)) {
      MapData.updateData(rc.senseMapInfo(ruin));
      RobotInfo tower = rc.senseRobotAtLocation(ruin);
      if(tower!=null){
        if(tower.getTeam()==rc.getTeam().opponent()){
          //enemy tower, attack!
          GoalManager.pushGoal(Goal.Type.FIGHT_TOWER,tower.getLocation());
          goalTower = tower;
          //if we're attacking a tower, assume that's all we're doing (we don't care about any other opening logic)
          break;
        }else{
          //ally tower (probably the spawn tower), we don't care
          continue;
        }
      }
      if(isExploring) continue;
      if(MapData.isContested(ruin)) continue; //ignore any ruins with enemy paint that we've already seen
      if(closestRuin == null || rc.getLocation().distanceSquaredTo(ruin)<rc.getLocation().distanceSquaredTo(closestRuin)){
        closestRuin = ruin;
        GoalManager.pushGoal(Goal.Type.CAPTURE_RUIN,ruin);
      }else if(!GoalManager.contains(Goal.Type.CAPTURE_RUIN,ruin)){
        GoalManager.pushSecondaryGoal(Goal.Type.CAPTURE_RUIN,ruin);
      }
    }

    // if we have a ruin in sight, look for enemy paint around it
    // we use a while loop so we can calculate multiple ruins at once (if possible)
    while(GoalManager.current().type == Goal.Type.CAPTURE_RUIN){
      MapInfo[] towerPatternTiles = rc.senseNearbyMapInfos(GoalManager.current().target,8);
      boolean ruinGood = true;
      for(MapInfo m : towerPatternTiles){
        if(m.getPaint().isEnemy()){
          MapData.setContested(GoalManager.current().target);
          //remove from GoalManager (we never want to go to a contested ruin in the opening)
          GoalManager.popGoal();
          ruinGood = false;
          break;
        }
      }
      if(ruinGood || Clock.getBytecodesLeft() < 500) break;
    }
    

    if(GoalManager.current().type == Goal.Type.FIGHT_TOWER){
      MapLocation enemyTower = GoalManager.current().target;
      //goalTower is a class variable set in the ruin-scanning for loop (only set to enemy towers in opening())
      Painter.paintFight(goalTower);
      rc.setIndicatorDot(enemyTower,0,255,0);
      if(rc.canSenseLocation(enemyTower)&&!rc.canSenseRobotAtLocation(enemyTower)){
        //if we've killed the enemy tower
        //set to survive mode
        initialSoldiers=false;  //we can be done with the opening if we successfully kill a tower
        GoalManager.setNewGoal(Goal.Type.SURVIVE,rc.getLocation());
        survive();
      }
    }else if(GoalManager.current().type == Goal.Type.CAPTURE_RUIN){
      MapLocation targetRuin = GoalManager.current().target;
      if (Painter.paintCaptureRuin() || (rc.canSenseLocation(targetRuin) && rc.senseRobotAtLocation(targetRuin)!=null)) {
        initialSoldiers=false;  //we can be done with the opening if we successfully capture the first tower
        GoalManager.setNewGoal(Goal.Type.REFILL_PAINT, targetRuin);
      }
    }else{ //explore
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
  private void survive(){
    //only 3 main priorities: stay on allied paint and stay away from enemy towers (and allied robots)
    //secondary priorities: make sure every ruin we see has at least 1 of our paint around it, try to navigate back to an allied tower to refill paint (without dying)
    

  }
}
