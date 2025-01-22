package quals;

import battlecode.common.*;
import quals.util.*;

public final class Soldier extends Robot {

  // Constants
  private final int REFILL_PAINT_THRESHOLD = GameConstants.INCREASED_COOLDOWN_THRESHOLD;

  // Other goal helpers
  RobotInfo goalTower;

  //variables for hard coding the first 2 soldiers from each starting tower
  private boolean initialSoldiers; //true if this soldier was the 1st or 2nd soldier spawned from the starting paint/money towers
  private int followID; //2nd soldier spawned from the starting paint/money towers stores the 1st soldier's id (1st soldier store -1 here)
  private RobotInfo spawnTowerInfo; //true if this soldier is spawned from the starting paint tower


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
          spawnTowerInfo = robot;
        }
      }
      MapLocation correctFirstSoldier = null;
      for(RobotInfo robot : nearbyRobots){
        if(rc.getRoundNum()>2&&robot.getType()==UnitType.SOLDIER && //TODO: change the roundNum threshold once we don't overflow on bytecode turn 1
        (correctFirstSoldier==null || spawnTowerInfo.getLocation().distanceSquaredTo(robot.getLocation()) < spawnTowerInfo.getLocation().distanceSquaredTo(correctFirstSoldier))){
          followID = robot.getID();
          correctFirstSoldier = robot.getLocation();
        }
      }
    }
  }

  protected void doMicro() throws GameActionException {
    //surviving takes all precendent over everything
    if(GoalManager.current().type == Goal.Type.SURVIVE){
      rc.setIndicatorString("SURVIVE");
      survive();
      return;
    }
    //hard code the opening (ignores the rest of the function for now)
    if(initialSoldiers){
      rc.setIndicatorString("INITIAL SOLDIERS");
      opening();
      return;
    }
    rc.setIndicatorString("GOAL - " + GoalManager.current());
    if (GoalManager.current().target != null) {
      rc.setIndicatorLine(rc.getLocation(), GoalManager.current().target, 255, 0, 255);
    }

    // TODO: TAKE THIS OUT AND DEAL WITH CROWDING lol
    // If surrounded, kill yourself
    if (rc.senseNearbyRobots(9, TEAM).length > 24 /* Out of 28 */) {
      rc.disintegrate();
      return;
    }

    // UPDATE GOAL ------------------------------------------------------------

    // Check for a suicide message, if received this is priority number 1
    Message[] messages = rc.readMessages(rc.getRoundNum() - 1);
    for (Message m : messages) {
      switch (m.getBytes() & Communication.MESSAGE_TYPE_BITMASK) {
        case Communication.SUICIDE:
          GoalManager.replaceTopGoal(Goal.Type.CAPTURE_RUIN, Communication.getCoordinates(m.getBytes()));
          break;
        default:
          System.out.println("RECEIVED UNKNOWN MESSAGE: " + m);
      }
    }

    // Check if someone else finished the current ruin
    // TODO: Should this check stay here? Duplicated in Painter
    if (GoalManager.current().type == Goal.Type.CAPTURE_RUIN) {
      if (rc.canSenseRobotAtLocation(GoalManager.current().target)) {
        MapData.updateData(rc.senseMapInfo(GoalManager.current().target));
        GoalManager.setNewGoal(Goal.Type.REFILL_PAINT, GoalManager.current().target);
      }
    }

    // Check if someone else finished the current SRP
    // TODO: Should this check stay here? Duplicated in Painter
    if (GoalManager.current().type == Goal.Type.CAPTURE_SRP) {
      MapLocation target = GoalManager.current().target;
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
    if (GoalManager.current().type.v < Goal.Type.REFILL_PAINT.v && rc.getPaint() < REFILL_PAINT_THRESHOLD * rc.getType().paintCapacity / 100) {
      // After refill, we want to explore back to our previous location in case we find something better along the way
      GoalManager.replaceTopGoal(Goal.Type.EXPLORE, GoalManager.current().target);
      MapLocation tower = MapData.closestFriendlyTower();
      GoalManager.pushGoal(Goal.Type.REFILL_PAINT, tower == null ? Robot.spawnTower : tower);
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
        if (!rc.canSenseRobotAtLocation(GoalManager.current().target) || rc.senseRobotAtLocation(GoalManager.current().target).getTeam() != OPPONENT) {
          GoalManager.setNewGoal(Goal.Type.CAPTURE_RUIN, GoalManager.current().target);
        }
        break;
      case CAPTURE_SRP:
        if (Painter.paintCaptureSRP()) {
          GoalManager.popGoal();
        }
        break;
      case CAPTURE_RUIN:
        if (Painter.paintCaptureRuin()) {
          // Check if we actually finished ruin or if we just can't make progress
          if (rc.canSenseRobotAtLocation(GoalManager.current().target)) {
            GoalManager.replaceTopGoal(Goal.Type.REFILL_PAINT, GoalManager.current().target);
          } else {
            // Only request backup if we don't already see a mopper
            boolean foundMopper = false;
            for (RobotInfo info : rc.senseNearbyRobots(-1, TEAM)) {
              if (info.getType() == UnitType.MOPPER) {
                foundMopper = true;
                break;
              }
            }
            if (!foundMopper) {
              MapLocation tower = MapData.closestFriendlyTower();
              GoalManager.pushGoal(Goal.Type.GET_BACKUP, tower == null ? Robot.spawnTower : tower);
            }
          }
        }
        // Pathfinding target is the tower which was just built, should have paint
        break;
      case GET_BACKUP: // INTENTIONAL FALLTHROUGH
      case REFILL_PAINT:
        if (rc.getLocation().isWithinDistanceSquared(GoalManager.current().target, GameConstants.PAINT_TRANSFER_RADIUS_SQUARED)) {
          RobotInfo tower = rc.senseRobotAtLocation(GoalManager.current().target);
          if (tower == null) {
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, GoalManager.current().target)) {
              rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, GoalManager.current().target);
              tower = rc.senseRobotAtLocation(GoalManager.current().target);
            } else {
              MapLocation friendTower = MapData.closestFriendlyTower();
              GoalManager.replaceTopGoal(Goal.Type.REFILL_PAINT, friendTower == null ? Robot.spawnTower : friendTower);
              return;
            }
          }
          int paintAmount = rc.getType().paintCapacity - rc.getPaint();
          if (tower.getPaintAmount() < paintAmount) { paintAmount = tower.getPaintAmount(); }
          if (rc.canTransferPaint(GoalManager.current().target, -paintAmount)) {
            rc.transferPaint(GoalManager.current().target, -paintAmount);
            Goal.Type prevGoalType = GoalManager.current().type;
            GoalManager.popGoal();
            if (prevGoalType == Goal.Type.GET_BACKUP) {
              int message = Communication.REQUEST_MOPPER;
              Communication.trySendMessage(Communication.addCoordinates(message, GoalManager.current().target), tower.getLocation());
            }
          }
        }
        break;
      case EXPLORE: // TODO: Address clumping of units
        if (rc.getLocation().isWithinDistanceSquared(GoalManager.current().target, GameConstants.VISION_RADIUS_SQUARED)) {
          MapData.updateData(rc.senseMapInfo(GoalManager.current().target));
          GoalManager.replaceTopGoal(Goal.Type.EXPLORE,MapData.getExploreTarget());
        }
        break;
      default: break;
    }
  }

  protected void doMacro() throws GameActionException {
    if (GoalManager.current().type != Goal.Type.FIGHT_TOWER || !rc.getLocation().isWithinDistanceSquared(GoalManager.current().target, GameConstants.VISION_RADIUS_SQUARED)) {
      Pathfinding.moveTo(GoalManager.current().target); //note that Soldier defaults to ANY, can be set anywhere, but must be set back to ANY
    }
    if(!initialSoldiers && GoalManager.current().type != Goal.Type.SURVIVE && GoalManager.current().type != Goal.Type.REFILL_PAINT){  //initial soldier, SURVIVE, and REFILL_PAINT soldiers shouldn't waste paint
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
      Painter.paintCaptureRuin();
      if (rc.canSenseLocation(targetRuin) && rc.senseRobotAtLocation(targetRuin)!=null) {
        initialSoldiers=false;  //we can be done with the opening if we successfully capture the first tower
        //note initial soldiers will never finish working on a tower that is contested, so we never need to call for reinforcements
        GoalManager.replaceTopGoal(Goal.Type.REFILL_PAINT, GoalManager.current().target);
      }
    }else{ //explore
      //roam if there's no ruin in sight
      if(followID!=-1 && rc.canSenseRobot(followID)){ //second soldier
        RobotInfo firstSoldier = rc.senseRobot(followID);
        GoalManager.replaceTopGoal(Goal.Type.EXPLORE, firstSoldier.getLocation());
        rc.setIndicatorString("second soldier, followid: "+followID);
      }else{
        MapLocation closestEnemyTower = MapData.closestEnemyTower();
        if(closestEnemyTower!=null){
          GoalManager.replaceTopGoal(Goal.Type.EXPLORE, closestEnemyTower);
        }else{
          int[] symmetryPriority = new int[3];
          //we want the soldiers from each tower to assume different symmetries
          if(spawnTowerInfo.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER){
            symmetryPriority[0] = 0b010;  //horizontal
            symmetryPriority[1] = 0b001;  //rotational
            symmetryPriority[2] = 0b100;  //vertical
          }else{
            symmetryPriority[0] = 0b100;  //vertical
            symmetryPriority[1] = 0b001;  //rotational
            symmetryPriority[2] = 0b010;  //horizontal
          }

          //Guess the location of the enemy tower and choose it as our target
          MapLocation guessEnemyTower = MapData.symmetryLoc(spawnTowerInfo.getLocation(),symmetryPriority[0]);

          if(MapData.known(guessEnemyTower)){
            guessEnemyTower = MapData.symmetryLoc(spawnTowerInfo.getLocation(),symmetryPriority[1]);
            if(MapData.known(guessEnemyTower)){
              guessEnemyTower = MapData.symmetryLoc(spawnTowerInfo.getLocation(),symmetryPriority[2]);
              if(MapData.known(guessEnemyTower)){
                guessEnemyTower = null;
                
              }
            }
          }

          if(guessEnemyTower!=null){
            GoalManager.replaceTopGoal(Goal.Type.EXPLORE, guessEnemyTower);
          }else{
            //this should never run, since it rules out all 3 symmetries, but if it does, default to normal exploration
            System.out.println("Initial soldiers ruled out all 3 symmetries???");
            GoalManager.replaceTopGoal(Goal.Type.EXPLORE, MapData.getExploreTarget());
          }
        }
      }
      
    }
  }

  //called by micro (and opening) when we only want the soldiers to survive
  //we don't care that much about soldiers doing anything like capturing towers or srps
  //runs when we think the soldier is low on paint deep in enemy territory
  private void survive() throws GameActionException{
    //only 3 main priorities: stay on allied paint and stay away from enemy towers/moppers (and allied robots)
    //secondary priorities: make sure every ruin we see has at least 1 of our paint around it, try to navigate back to an allied tower to refill paint (without dying)

    MapLocation currentLoc = rc.getLocation();
    int[] directionScores = {0,0,0,0,0,0,0,0,0}; //a score for every direction (including center) in the order described by Direction.getDirectionOrderNum()

    String enemyTowerString = "";
    MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots();

    //fill enemyTowerString with a for loop
    for (MapLocation ruin : nearbyRuins){
      if(rc.senseRobotAtLocation(ruin)!=null && rc.senseRobotAtLocation(ruin).getTeam()==rc.getTeam().opponent()){
        //add to enemyTowerString
        //note, we can't use maplocation.toString() here because toString will result in strings of variable length ie: "(1,0)" doesn't have the same length as "(51,24)"
        //we use a string bc it's a low-bytecode resizable array
        enemyTowerString += (char) ruin.x;
        enemyTowerString += (char) ruin.y;
        enemyTowerString += (char) 69;  //hope that no map exceeds 68x68 cuz its funny
      }
    }

    //This is the trigger for searching the map for nearby allied paint
    boolean alliedPaintInMoveRange = false;

    //give scores to each direction (scores are initialized to 0s)
    for(Direction d : Direction.DIRECTION_ORDER){
      MapLocation destination = currentLoc.add(d);
      if(!rc.onTheMap(destination) || !rc.sensePassability(destination) || (d!=Direction.CENTER && rc.senseRobotAtLocation(destination)!=null)){
        //ignore tiles off the map, walls, and tiles with robots on them (we can't move through robots) 
        directionScores[d.getDirectionOrderNum()]=Integer.MIN_VALUE;
        continue;
      }
      //give direction score penalties based on enemy towers
      if(enemyTowerString.length()>0){
        for(int i = 0; i<enemyTowerString.length(); i+=3){
          MapLocation enemyTower = new MapLocation(enemyTowerString.charAt(i),enemyTowerString.charAt(i+1));  //don't worry, I did my research, charAt is very bytecode efficient (although it doesn't have set-1 bytecode)
          int destinationDistanceToTower = destination.distanceSquaredTo(enemyTower);
          int currentDistanceToTower = currentLoc.distanceSquaredTo(enemyTower);
          if(destinationDistanceToTower<=16){ //tower attack range is 4 (and it's not a gameconstant)
            //penalize a direction's score if it's in range of an enemy tower
            directionScores[d.getDirectionOrderNum()]-=100;
          }
          if(currentDistanceToTower<=16 && destinationDistanceToTower < currentDistanceToTower){
            //penalize a direction's score if it gets closer to an enemy tower (only if we're already in attack range of the tower)
            directionScores[d.getDirectionOrderNum()]-=100;
          }
        }
      }

      //give direction score penalties/bonuses based on paint
      MapInfo tile = rc.senseMapInfo(destination);
      switch(tile.getPaint()){
        case PaintType.ALLY_PRIMARY:  //I'm the goat if switch case statements work like this
        case PaintType.ALLY_SECONDARY:
          directionScores[d.getDirectionOrderNum()]+=50;
          alliedPaintInMoveRange = true;
          break;
        case PaintType.ENEMY_PRIMARY:
        case PaintType.ENEMY_SECONDARY:
          directionScores[d.getDirectionOrderNum()]-=50;
          if(rc.getPaint()<10) directionScores[d.getDirectionOrderNum()]-=100;
          break;
        case PaintType.EMPTY:
          directionScores[d.getDirectionOrderNum()]-=25;
          if(rc.getPaint()<10) directionScores[d.getDirectionOrderNum()]-=50;
          break;
      }

      //give penalties based on enemy moppers and allied units
      for(RobotInfo r : nearbyRobots){
        if(r.getType().isTowerType() || (r.getTeam()!=rc.getTeam() && r.getType()!=UnitType.MOPPER)) continue;  //ignore towers (both teams) and enemies that aren't moppers
        if(r.getTeam()==rc.getTeam() && r.getLocation().distanceSquaredTo(destination)<=2){
          //allied crowding penalty
          directionScores[d.getDirectionOrderNum()]-=100;
          //rc.setIndicatorDot(destination,0,255,0);
        }else if(r.getTeam()!=rc.getTeam() && r.getType()==UnitType.MOPPER && r.getLocation().distanceSquaredTo(destination)<=13){
          //mopper in range of destination
          if(r.getLocation().distanceSquaredTo(currentLoc)<=5 && r.getLocation().distanceSquaredTo(destination)>r.getLocation().distanceSquaredTo(currentLoc)){
            //in this case, we are currently in a position where it's impossible to move outside of mopper range
            //it would be nice to move further away from it, but not crucial since we're getting hit either way
            directionScores[d.getDirectionOrderNum()]+=50;
          }
          directionScores[d.getDirectionOrderNum()]-=100;
          //rc.setIndicatorDot(destination,255,0,0);
        }
      }
    }

    //calculate the direction to the nearest allied paint
    if(!alliedPaintInMoveRange){
      MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
      for(MapInfo m : nearbyTiles){
        if(currentLoc.distanceSquaredTo(m.getMapLocation())<=2) continue; //we already checked these in previous scoring
        if(m.getPaint().isAlly()){
          //(20 - distance to allied paint) will give us a good, small score to add. Note that it will never be greater than any other single score addition, so it's essentially just a tiebreaker
          directionScores[currentLoc.directionTo(m.getMapLocation()).getDirectionOrderNum()]+=(20-currentLoc.distanceSquaredTo(m.getMapLocation()));
        }
        if(Clock.getBytecodesLeft()<500) break; //this for loop may get costly
      }
    }

    //another tiebreaker: distance to nearest allied tower:
    if(Clock.getBytecodesLeft()>500){
      MapLocation nearestAlliedTower = MapData.closestFriendlyTower();
      if (nearestAlliedTower != null) {
        directionScores[currentLoc.directionTo(nearestAlliedTower).getDirectionOrderNum()] += 10;
      }
    }

    //now pick the best direction
    Direction bestDirection = Direction.CENTER; // Start with center, guaranteed can move
    if (directionScores[0]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[0]; }
    if (directionScores[1]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[1]; }
    if (directionScores[2]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[2]; }
    if (directionScores[3]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[3]; }
    if (directionScores[4]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[4]; }
    if (directionScores[5]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[5]; }
    if (directionScores[6]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[6]; }
    if (directionScores[7]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[7]; }
    if (directionScores[8]>(directionScores[bestDirection.getDirectionOrderNum()])) { bestDirection = Direction.DIRECTION_ORDER[8]; }

    /*
    String indicatorString = "Loc "+rc.getLocation().toString()+"\n";
    for(int i = 0; i<9; ++i){
      indicatorString += Direction.DIRECTION_ORDER[i].toString()+": "+directionScores[i] + "\n";
    }
    rc.setIndicatorString(indicatorString);
    */

    //set goal to the best direction
    GoalManager.setNewGoal(Goal.Type.SURVIVE,currentLoc.add(bestDirection));
    //rc.setIndicatorDot(currentLoc.add(bestDirection),0,0,255);
    if(rc.canMove(bestDirection)){
      rc.move(bestDirection);
      //paint tile we want to go to if we never saw any allied paint nearby
      if(!alliedPaintInMoveRange && rc.getPaint()>7 && rc.canAttack(currentLoc.add(bestDirection))){
        rc.attack(currentLoc.add(bestDirection));
      }
    }
  }
}
