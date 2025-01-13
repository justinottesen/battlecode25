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

  public final int SYMMETRY_KNOWLEDGE = 0b0001; // ----------------------------

      // Bits 4-6: Symmetry Value
      public final int SYMMETRY_VALUE_MASK = 0b111_0000;

  public final int REQUEST_MOPPER = 0b0010; // --------------------------------

      // Bits 4-15: Coordinates (specified above)

  public Communication(RobotController rc_) {
    rc = rc_;
  }
}
