package jottesen_test.util;

import javax.management.monitor.GaugeMonitor;

import battlecode.common.*;

/**
 * A class which contains the robots knowledge of the map. This only contains static (unchanging)
 * data.
 */
public class MapData {

  private final RobotController rc;
  private final Team TEAM;

  public final int MAP_WIDTH;
  public final int MAP_HEIGHT;

  public final int MAX_DISTANCE_SQ;

  private final int[] mapData;
  private final int UNKNOWN = 0b0;

  // Bits 0-1: Immutable characteristics
  private final int EMPTY             = 0b01;
  private final int RUIN              = 0b10;
  private final int WALL              = 0b11;
  private final int TILE_TYPE_BITMASK = 0b11;
  
  // Bits 2-4: Tower Type Data (Only applicable for ruins)
  private final int UNCLAIMED_RUIN     = 0b001_00;
  private final int MONEY_TOWER        = 0b010_00;
  private final int PAINT_TOWER        = 0b011_00;
  private final int DEFENSE_TOWER      = 0b100_00;
  private final int TOWER_TYPE_BITMASK = 0b111_00;

  // Bit 5: Friendly = 1, foe = 0 (Only applicable for claimed ruins)
  private final int FRIENDLY_TOWER = 0b1_000_00;

  // Bits 6-16: Last round updated (Only applicable for ruins)
  private final int LAST_UPDATED_BITMASK = 0b11111111111_0_000_00;
  private final int LAST_UPDATED_BITSHIFT = 6;

  // Bits 17-18: Paint status of tiles (Only applicable for empty)
  private final int ENEMY_PAINT        = 0b01_00000000000_0_000_00;
  private final int FRIENDLY_PRIMARY   = 0b10_00000000000_0_000_00;
  private final int FRIENDLY_SECONDARY = 0b11_00000000000_0_000_00;
  private final int PAINT_BITMASK      = 0b11_00000000000_0_000_00;

  private int symmetryType     = 0b111;
  private final int ROTATIONAL = 0b001;
  private final int HORIZONTAL = 0b010;
  private final int VERTICAL   = 0b100;

  private final int[] knownRuins;
  private int ruinIndex;

  public MapData(RobotController rc_) {
    rc = rc_;
    TEAM = rc.getTeam();
    MAP_WIDTH = rc.getMapWidth();
    MAP_HEIGHT = rc.getMapHeight();
    MAX_DISTANCE_SQ = MAP_WIDTH * MAP_WIDTH + MAP_HEIGHT * MAP_HEIGHT;

    mapData = new int[MAP_WIDTH * MAP_HEIGHT];
    knownRuins = new int[MAP_WIDTH / GameConstants.PATTERN_SIZE * MAP_HEIGHT / GameConstants.PATTERN_SIZE];
  }

  /**
   * Checks all visible squares around the robot and adds their information to the
   * mapData grid.
   * 
   */
  public void updateAllVisible() throws GameActionException {
    for (MapInfo info : rc.senseNearbyMapInfos()) {
      updateData(info);
    }
  }
  
  /**
   * Updates only the newly visible squares and updates them to the mapData grid
   * @param lastDir The Direction the robot just moved
   */
  public void updateNewlyVisible(Direction lastDir) throws GameActionException {
    if (lastDir == Direction.CENTER) { return; }
    // This needs to be updated if the vision radius changes
    assert GameConstants.VISION_RADIUS_SQUARED == 20;
    
    MapLocation current = rc.getLocation();
    Direction leftDir = lastDir.rotateLeft().rotateLeft();
    Direction rightDir = lastDir.rotateRight().rotateRight();
    // Cardinal direction
    if (lastDir.dx == 0 ^ lastDir.dy == 0) {
      MapLocation center = current.translate(4 * lastDir.dx, 4 * lastDir.dy);
      MapLocation leftLoc = center.add(leftDir);
      MapLocation rightLoc = center.add(rightDir);
      if (rc.onTheMap(center)) { updateData(rc.senseMapInfo(center)); }
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
      leftLoc = leftLoc.add(leftDir);
      rightLoc = rightLoc.add(rightDir);
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
      leftDir = leftDir.rotateLeft();
      rightDir = rightDir.rotateRight();
      leftLoc = leftLoc.add(leftDir);
      rightLoc = rightLoc.add(rightDir);
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
      leftLoc = leftLoc.add(leftDir);
      rightLoc = rightLoc.add(rightDir);
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
    } else {
      MapLocation center = current.translate(3 * lastDir.dx, 3 * lastDir.dy);
      MapLocation leftLoc = center.add(leftDir);
      MapLocation rightLoc = center.add(rightDir);
      if (rc.onTheMap(center)) { updateData(rc.senseMapInfo(center)); }
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
      leftDir = leftDir.rotateLeft();
      rightDir = rightDir.rotateRight();
      leftLoc = leftLoc.add(leftDir);
      rightLoc = rightLoc.add(rightDir);
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
      leftLoc = leftLoc.add(leftDir);
      rightLoc = rightLoc.add(rightDir);
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
      leftLoc = leftLoc.add(leftDir);
      rightLoc = rightLoc.add(rightDir);
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
      leftLoc = leftLoc.add(leftDir);
      rightLoc = rightLoc.add(rightDir);
      if (rc.onTheMap(leftLoc)) { updateData(rc.senseMapInfo(leftLoc)); }
      if (rc.onTheMap(rightLoc)) { updateData(rc.senseMapInfo(rightLoc)); }
      if (rc.onTheMap(center.add(leftDir))) { updateData(rc.senseMapInfo(center.add(leftDir))); }
      if (rc.onTheMap(center.add(rightDir))) { updateData(rc.senseMapInfo(center.add(rightDir))); }
    }
  }

