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

    //should be called by money towers when they want a soldier to rebuild it
    //returns true if it sends the message, false if it found no robot to send the message to
    public static boolean requestMoneyTowerReplacement() throws GameActionException{
        RobotInfo[] nearbyRobots = Robot.rc.senseNearbyRobots(-1,Robot.rc.getTeam());
        if(nearbyRobots.length==0) return false;    //no nearby robots, return false

        for(RobotInfo robot:nearbyRobots){
            if(!robot.type.equals(UnitType.SOLDIER)) continue;  //we only care about soldiers here
            if(Robot.rc.canSendMessage(robot.getLocation(), 0)){
                int code = Robot.rc.getLocation().x*MAX_MAP_SIZE + Robot.rc.getLocation().y;
                //message: code(12 bits) 000(3 bit message type)
                Robot.rc.sendMessage(robot.getLocation(),0+code<<3);
                Robot.rc.setIndicatorString("Sent message to "+robot.getLocation());
                return true;
            }
        }
        return false;
    }

    //should be called by money towers when they want a mopper or a splasher to come and help rebuild the money tower pattern
    //returns true if it sends the message, false if it found no robot to send the message to
    public static boolean requestPatternHelp() throws GameActionException{
        RobotInfo[] nearbyRobots = Robot.rc.senseNearbyRobots(-1,Robot.rc.getTeam());
        if(nearbyRobots.length==0) return false;    //no nearby robots, return false

        for(RobotInfo robot:nearbyRobots){
            if(Robot.rc.canSendMessage(robot.getLocation(), 0)){
                if(!robot.type.equals(UnitType.SPLASHER)&&!robot.type.equals(UnitType.MOPPER)) continue;  //we only care about moppers or splashers here
                int code = Robot.rc.getLocation().x*MAX_MAP_SIZE + Robot.rc.getLocation().y;
                //message: code(12 bits) 000(3 bit message type)
                Robot.rc.sendMessage(robot.getLocation(),0+code<<3);
                return true;
            }
        }
        return false;
    }

    //should be called by all robots to constantly check for money tower requests (designed to work for both pattern help and rebuilding)
    //returns the tower MapLocation if it received a message, null if it received no message
    public static MapLocation receiveMoneyTowerRequest() throws GameActionException{
        Message[] receivedMessages = Robot.rc.readMessages(-1);
        if(receivedMessages.length==0) return null;
        int index = 0;
        int message = -1;

        while(index<receivedMessages.length){
            message = receivedMessages[index].getBytes();
            if((message & 7) == 0) break;  //only read the message if its the correct message type (type 0)
        }
        if(message==-1) return null;    //no message had the 3 bit message type 000
        int code = message >> 3;
        int x = code / MAX_MAP_SIZE; int y = code % MAX_MAP_SIZE;
        Robot.rc.setIndicatorString("Received money tower request at "+new MapLocation(x,y));
        return new MapLocation(x,y);
    }


}
