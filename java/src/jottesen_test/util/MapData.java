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
   * TODO: If map symmetry is known, update pair location as well
   * 
   * @param info The `MapInfo` of the tile to be updated
   */
  public void updateData(MapInfo info) {
    int index = getIndex(info.getMapLocation());
    if (mapData[index] != 0) { return; }
    if (info.hasRuin()) { mapData[index] |= RUIN; return; }
    if (info.isWall()) { mapData[index] |= WALL; return; }
    mapData[index] |= EMPTY;
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
}