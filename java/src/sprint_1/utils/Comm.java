package sprint_1.utils;
import sprint_1.robots.Robot;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;
import battlecode.common.Message;

public class Comm {
    final static int MAX_MAP_SIZE = 64;
    final static int MAP_SZ_SQ = MAX_MAP_SIZE * MAX_MAP_SIZE;

    //returns true if it sends the message, false if it found no robot to send the message to
    public static boolean requestMoneyTowerReplacement() throws GameActionException{
        RobotInfo[] nearbyRobots = Robot.rc.senseNearbyRobots(-1,Robot.rc.getTeam());
        if(nearbyRobots.length==0) return false;    //no nearby robots, return false

        for(RobotInfo robot:nearbyRobots){
            if(Robot.rc.canSendMessage(robot.getLocation(), 0)){
                if(!robot.type.equals(UnitType.SOLDIER)) continue;  //we only care about soldiers here
                int code = Robot.rc.getLocation().x*MAX_MAP_SIZE + Robot.rc.getLocation().y;
                //message: code(12 bits) 000(3 bit message type)
                Robot.rc.canSendMessage(robot.getLocation(),0+code<<3);
                return true;
            }
        }
        return false;
    }

    //returns the tower MapLocation if it received a message, null if it received no message
    public static MapLocation receiveMoneyTowerRequest() throws GameActionException{
        Message[] receivedMessages = Robot.rc.readMessages(Robot.rc.getRoundNum());
        if(receivedMessages.length==0) return null;

        int index = 0;
        int message = -1;

        while(index<receivedMessages.length){
            message = receivedMessages[index].getBytes();
            if((message & 7) == 0) break;  //only read the message
        }
        if(message==-1) return null;    //no message had the 3 bit message type 000
        int code = message >> 3;
        int x = code / MAX_MAP_SIZE; int y = code % MAX_MAP_SIZE;
        return new MapLocation(x,y);
    }
}
