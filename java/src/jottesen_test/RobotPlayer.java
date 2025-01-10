package jottesen_test;

import battlecode.common.*;

public class RobotPlayer {

  private static Robot robot;

  public static void run(RobotController rc) {
      robot = switch (rc.getType()) {
          case SOLDIER -> new Soldier(rc);
          case MOPPER -> new Mopper(rc);
          case SPLASHER -> new Splasher(rc);
          default -> new Tower(rc);
      };

    while (true) {
      try {
        robot.run();
      } catch (GameActionException e) {
        System.out.println(rc.getType() + " Exception");
        e.printStackTrace();
      } catch (Exception e) {
        System.out.println(rc.getType() + " Exception");
        e.printStackTrace();
      } finally {
        Clock.yield();
      }
    }
  }
}