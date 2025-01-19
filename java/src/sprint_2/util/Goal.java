package sprint_2.util;

import battlecode.common.*;

public class Goal {
  
  public enum Type {
    EXPLORE(0),
    CAPTURE_SRP(1),
    CAPTURE_RUIN(2),
    FIGHT_TOWER(3),
    REFILL_PAINT(4);

    public final int v;

    private Type(int v) {
      this.v = v;
    }
  }

  public final Type type;
  public final MapLocation target;

  public Goal(Type type, MapLocation target) {
    this.type = type;
    this.target = target;
  };

  @Override
  public String toString() { 
    return switch (type) {
      case Type.EXPLORE -> "EXPLORE";
      case Type.CAPTURE_SRP -> "CAPTURE_SRP"; 
      case Type.CAPTURE_RUIN -> "CAPTURE_RUIN"; 
      case Type.FIGHT_TOWER -> "FIGHT_TOWER";
      case Type.REFILL_PAINT -> "REFILL_PAINT"; 
    } + target;
  }

}
