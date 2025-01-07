package sprint_1;

import battlecode.common.MapLocation;

public class Pathfinding {
    Bugpath bugPath;


    Pathfinding(){
        bugPath = new Bugpath();
    }

    void moveTo(MapLocation target){
        bugPath.moveTo(target);
    }
}