  /**
   * Takes the info for a single space and updates the robot's knowledge with
   * that information.
   * 
   * @param info The `MapInfo` of the tile to be updated
   */
  public void updateData(MapInfo info) throws GameActionException {
    // Update this square
    MapLocation loc = info.getMapLocation();
    int index = getIndex(loc);

    // First time seeing square
    if (mapData[index] == 0) {
      if (info.hasRuin()) { mapData[index] = RUIN; knownRuins[ruinIndex++] = index; }
      else if (info.isWall()) { mapData[index] = WALL; }
      else { mapData[index] = EMPTY; }
      
      // If symmetry is not known, try to figure it out
      if (!symmetryKnown() && rc.getRoundNum() > 2) {
        if ((symmetryType & HORIZONTAL) > 0) {
          int h_index = symmetryIndex(index, HORIZONTAL);
          if (mapData[h_index] != UNKNOWN && mapData[h_index] != mapData[index]) {
            System.out.println("Ruled out HORIZONTAL");
            symmetryType ^= HORIZONTAL;
          }
        }
        if ((symmetryType & VERTICAL) > 0) {
          int v_index = symmetryIndex(index, VERTICAL);
          if (mapData[v_index] != UNKNOWN && mapData[v_index] != mapData[index]) {
            System.out.println("Ruled out VERTICAL");
            symmetryType ^= VERTICAL;
          }
        }
        if ((symmetryType & ROTATIONAL) > 0) {
          int r_index = symmetryIndex(index, ROTATIONAL);
          if (mapData[r_index] != UNKNOWN && mapData[r_index] != mapData[index]) {
            System.out.println("Ruled out ROTATIONAL");
            symmetryType ^= ROTATIONAL;
          }
        }
      }
    }
      
    // Copy data over symmetrically
    if (symmetryKnown()) {
      int symIndex = symmetryIndex(index, symmetryType);
      if (symIndex == UNKNOWN) {
        mapData[symIndex] = mapData[index] & TILE_TYPE_BITMASK;
        if (info.hasRuin()) { knownRuins[ruinIndex++] = index; }
      }
    }
    
    // If it is a ruin or empty, more info can be gathered
    if ((mapData[index] & TILE_TYPE_BITMASK) == RUIN) {
      RobotInfo towerInfo = rc.senseRobotAtLocation(loc);
      if (towerInfo != null) {
        int roundNum = rc.getRoundNum();
        mapData[index] &= ~LAST_UPDATED_BITMASK;
        mapData[index] |= roundNum << LAST_UPDATED_BITSHIFT;
        mapData[index] &= ~TILE_TYPE_BITMASK;
        mapData[index] |= switch (towerInfo.getType().getBaseType()) {
          case UnitType.LEVEL_ONE_DEFENSE_TOWER -> DEFENSE_TOWER;
          case UnitType.LEVEL_ONE_PAINT_TOWER -> PAINT_TOWER;
          case UnitType.LEVEL_ONE_MONEY_TOWER -> MONEY_TOWER;
          default -> 0;
        };
        if (towerInfo.getTeam().equals(TEAM)) { 
          mapData[index] |= FRIENDLY_TOWER;
        }
      } else {
        mapData[index] |= UNCLAIMED_RUIN;
      }
    } else if ((mapData[index] & TILE_TYPE_BITMASK) == EMPTY) {
      PaintType paint = info.getPaint();
      int roundNum = rc.getRoundNum();
      mapData[index] &= ~LAST_UPDATED_BITMASK;
      mapData[index] |= roundNum << LAST_UPDATED_BITSHIFT;
      mapData[index] &= ~PAINT_BITMASK;
      if (paint.isEnemy()) {
        mapData[index] |= ENEMY_PAINT;
      } else if (paint.isAlly()) {
        mapData[index] |= paint.isSecondary() ? FRIENDLY_SECONDARY : FRIENDLY_PRIMARY;
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
  public boolean passable(MapLocation loc) { return (readData(loc) & TILE_TYPE_BITMASK) < RUIN; }

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

  /**
   * Returns the closest known ruin (claimed or unclaimed) to the robot
   * @return The location of the closest known ruin
   */
  public MapLocation closestRuin() {
    if (ruinIndex == 0) { return null; }
    MapLocation current = rc.getLocation();
    MapLocation closestRuin = getLoc(knownRuins[0]);
    int closestDist = current.distanceSquaredTo(closestRuin);
    for (int i = 1; i < ruinIndex; ++i) {
      MapLocation ruinLoc = getLoc(knownRuins[i]);
      int ruinDist = current.distanceSquaredTo(ruinLoc);
      if (ruinDist < closestDist) {
        closestDist = ruinDist;
        closestRuin = ruinLoc;
      }
    }
    return closestRuin;
  }

  /**
   * Returns the closest known unclaimed (or unknown if claimed) ruin to the robot
   * @return The location of the closest known unclaimed ruin
   */
  public MapLocation closestUnclaimedRuin() {
    if (ruinIndex == 0) { return null; }
    MapLocation current = rc.getLocation();
    MapLocation closestTower = null;
    int closestDist = 0;
    for (int i = 0; i < ruinIndex; ++i) {
      if ((mapData[knownRuins[i]] & TOWER_TYPE_BITMASK) > UNCLAIMED_RUIN) { continue; }
      MapLocation towerLoc = getLoc(knownRuins[i]);
      int ruinDist = current.distanceSquaredTo(towerLoc);
      if (closestTower == null || ruinDist < closestDist) {
        closestTower = towerLoc;
        closestDist = ruinDist;
      }
    }
    return closestTower;
  }
  
  /**
   * Returns the closest known friendly tower to the robot
   * @return The location of the closest known friendly tower
   */
  public MapLocation closestFriendlyTower() throws GameActionException {
    if (ruinIndex == 0) { return null; }
    MapLocation current = rc.getLocation();
    MapLocation closestTower = null;
    int closestDist = 0;
    for (int i = 0; i < ruinIndex; ++i) {
      if ((mapData[knownRuins[i]] & TOWER_TYPE_BITMASK) == 0) { continue; }
      if ((mapData[knownRuins[i]] & TOWER_TYPE_BITMASK) == UNCLAIMED_RUIN) { continue; }
      if ((mapData[knownRuins[i]] & FRIENDLY_TOWER) == 0) { continue; }
      MapLocation towerLoc = getLoc(knownRuins[i]);
      int ruinDist = current.distanceSquaredTo(towerLoc);
      if (closestTower != null) {
        System.out.println(towerLoc.toString() + " " + ruinDist + " | " + closestTower.toString() + " " + closestDist);
      }
      if (closestTower == null || ruinDist < closestDist) {
        closestTower = towerLoc;
        closestDist = ruinDist;
      }
    }
    return closestTower;
  }

  /**
   * Returns the closest known enemy tower to the robot
   * @return The location of the closest known enemy tower
   */
  public MapLocation closestEnemyTower() {
    if (ruinIndex == 0) { return null; }
    MapLocation current = rc.getLocation();
    MapLocation closestTower = null;
    int closestDist = 0;
    for (int i = 0; i < ruinIndex; ++i) {
      if ((mapData[knownRuins[i]] & TOWER_TYPE_BITMASK) == 0) { continue; }
      if ((mapData[knownRuins[i]] & TOWER_TYPE_BITMASK) == UNCLAIMED_RUIN) { continue; }
      if ((mapData[knownRuins[i]] & FRIENDLY_TOWER) > 0) { continue; }
      MapLocation towerLoc = getLoc(knownRuins[i]);
      int ruinDist = current.distanceSquaredTo(towerLoc);
      if (closestTower == null || ruinDist < closestDist) {
        closestTower = towerLoc;
        closestDist = ruinDist;
      }
    }
    return closestTower;
  }
}