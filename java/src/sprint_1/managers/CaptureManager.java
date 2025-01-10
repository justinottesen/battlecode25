package sprint_1.managers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.UnitType;
import battlecode.common.RobotInfo;
import sprint_1.robots.Robot;
import sprint_1.utils.MapData;
import sprint_1.utils.Pathfinding;
/**
 * Capture Manager. Helps turn ruins into our towers
 */
public class CaptureManager {
    static MapData mapData;
    static Pathfinding pathfinding;
    static UnitType buildTowerType;
    static String ignoreRuin;   //ignore ruins that other soldiers have under control
    static MapLocation savedRebuildLocation;

    public CaptureManager(){
        mapData = Robot.mapData;
        pathfinding = new Pathfinding();
        buildTowerType = UnitType.LEVEL_ONE_MONEY_TOWER;
        ignoreRuin = "";
        savedRebuildLocation=null;
    }

    public boolean isInIgnoreList(MapLocation ruin){
        String ruinString = "";
        char charx = (char)ruin.x;
        char chary = (char)ruin.y;
        ruinString +=charx+chary;
        return ignoreRuin.contains(ruinString);
    }

    public boolean betterBuilderAvailable(MapLocation ruin) throws GameActionException{
        if(!Robot.rc.canSenseLocation(ruin)) return true;
        RobotInfo[] soldiersNearRuin = Robot.rc.senseNearbyRobots(ruin,4,Robot.rc.getTeam());
        if(soldiersNearRuin.length==0) return false;
        for(RobotInfo r:soldiersNearRuin){
            //go through every soldier near the ruin
            //if that soldier is closer to the ruin we can piss off (and assume that robot will build the tower)
            //use id as the tiebreaker
            if(r.getLocation().distanceSquaredTo(ruin)<Robot.rc.getLocation().distanceSquaredTo(ruin) 
            || (r.getLocation().distanceSquaredTo(ruin)==Robot.rc.getLocation().distanceSquaredTo(ruin) && r.getID()>Robot.rc.getID())){
                //Robot.rc.setIndicatorString("Robot at "+r.getLocation()+" got this, I'm out");
                return true;
            }
        }
        return false;
    }

    public void captureTower() throws GameActionException{
        MapLocation ruin = mapData.getClosestRuin();
        if(ruin == null) return;
        MapLocation homeTower = mapData.getClosestTower();
        if(homeTower == null) return; //this one should never run bc the robot will always have its home tower in its mapdata
        //paint the pattern and capture the ruin
        Direction dir = Robot.rc.getLocation().directionTo(ruin);
        pathfinding.moveTo(ruin);
        // Mark the pattern we need to draw to build a tower here if we haven't already.
        MapLocation shouldBeMarked = ruin.subtract(dir);
        if (Robot.rc.canSenseLocation(shouldBeMarked) && Robot.rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && Robot.rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruin)){
            Robot.rc.markTowerPattern(buildTowerType, ruin);
            //System.out.println("Trying to build a tower at " + targetLoc);
        }
        boolean patternComplete = true;

        // Fill in any spots in the pattern with the appropriate paint.
        for (MapInfo patternTile : Robot.rc.senseNearbyMapInfos(ruin, 8)){
            if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
                patternComplete = false;
                if((patternTile.getPaint() == PaintType.ENEMY_PRIMARY || patternTile.getPaint() == PaintType.ENEMY_SECONDARY) && Robot.rc.getType() == UnitType.SOLDIER) continue;  //Soldiers can't paint over enemy paint
                boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                if (Robot.rc.canAttack(patternTile.getMapLocation()))
                    Robot.rc.attack(patternTile.getMapLocation(), useSecondaryColor);
            }
        }
        // Complete the ruin if we can.
        if (Robot.rc.canCompleteTowerPattern(buildTowerType, ruin)){
            Robot.rc.completeTowerPattern(buildTowerType, ruin);
            Robot.rc.setTimelineMarker("Tower built", 0, 255, 0);
            //System.out.println("Built a tower at " + ruin + "!");
        }else if(patternComplete){
            //Robot.rc.setIndicatorString("Pattern complete, but can't build the tower yet");
            //pattern is complete, but we don't have enough chips to build the tower
            //leave only one soldier to build the tower
            RobotInfo[] soldiersNearRuin = Robot.rc.senseNearbyRobots(ruin,4,Robot.rc.getTeam());
            if(soldiersNearRuin.length==0) return;
            for(RobotInfo r:soldiersNearRuin){
                //go through every soldier near the ruin
                //if that soldier is closer to the ruin we can piss off (and assume that robot will build the tower)
                //use id as the tiebreaker
                if(r.getLocation().distanceSquaredTo(ruin)<Robot.rc.getLocation().distanceSquaredTo(ruin) 
                || (r.getLocation().distanceSquaredTo(ruin)==Robot.rc.getLocation().distanceSquaredTo(ruin) && r.getID()>Robot.rc.getID())){
                    char x = (char)ruin.x;
                    char y = (char)ruin.y;
                    ignoreRuin+=x+y+" ";
                    //Robot.rc.setIndicatorString("Robot at "+r.getLocation()+" got this, I'm out");
                    break;
                }
            }
        }
    }

    public void setRebuildLocation(MapLocation rebuildLocation){
        savedRebuildLocation=rebuildLocation;
    }
    public MapLocation getRebuildLocation(){
        return savedRebuildLocation;
    }

    public void rebuildTower() throws GameActionException{
        if(savedRebuildLocation == null) return;
        //check if the pattern is complete
        pathfinding.moveTo(savedRebuildLocation);
        // Complete the ruin if we can.
        if (Robot.rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, savedRebuildLocation)){
            Robot.rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, savedRebuildLocation);
            Robot.rc.setTimelineMarker("Tower rebuilt", 0, 255, 0);
            savedRebuildLocation=null;
            //System.out.println("Built a tower at " + ruin + "!");
        }
    }

    
}
