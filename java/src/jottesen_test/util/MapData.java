package jottesen_test.util;

import battlecode.common.*;

/**
 * A class which contains the robots knowledge of the map. This only contains static (unchanging)
 * data.
 */
public class MapData {

  private final RobotController rc;

  public final int MAP_WIDTH;
  public final int MAP_HEIGHT;

  public final int MAX_DISTANCE_SQ;

  private final int[] mapData;
  private final int UNKNOWN = 0b0;
  private final int EMPTY = 0b1;
  private final int RUIN = 0b10;
  private final int WALL = 0b100;

  private int symmetryType = 0b111;
  private final int ROTATIONAL = 0b1;
  private final int HORIZONTAL = 0b10;
  private final int VERTICAL = 0b100;

  public MapData(RobotController rc_) {
    rc = rc_;
    MAP_WIDTH = rc.getMapWidth();
    MAP_HEIGHT = rc.getMapHeight();
    MAX_DISTANCE_SQ = MAP_WIDTH * MAP_WIDTH + MAP_HEIGHT * MAP_HEIGHT;

    mapData = new int[MAP_WIDTH * MAP_HEIGHT];
  }

  /**
   * Checks all visible squares around the robot and adds their information to the
   * mapData grid.
   * 
   * TODO: Make a version which only checks newly visible squares (after moving)
   */
  public void updateAllVisible() {
    for (MapInfo info : rc.senseNearbyMapInfos()) {
      updateData(info);
    }
  }

  /**
   * Takes the info for a single space and updates the robot's knowledge with
   * that information.
   * 
   * @param info The `MapInfo` of the tile to be updated
   */
  public void updateData(MapInfo info) {
    // Update this square
    int index = getIndex(info.getMapLocation());
    if (mapData[index] != 0) { return; }
    try {
      rc.setIndicatorDot(info.getMapLocation(), 255, 0, 255);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (info.hasRuin()) { mapData[index] |= RUIN; }
    else if (info.isWall()) { mapData[index] |= WALL; }
    else { mapData[index] |= EMPTY; }

    // If symmetry is not known, try to figure it out
    if (!symmetryKnown()) {
      if ((symmetryType & HORIZONTAL) > 0) {
        int h_index = symmetryIndex(index, HORIZONTAL);
        if (mapData[h_index] != UNKNOWN && mapData[h_index] != mapData[index]) {
          System.out.println("Ruled out HORIZONTAL");
          symmetryType ^= HORIZONTAL;
        }
      }
      if ((symmetryType & VERTICAL) > 0) {
        int h_index = symmetryIndex(index, VERTICAL);
        if (mapData[h_index] != UNKNOWN && mapData[h_index] != mapData[index]) {
          System.out.println("Ruled out VERTICAL");
          symmetryType ^= VERTICAL;
        }
      }
      if ((symmetryType & ROTATIONAL) > 0) {
        int h_index = symmetryIndex(index, ROTATIONAL);
        if (mapData[h_index] != UNKNOWN && mapData[h_index] != mapData[index]) {
          System.out.println("Ruled out ROTATIONAL");
          symmetryType ^= ROTATIONAL;
        }
      }
    }

    // Copy data over symmetrically
    if (symmetryKnown()) {
      mapData[symmetryIndex(index, symmetryType)] = mapData[index];
      try {
        rc.setIndicatorDot(getLoc(symmetryIndex(index, symmetryType)), 255, 255, 0);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Determines whether a given location is passable. Will return true if:
   * - The location does NOT have a ruin (or tower) or wall
   * - The location is unknown
   * @param loc The location to check passability of
   * @return Whether the location is passable or not
   */
  public boolean passable(MapLocation loc) { return (readData(loc) & (RUIN | WALL)) == 0; }

  /**
   * Determines whether a given location is known
   * 
   * @param loc The location to check knowledge of
   * @return Whether that location is known or not
   */
  public boolean known(MapLocation loc) { return readData(loc) != UNKNOWN; }

  /**
   * Gets the index of the value in the `mapData` array corresponding to the location
   * @param loc The `MapLocation` to find the index of
   * @return The index in the `mapData` array of `loc`
   */
  private int getIndex(MapLocation loc) { return loc.x * MAP_HEIGHT + loc.y; }

  /**
   * Gets the `MapLocation` corresponding to the index in `mapData`
   * @param index The position in the `mapData` array
   * @return The corresponding `MapLocation`
   */
  private MapLocation getLoc(int index) { return new MapLocation(index / MAP_HEIGHT, index % MAP_HEIGHT); }

  /**
   * Gets the `mapData` value associated with the given `MapLocation`
   * 
   * @param loc The `MapLocation` to get the value for
   * @return The value representing the known information about that square
   */
  private int readData(MapLocation loc) { return mapData[getIndex(loc)]; }

  /**
   * Checks whether the symmetry of the map is known
   *  
   * @return Whether the symmetry is known or not
   */
  boolean symmetryKnown() { return (symmetryType == 1) || (symmetryType & (symmetryType - 1)) == 0; }

  /**
   * Returns the location of the symmetrically paired tile on the map
   * @param loc The starting position to find the pair for
   * @param symmetryType The type of symmetry
   * @return The position of the symmetric value
   */
  private MapLocation symmetryLoc(MapLocation loc, int symmetryType) {
    return switch (symmetryType) {
      case HORIZONTAL -> new MapLocation(loc.x, MAP_HEIGHT - (loc.y + 1));
      case VERTICAL -> new MapLocation(MAP_WIDTH - (loc.x + 1), loc.y);
      case ROTATIONAL -> new MapLocation(MAP_WIDTH - (loc.x + 1), MAP_HEIGHT - (loc.y + 1));
      default -> null; 
    };
  }

  /**
   * Returns the index of the symmetrically paired tile on the map
   * @param index The starting index to find the pair for
   * @param symmetryType The type of symmetry
   * @return The position of the symmetric value
   */
  private int symmetryIndex(int index, int symmetryType) {
    int x = index / MAP_HEIGHT;
    int y = index % MAP_HEIGHT;
    return switch (symmetryType) {
      case HORIZONTAL -> x * MAP_HEIGHT + MAP_HEIGHT - (y + 1);
      case VERTICAL -> (MAP_WIDTH - (x + 1)) * MAP_HEIGHT + y;
      case ROTATIONAL -> (MAP_WIDTH - (x + 1)) * MAP_HEIGHT + MAP_HEIGHT - (y + 1);
      default -> -1;
    };
  }
}