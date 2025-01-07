package sprint_1.utils;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import sprint_1.robots.Robot;

/**
 * Class used to select random locations to explore.
 */
public class Explore {

    MapData mapData;

    RobotController rc;
    int myVisionRange;

    int targetRound = -100;
    MapLocation exploreLoc = null;

    MapLocation[] checkLocs = new MapLocation[5];
    //boolean checker = false;

    public Explore(MapData mapData_){
        this.rc = Robot.rc;
        myVisionRange = GameConstants.VISION_RADIUS_SQUARED;
        mapData = mapData_;
        generateLocs();
    }

    void generateLocs(){
        int w = rc.getMapWidth();
        int h = rc.getMapHeight();
        checkLocs[0] = new MapLocation(w/2,h/2);
        checkLocs[1] = new MapLocation(0,0);
        checkLocs[2] = new MapLocation(w-1,0);
        checkLocs[3] = new MapLocation(0,h-1);
        checkLocs[4] = new MapLocation(w-1,h-1);
    }

    void getEmergencyTarget(int tries) {
        //MapLocation myLoc = rc.getLocation();
        int maxX = rc.getMapWidth();
        int maxY = rc.getMapHeight();
        while (tries-- > 0){
            //if (exploreLoc != null) return;
            MapLocation newLoc = new MapLocation((int)(Math.random()*maxX), (int)(Math.random()*maxY));
            //if (checkDanger && Robot.comm.isEnemyTerritoryRadial(newLoc)) continue;
            if (mapData.visited(newLoc.x, newLoc.y)) continue;
            if (exploreLoc != null && rc.getLocation().distanceSquaredTo(exploreLoc) < rc.getLocation().distanceSquaredTo(newLoc)) continue;
            /*if (myLoc.distanceSquaredTo(newLoc) > myVisionRange){
                exploreLoc = newLoc;
            }*/
            exploreLoc = newLoc;
            targetRound = Robot.rc.getRoundNum();
        }
    }

    void getCheckerTarget(int tries){
        //MapLocation myLoc = rc.getLocation();
        while (tries-- > 0){
            int checkerIndex = (int)(Math.random()* checkLocs.length);
            MapLocation newLoc = checkLocs[checkerIndex];
            if (mapData.visited(newLoc.x, newLoc.y)) continue;
            if (exploreLoc != null && rc.getLocation().distanceSquaredTo(exploreLoc) < rc.getLocation().distanceSquaredTo(newLoc)) continue;
            exploreLoc = newLoc;
            targetRound = Robot.rc.getRoundNum();
        }
        if (exploreLoc == null) getEmergencyTarget(tries);
    }

    public MapLocation getExploreTarget() {
        //not sure what target round does here
        if (Robot.rc.getRoundNum() - targetRound > 40 || (exploreLoc != null && mapData.visited(exploreLoc.x, exploreLoc.y))) exploreLoc = null;
        if (exploreLoc == null){
            if (rc.getID()%2 == 0) getCheckerTarget(15);
            else getEmergencyTarget(15);
        }
        return exploreLoc;
    }

}