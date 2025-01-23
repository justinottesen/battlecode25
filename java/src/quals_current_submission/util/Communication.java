package quals_current_submission.util;

import quals_current_submission.*;

import battlecode.common.*;

public class Communication {

  // Bits 0-3: Message Type
  public static final int MESSAGE_TYPE_BITMASK = 0b1111;
  // Bits 4-15: Coordinates (unless specified differently below)
  public static final int X_COORDINATE_MASK = 0b000000_111111_0000;
  public static final int X_COORDINATE_BITSHIFT = 4;
  public static final int Y_COORDINATE_MASK = 0b111111_000000_0000;
  public static final int Y_COORDINATE_BITSHIFT = 10;

  // TODO: IMPLEMENT THIS COMMUNICATION
  public static final int SYMMETRY_KNOWLEDGE = 0b0001; // ----------------------------

      // Bits 4-6: Symmetry Value
      public static final int SYMMETRY_VALUE_MASK = 0b111_0000;

  // TODO: IMPLEMENT THIS COMMUNICATION
  public static final int REQUEST_MOPPER = 0b0010; // --------------------------------

      // Bits 4-15: Coordinates (specified above)

  public static final int SUICIDE = 0b0011; // ---------------------------------------

      // Bits 4-15: Coordinates (specified above)

  /**
   * Extracts the message type from the given message
   * @param message The message to extract from
   * @return The type of the message
   */
  public static int getMessageType(int message) { return message & MESSAGE_TYPE_BITMASK; }

  /**
   * Extracts the coordinates from the given message
   * @param message The message to extract from
   * @return The location encoded in the message
   */
  public static MapLocation getCoordinates(int message) { 
    return new MapLocation((message & X_COORDINATE_MASK) >> X_COORDINATE_BITSHIFT, 
                           (message & Y_COORDINATE_MASK) >> Y_COORDINATE_BITSHIFT);
  }

  /**
   * Adds coordinate information to a message
   * @param message The previous value of the message
   * @param loc The location to add to the message
   * @return The successfully created message
   */
  public static int addCoordinates(int message, MapLocation loc) { return addCoordinates(message, loc.x, loc.y); }

  /**
   * Adds coordinate information to a message
   * @param message The previous value of the message
   * @param x The x coordinate to add to the message
   * @param y The y coordinate to add to the message
   * @return The successfully created message
   */
  public static int addCoordinates(int message, int x, int y) {
    message &= ~(X_COORDINATE_MASK | Y_COORDINATE_MASK); // Clear previous coordinates
    message |= (x << X_COORDINATE_BITSHIFT) | (y << Y_COORDINATE_BITSHIFT);
    return message;
  }

  /**
   * Tries to send the given message to the given target
   * @param message The message to be sent to the target
   * @param target The target location to send the message
   * @return Whether the message was sent or not
   * @throws GameActionException
   */
  public static boolean trySendMessage(int message, MapLocation target) throws GameActionException {
    if (!Robot.rc.canSenseRobotAtLocation(target) || !Robot.rc.canSendMessage(target)) { return false; }
    Robot.rc.sendMessage(target, message);
    return true;
  }

  /**
   * Tries to send the given message to all of the given targets
   * @param message The message to be sent to the targets
   * @param targets The potential recipients of the message
   * @return Whether anyone was successfully sent the message
   * @throws GameActionException
   */
  public static boolean trySendAllMessage(int message, RobotInfo[] targets) throws GameActionException {
    boolean success = false;
    for (RobotInfo robot : targets) {
      success = trySendMessage(message, robot.getLocation()) || success;
    }
    return success;
  }
}
