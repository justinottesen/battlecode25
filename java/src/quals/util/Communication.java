package quals.util;

import quals.*;

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

  public static final int FRONT = 0b0100; // -----------------------------------------

      // 9 bits per coordinate, 8 for the 5x5 chunk, 1 for add / remove
      public static final int FRONT_LOC_BITMASK = 0b11111111; // Must be shifted
      public static final int LOC_1_BITSHIFT = 4;
      public static final int LOC_2_BITSHIFT = 12;
      public static final int LOC_3_BITSHIFT = 20;
      public static final int ADD_1_BITSHIFT = 11;
      public static final int ADD_2_BITSHIFT = 19;
      public static final int ADD_3_BITSHIFT = 27;

      public static boolean activePriority = true;
      public static int activeFrontsIndex = 0;
      public static int inactiveFrontsIndex = 0;


  /**
   * Extracts the message type from the given message
   * @param message The message to extract from
   * @return The type of the message
   */
  public static int getMessageType(int message) { return message & MESSAGE_TYPE_BITMASK; }

  /**
   * Creates the fronts message from the next fronts which should be updated
   */
  public static int createFrontsMessage() {

    MapLocation loc1 = null;
    boolean add1 = false;
    MapLocation loc2 = null;
    boolean add2 = false;
    MapLocation loc3 = null;
    boolean add3 = false;
    if (MapData.numActiveFronts() > 0) {
      loc1 = MapData.getActiveFrontAtIndex(0);
      add1 = true;
    } else if (MapData.numInactiveFronts() > 0) {
      loc1 = MapData.getInactiveFrontAtIndex(0);
    }
    if (loc1 != null) {
      if ( MapData.numActiveFronts() > 1) {
        loc2 = MapData.getActiveFrontAtIndex(1);
        add2 = true;
      } else if (MapData.numInactiveFronts() > 1) {
        loc2 = MapData.getInactiveFrontAtIndex(1);
      }
      if (loc2 != null) {
        if (MapData.numActiveFronts() > 2) {
          loc3 = MapData.getActiveFrontAtIndex(2);
          add3 = true;
        } else {
          loc3 = MapData.getInactiveFrontAtIndex(2);
        }
      }
    }

    return createFrontsMessage(loc1, add1, loc2, add2, loc3, add3);

    // Ijottesen_test.util.f both lists are empty, no need
    // if (MapData.numActiveFronts() == 0 && MapData.numInactiveFronts() == 0) { return 0; }

    // // Reset the indices
    // if (activeFrontsIndex >= MapData.numActiveFronts()) { activeFrontsIndex = 0;}
    // if (inactiveFrontsIndex >= MapData.numInactiveFronts()) { inactiveFrontsIndex = 0;}

    // // Create return values
    // MapLocation loc1 = null;
    // boolean add1 = false;
    // MapLocation loc2 = null;
    // boolean add2 = false;
    // MapLocation loc3 = null;
    // boolean add3 = false;

    // // Get the indices to work with
    // int activeI = activeFrontsIndex;
    // boolean firstActive = MapData.numActiveFronts() > 0;
    // int inactiveI = inactiveFrontsIndex;
    // boolean firstInactive = MapData.numInactiveFronts() > 0;
    
    // // Get the next messgae, alternating active and inactive
    // for (int locNum = 0; locNum < 3; /* Incremented in loop */) {
    //   if (activePriority && MapData.numActiveFronts() > 0) {
    //     // Get the next front
    //     MapLocation loc = MapData.getActiveFrontAtIndex(activeI);
    //     if (loc != null) {
    //       // Set the correct parameter
    //       switch (locNum++) {
    //         case 0: loc1 = loc; add1 = true; break;
    //         case 1: loc2 = loc; add2 = true; break;
    //         case 2: loc3 = loc; add3 = true; break;
    //       }
    //       // Increment the index
    //       activeI = (activeI + 1) % MapData.numActiveFronts();
    //     } else {
    //       // If we end up outside the list, stop
    //       activeI = activeFrontsIndex;
    //       firstActive = false;
    //     }
    //     // Start and end are the same
    //     if (activeI == activeFrontsIndex && firstActive) {
    //       firstActive = false;
    //     }
    //     ++activeI;
    //     // Switch priority if we aren't at the end of the other
    //     if (firstInactive || inactiveI != inactiveFrontsIndex) {
    //       activePriority = false;
    //     }
    //   } else if (MapData.numInactiveFronts() > 0) {
    //     MapLocation loc = MapData.getInactiveFrontAtIndex(inactiveI);
    //     if (loc != null) {
    //       switch (locNum++) {
    //         case 0: loc1 = loc; add1 = false; break;
    //         case 1: loc2 = loc; add2 = false; break;
    //         case 2: loc3 = loc; add3 = false; break;
    //       }
    //       inactiveI = (inactiveI + 1) % MapData.numInactiveFronts();
    //     } else {
    //       inactiveI = inactiveFrontsIndex;
    //       firstInactive = false;
    //     }
    //     if (inactiveI == inactiveFrontsIndex && firstInactive) {
    //       firstInactive = false;
    //     }
    //     ++inactiveI;
    //     if (firstActive || activeI != activeFrontsIndex) {
    //       activePriority = true;
    //     }
    //   }

    //   // If we exhausted both lists, exit loop
    //   if (activeI == activeFrontsIndex && !firstActive && inactiveI == inactiveFrontsIndex && !firstInactive) {
    //     break;
    //   }
    // }

    // // Update the most recently sent indices
    // activeFrontsIndex = activeI;
    // inactiveFrontsIndex = inactiveI;

    // return createFrontsMessage(loc1, add1, loc2, add2, loc3, add3);
  }

  /**
   * Create Fronts message given all information
   */
  public static int createFrontsMessage(MapLocation loc1, boolean add1, MapLocation loc2, boolean add2, MapLocation loc3, boolean add3) {
    int loc1bits = (loc1 == null ? 0 : (loc1.x / 5) + (loc1.y / 5) * 12) << LOC_1_BITSHIFT;
    int loc2bits = (loc2 == null ? 0 : (loc2.x / 5) + (loc2.y / 5) * 12) << LOC_2_BITSHIFT;
    int loc3bits = (loc3 == null ? 0 : (loc3.x / 5) + (loc3.y / 5) * 12) << LOC_3_BITSHIFT;

    int add1bits = (add1 ? 1 << ADD_1_BITSHIFT : 0);
    int add2bits = (add2 ? 1 << ADD_2_BITSHIFT : 0);
    int add3bits = (add3 ? 1 << ADD_3_BITSHIFT : 0);

    return (add3bits | loc3bits | add2bits | loc2bits | add1bits | loc1bits | FRONT);
  }

  /**
   * Update Fronts From Message
   */
  public static void updateFronts(int message) {
    int loc1bits = (message >> LOC_1_BITSHIFT) & FRONT_LOC_BITMASK;
    int loc2bits = (message >> LOC_2_BITSHIFT) & FRONT_LOC_BITMASK;
    int loc3bits = (message >> LOC_3_BITSHIFT) & FRONT_LOC_BITMASK;

    if (loc1bits != 0) {
      if ((message & (1 << ADD_1_BITSHIFT)) > 0) {
        MapData.addToFronts(loc1bits % 12, loc1bits / 12);
      } else {
        MapData.removeFromFronts(loc1bits % 12, loc1bits / 12);
      }
    }
    if (loc2bits != 0) {
      if ((message & (1 << ADD_2_BITSHIFT)) > 0) {
        MapData.addToFronts(loc2bits % 12, loc2bits / 12);
      } else {
        MapData.removeFromFronts(loc2bits % 12, loc2bits / 12);
      }
    }
    if (loc3bits != 0) {
      if ((message & (1 << ADD_3_BITSHIFT)) > 0) {
        MapData.addToFronts(loc3bits % 12, loc3bits / 12);
      } else {
        MapData.removeFromFronts(loc3bits % 12, loc3bits / 12);
      }
    }
  }

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
    if (message == 0) { return false; }
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
    if (message == 0) { return false; }
    boolean success = false;
    for (RobotInfo robot : targets) {
      success = trySendMessage(message, robot.getLocation()) || success;
    }
    return success;
  }

  public static boolean tryBroadcastMessae(int message) throws GameActionException {
    if (message == 0) { return false; }
    if (!Robot.rc.canBroadcastMessage()) { return false; }
    Robot.rc.broadcastMessage(message);
    return true;
  }
}
