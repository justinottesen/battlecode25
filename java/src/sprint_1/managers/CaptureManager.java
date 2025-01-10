package sprint_1.managers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.UnitType;
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

    public CaptureManager(){
        mapData = Robot.mapData;
        pathfinding = new Pathfinding();
        buildTowerType = UnitType.LEVEL_ONE_MONEY_TOWER;
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
        // Fill in any spots in the pattern with the appropriate paint.
        for (MapInfo patternTile : Robot.rc.senseNearbyMapInfos(ruin, 8)){
            if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
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
        }
    }

    public void rebuildTower(MapLocation rebuildLocation) throws GameActionException{
        if(rebuildLocation == null) return;
        //check if the pattern is complete
        pathfinding.moveTo(rebuildLocation);
        // Complete the ruin if we can.
        if (Robot.rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, rebuildLocation)){
            Robot.rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, rebuildLocation);
            Robot.rc.setTimelineMarker("Tower rebuilt", 0, 255, 0);
            //System.out.println("Built a tower at " + ruin + "!");
        }
    }

    
}
