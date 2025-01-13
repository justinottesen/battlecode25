package sprint_1.utils;

import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import sprint_1.robots.Robot;

/**
 * Class that stores persistent info about the map. It is updated every round.
 */
public class MapData {

    RobotController rc;

    public MapData(){
        rc = Robot.rc;
    }

    final static int MAX_MAP_SIZE = 64;
    final static int MAP_SZ_SQ = MAX_MAP_SIZE * MAX_MAP_SIZE;

    int ruinListSize=0;
    MapLocation targetRuin = null;

    int[] mapData = new int[MAP_SZ_SQ];
    //first bit = seen(1) / not seen(0)
    //second bit = wall(1) / not wall(0)
    //third-fourth bit =         00 = no ruin, 01 = unclaimed ruin, 10 = our ruin, 11 opponent ruin
    //fifth-sixth bit paint     00 = no paint, 01 = your primary paint, 10 = your secondary paint, 11 = opponent paint
    //seventh-eigth bit  mark    00 = no mark, 01 = primary mark, 10, secondary mark

    final static int MapData_Data_Length = 8;
    public void checkAll(){
        MapInfo[] locs = rc.senseNearbyMapInfos();
        for (MapInfo mi : locs){
            MapLocation mloc = mi.getMapLocation();
            int code = mloc.x*MAX_MAP_SIZE + mloc.y;
            //set seen
            int c = (mapData[code] | 1);
            //set wall
            if (mi.isWall()) c |= 2;

            int ruin = c&4 | c&8;
            try{
                if(mi.hasRuin()){    //we have to senseRobotAtLocation so that we don't track bases
                    if(ruin==0){    //if this is the first time seeing this ruin
                        mapData[ruinListSize++] |= (code << MapData_Data_Length); //we can store ruin data in the same array, just after all 7 bits
                        //c |= 4;
                    }
                    RobotInfo tower = rc.senseRobotAtLocation(mloc);
                    if(tower == null){
                        c &= 243; //decimal for 11 11 00 11
                        c |= 4;
                    }else if(tower.getTeam().equals(rc.getTeam())){
                        c &= 243; //decimal for 11 11 00 11
                        c |= 8;
                    }else{
                        c |= 12;
                        rc.setIndicatorString("enemy tower spotted @ "+mloc+ " bool: "+((c&12) != 8));
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            //set paint
            switch(mi.getPaint()){
                case PaintType.EMPTY: 
                    c &= 207; //decimal for 11 00 11 11
                    break;
                case PaintType.ALLY_PRIMARY:
                    c &= 207; //decimal for 11 00 11 11
                    c |= 16; //decimal for 00 01 00 00
                    break;
                case PaintType.ALLY_SECONDARY:
                    c &= 207; //decimal for 11 00 11 11
                    c |= 32; //decimal for 00 10 00 00
                    break;
                default:
                    c |= 48; //decimal for 00 11 00 00
                    break;
            }


            switch(mi.getMark()){
                case PaintType.EMPTY: 
                    c &= 63; //decimal for 00 11 11 11
                    break;
                case PaintType.ALLY_PRIMARY:
                    c &= 63; //decimal for 00 11 11 11
                    c |= 64; //decimal for 01 00 00 00
                    break;
                case PaintType.ENEMY_PRIMARY:
                    c &= 63; //decimal for 00 11 11 11
                    c |= 64; //decimal for 01 00 00 00
                    break;
                case PaintType.ALLY_SECONDARY:
                    c &= 63; //decimal for 00 11 11 11
                    c |= 128; //decimal for 10 00 00 00
                    break;
                case PaintType.ENEMY_SECONDARY:
                    c &= 63; //decimal for 00 11 11 11
                    c |= 128; //decimal for 10 00 00 00
                    break;
                default:
                    //uh oh
                    break;
            }

            mapData[code] = c;
        }
    }

    boolean visited(int x, int y){
        return ((mapData[x*MAX_MAP_SIZE +y] & 1) == 1);
    }

    void checkCurrentRuin(){
        if (targetRuin == null) return;
        int code = targetRuin.x*MAX_MAP_SIZE + targetRuin.y;
        if ((mapData[code] & 4) == 0) targetRuin = null;
    }

    public MapLocation getClosestRuin(){
        if(ruinListSize==0) return null;
        checkCurrentRuin();
        if(targetRuin!=null) return targetRuin;
        MapLocation newTarget = null;
        for (int i = 0; i < ruinListSize; ++i){ //iterate through every known ruin
            //int ind = i%ruinListSize;
            int code = (mapData[i] >>> MapData_Data_Length);
            //rc.setIndicatorString("mapdata data: "+mapData[code]);
            if ((mapData[code]&4) == 0 || (mapData[code]&8) == 1) continue; //location is no longer a ruin (and has turned into a tower (or not a ruin?))
            int x = code / MAX_MAP_SIZE; int y = code % MAX_MAP_SIZE;
            MapLocation newLoc = new MapLocation(x,y);
            if (newTarget == null || rc.getLocation().distanceSquaredTo(newLoc) < rc.getLocation().distanceSquaredTo(newTarget)){
                newTarget = newLoc;
            }
        }
        targetRuin = newTarget;
        return targetRuin;
    }

    //this method is so we can navigate back to a tower for paint
    public MapLocation getClosestTower(){
        if(ruinListSize==0) return null;
        MapLocation newTarget = null;
        for (int i = 0; i < ruinListSize; ++i){ //iterate through every known ruin
            //int ind = i%ruinListSize;
            int code = (mapData[i] >>> MapData_Data_Length);
            //rc.setIndicatorString("mapdata data: "+mapData[code]);
            if ((mapData[code]&12) != 8) continue; //if location isn't our tower, continue
            //note: this condition is might not still work. needs testing
            
            int x = code / MAX_MAP_SIZE; int y = code % MAX_MAP_SIZE;
            MapLocation newLoc = new MapLocation(x,y);
            if (newTarget == null || rc.getLocation().distanceSquaredTo(newLoc) < rc.getLocation().distanceSquaredTo(newTarget)){
                newTarget = newLoc;
            }
        }
        return newTarget;
    }
    
    
    public boolean isResourcePatternCandidateLocation(MapLocation m){
        //requires a 5x5 space with no enemy paint and no walls
        //additionally, requires no tower pattern overlap
        int index = m.x*MAX_MAP_SIZE + m.y;
        for (int i = -2; i < 3; i++){
            for (int j = -2; j < 3; j++){
                if ((mapData[index + j + (i * MAX_MAP_SIZE)] & 1) == 0){    //out of vision
                    return false;
                }
                if ((mapData[index + j + (i * MAX_MAP_SIZE)] & 2) == 2){    //wall
                    return false;
                }
                if ((mapData[index + j + (i * MAX_MAP_SIZE)] & 24) != 0){    //ruin (need to check a larger radius in the future)
                    return false;
                }
            }
        }
        return true;
    }
    
}
