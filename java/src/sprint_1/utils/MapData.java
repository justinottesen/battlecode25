package sprint_1.utils;

import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.PaintType;

import sprint_1.robots.*;

/**
 * Class that stores persistent info about the map. It is updated every round.
 */
public class MapData {

    RobotController rc;

    MapData(){
        rc = Robot.rc;
    }

    final static int MAX_MAP_SIZE = 64;
    final static int MAP_SZ_SQ = MAX_MAP_SIZE * MAX_MAP_SIZE;

    int[] mapData = new int[MAP_SZ_SQ];
    //first bit = seen / not seen
    //second bit = wall / not wall
    //third bit = ruin / not ruin
    //fourth-fifth bit paint     00 = no paint, 01 = your primary paint, 10 = your secondary paint, 11 = opponent paint
    //sixth-seventh bit  mark    00 = no mark, 01 = primary mark, 10, secondary mark

    void checkAll(){
        MapInfo[] locs = rc.senseNearbyMapInfos();
        for (MapInfo mi : locs){
            MapLocation mloc = mi.getMapLocation();
            int code = mloc.x*MAX_MAP_SIZE + mloc.y;
            //set seen
            int c = (mapData[code] | 1);
            //set wall
            if (mi.isWall()) c |= 2;
            //set paint
            switch(mi.getPaint()){
                case PaintType.EMPTY: 
                    c &= 103; //decimal for 11 00 111
                    break;
                case PaintType.ALLY_PRIMARY:
                    c &= 103; //decimal for 11 00 111
                    c |= 8; //decimal for 00 01 000
                    break;
                case PaintType.ALLY_SECONDARY:
                    c &= 103; //decimal for 11 00 111
                    c |= 16; //decimal for 00 10 000
                    break;
                default:
                    c |= 24; //decimal for 00 11 000
                    break;
            }


            switch(mi.getMark()){
                case PaintType.EMPTY: 
                    c &= 31; //decimal for 00 11 111
                    break;
                case PaintType.ALLY_PRIMARY:
                    c &= 31; //decimal for 00 11 111
                    c |= 32; //decimal for 01 00 000
                    break;
                case PaintType.ENEMY_PRIMARY:
                    c &= 31; //decimal for 00 11 111
                    c |= 32; //decimal for 01 00 000
                    break;
                case PaintType.ALLY_SECONDARY:
                    c &= 31; //decimal for 00 11 111
                    c |= 64; //decimal for 10 00 000
                    break;
                case PaintType.ENEMY_SECONDARY:
                    c &= 31; //decimal for 00 11 111
                    c |= 64; //decimal for 10 00 000
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
}
