package jottesen_test.util;

import battlecode.common.*;

public class Communication {
  
  private final RobotController rc;

  // Bits 0-3: Message Type
  public final int MESSAGE_TYPE_BITMASK = 0b1111;
  // Bits 4-15: Coordinates (unless specified differently below)
  public final int X_COORDINATE_MASK = 0b000000_111111_0000;
  public final int X_COORDINATE_BITSHIFT = 4;
  public final int Y_COORDINATE_MASK = 0b111111_000000_0000;
  public final int Y_COORDINATE_BITSHIFT = 10;

  // TODO: IMPLEMENT THIS COMMUNICATION
  public final int SYMMETRY_KNOWLEDGE = 0b0001; // ----------------------------

      // Bits 4-6: Symmetry Value
      public final int SYMMETRY_VALUE_MASK = 0b111_0000;

  // TODO: IMPLEMENT THIS COMMUNICATION
  public final int REQUEST_MOPPER = 0b0010; // --------------------------------

      // Bits 4-15: Coordinates (specified above)

  public final int SUICIDE = 0b0011; // ---------------------------------------

      // Bits 4-15: Coordinates (specified above)

  
  public static final int SELF_TOWER_TYPE_IS_PAINT = 0b0100;
  public static final int SELF_TOWER_TYPE_IS_MONEY = 0b0101;
  public static final int SELF_TOWER_TYPE_IS_DEFENSE = 0b0110; // --------------------
      // Bits 4-15: Coordinates (specified above)


  public Communication(RobotController rc_) {
    rc = rc_;
  }

  /**
   * Extracts the message type from the given message
   * @param message The message to extract from
   * @return The type of the message
   */
  public int getMessageType(int message) { return message & MESSAGE_TYPE_BITMASK; }

  /**
   * Extracts the coordinates from the given message
   * @param message The message to extract from
   * @return The location encoded in the message
   */
  public MapLocation getCoordinates(int message) { 
    return new MapLocation((message & X_COORDINATE_MASK) >> X_COORDINATE_BITSHIFT, 
                           (message & Y_COORDINATE_MASK) >> Y_COORDINATE_BITSHIFT);
  }

  /**
   * Adds coordinate information to a message
   * @param message The previous value of the message
   * @param loc The location to add to the message
   * @return The successfully created message
   */
  public int addCoordinates(int message, MapLocation loc) { return addCoordinates(message, loc.x, loc.y); }

  /**
   * Adds coordinate information to a message
   * @param message The previous value of the message
   * @param x The x coordinate to add to the message
   * @param y The y coordinate to add to the message
   * @return The successfully created message
   */
  public int addCoordinates(int message, int x, int y) {
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
  public boolean trySendMessage(int message, MapLocation target) throws GameActionException {
    if (!rc.canSendMessage(target)) { return false; }
    rc.sendMessage(target, message);
    return true;
  }

  /**
   * Tries to send the given message to all of the given targets
   * @param message The message to be sent to the targets
   * @param targets The potential recipients of the message
   * @return Whether anyone was successfully sent the message
   * @throws GameActionException
   */
  public boolean tryBroadcastMessage(int message, RobotInfo[] targets) throws GameActionException {
    boolean success = false;
    for (RobotInfo robot : targets) {
      success = trySendMessage(message, robot.getLocation()) || success;
    }
    return success;
  }

  /**
   * Tries to send the given message to all towers in range (for tower use only)
   * @param message The message to be sent to the targets
   * @return Whether anyone was successfully sent the message
   * @throws GameActionException
   */
  public boolean tryBroadcastMessage(int message) throws GameActionException {
    if(rc.canBroadcastMessage()){
      rc.broadcastMessage(message);
      return true;
    }
    return false;
  }
}
