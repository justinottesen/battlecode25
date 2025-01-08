package sprint_1.managers;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import sprint_1.robots.Robot;
import sprint_1.utils.MapData;

public class PaintManager {
    public static final int PAINT_THRESHOLD = 50;   //fine tune this value to determine when the robot should return to the home base to refill on paint
    static MapData mapData;
    public PaintManager(){

    }

    public static void refill(MapLocation homeTower) throws GameActionException{
        if(Robot.rc.getLocation().distanceSquaredTo(homeTower)>2) return;   //too far away from tower, cannot refill
        if(Robot.rc.canSenseRobotAtLocation(homeTower)){
            RobotInfo homeTowerInfo = Robot.rc.senseRobotAtLocation(homeTower);

            int paintTransferAmount = Robot.rc.getType().paintCapacity-Robot.rc.getPaint(); //for now, we'll always refill up to full capacity
            if(homeTowerInfo.getPaintAmount()<paintTransferAmount) paintTransferAmount = homeTowerInfo.getPaintAmount();    //if the hometower does not have enough paint to refill up to full capacity, just refill as much as possible

            if(Robot.rc.canTransferPaint(homeTower,-paintTransferAmount)){
                Robot.rc.transferPaint(homeTower, -paintTransferAmount);    //for some reason, units draw paint from towers by transfering a negative amount
            }
        }
    }
}
