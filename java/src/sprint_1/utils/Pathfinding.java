package sprint_1.utils;

import battlecode.common.MapLocation;

public class Pathfinding {
    Bugpath bugPath;


    public Pathfinding(){
        bugPath = new Bugpath();
    }

    public void moveTo(MapLocation target){
        bugPath.moveTo(target);
    }
}
