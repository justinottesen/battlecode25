package sprint_1.managers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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
        pathfinding.moveTo(ruin);
        boolean patternComplete = true;
        int ruinx = ruin.x, ruiny = ruin.y;
        // Fill in any spots in the pattern with the appropriate paint.
        //Money tower pattern: 01110 11011 10001 11011 01110
        int index = -1;
        for(int dx = -2; dx<3;++dx){
            for(int dy = -2; dy<3;++dy){
                ++index;
                if(dx==0&&dy==0) continue; //skip the ruin tile itself
                MapLocation patternTile = new MapLocation(ruinx+dx,ruiny+dy);
                if(!Robot.rc.canSenseLocation(patternTile)){
                     patternComplete = false; 
                     continue; //if we can't sense a maplocation, skip it
                }
                PaintType patternTilePaint = Robot.rc.senseMapInfo(patternTile).getPaint();
                if((patternTilePaint == PaintType.ENEMY_PRIMARY || patternTilePaint == PaintType.ENEMY_SECONDARY) && Robot.rc.getType() == UnitType.SOLDIER) continue;  //Soldiers can't paint over enemy paint
                boolean useSecondaryColor = ((GameConstants.MONEY_TOWER_PATTERN >> index) & 1) == 1;
                if(patternTilePaint == PaintType.EMPTY || (patternTilePaint == PaintType.ALLY_PRIMARY && useSecondaryColor) || (patternTilePaint == PaintType.ALLY_SECONDARY && !useSecondaryColor)){
                    //if the tile is empty, or the paint type is different than its supposed to be
                    if(Robot.rc.canAttack(patternTile)){
                        Robot.rc.attack(patternTile,useSecondaryColor);
                    }else{
                        patternComplete = false;    //since we know that a tile is wrong and we can't paint it

                        break;

                    }
                }
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

    public void markResourcePatternCenter(MapLocation center) throws GameActionException{
        MapInfo[] mapInfos = Robot.rc.senseNearbyMapInfos(center,9);
        //resource pattern centers can't be less than 9 away from each other
        for(MapInfo m : mapInfos){
            if(m.getMark() == PaintType.ALLY_PRIMARY || m.getMark() == PaintType.ALLY_SECONDARY) return;
        }
        if(Robot.rc.canMark(center)) Robot.rc.mark(center, false);
    }

    public void captureResourcePattern() throws GameActionException{

        //look for a center:
        MapLocation center = null;
        MapInfo[] mapInfos = Robot.rc.senseNearbyMapInfos();
        for(MapInfo m : mapInfos){
            if(m.getMark()==PaintType.ALLY_PRIMARY){
                if(center==null || Robot.rc.getLocation().distanceSquaredTo(center)>Robot.rc.getLocation().distanceSquaredTo(m.getMapLocation())){
                    //get the closest center to the robot
                    center = m.getMapLocation();
                }
            }
        }

        if(center==null) return;

        pathfinding.moveTo(center);
        //boolean patternComplete = true;
        int centerx = center.x, centery = center.y;
        // Fill in any spots in the pattern with the appropriate paint.
        //Special Resource pattern: 10101 01010 10001 01010 10101
        int index = -1;
        for(int dx = -2; dx<3;++dx){
            for(int dy = -2; dy<3;++dy){
                ++index;
                MapLocation patternTile = new MapLocation(centerx+dx,centery+dy);
                if(!Robot.rc.canSenseLocation(patternTile)){
                     //patternComplete = false; 
                     continue; //if we can't sense a maplocation, skip it
                }
                PaintType patternTilePaint = Robot.rc.senseMapInfo(patternTile).getPaint();
                if((patternTilePaint == PaintType.ENEMY_PRIMARY || patternTilePaint == PaintType.ENEMY_SECONDARY) && Robot.rc.getType() == UnitType.SOLDIER) continue;  //Soldiers can't paint over enemy paint
                boolean useSecondaryColor = ((GameConstants.RESOURCE_PATTERN >> index) & 1) == 1;
                if(patternTilePaint == PaintType.EMPTY || (patternTilePaint == PaintType.ALLY_PRIMARY && useSecondaryColor) || (patternTilePaint == PaintType.ALLY_SECONDARY && !useSecondaryColor)){
                    //if the tile is empty, or the paint type is different than its supposed to be
                    if(Robot.rc.canAttack(patternTile)){
                        Robot.rc.attack(patternTile,useSecondaryColor);
                    }else{
                        //patternComplete = false;    //since we know that a tile is wrong and we can't paint it
                        break;
                    }
                }
            }
        }
        // Complete the ruin if we can.
        if (Robot.rc.canCompleteResourcePattern(center)){
            Robot.rc.completeResourcePattern(center);
            Robot.rc.mark(center,true);
            //System.out.println("Completed resourc pattern at "+center);
        }
    }
    
}
