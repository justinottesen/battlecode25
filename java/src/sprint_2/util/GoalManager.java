package sprint_2.util;

import battlecode.common.*;

/**
 * THIS CLASS WILL NEVER LET THE GOAL STACK BE EMPTY.
 * IF NO GOAL IS SPECIFIED, IT WILL DEFAULT TO EXPLORE
 */
public class GoalManager {

  private final RobotController rc;
  private final Pathfinding pathfinding;
  private final MapData mapData;
  
  private final Stack<Goal> goalStack;
  private final int GOAL_STACK_SIZE = 10;

  public GoalManager(RobotController rc, Pathfinding pathfinding, MapData mapData) {
    this.rc = rc;
    this.pathfinding = pathfinding;
    this.mapData = mapData;
    goalStack = new Stack<Goal>(GOAL_STACK_SIZE);
    goalStack.push(new Goal(Goal.Type.EXPLORE, mapData.getExploreTarget()));
  }

  /**
   * Gets the current goal
   * @return The current goal
   */
  public Goal current() { return goalStack.top(); }

  /**
   * Pushes a goal to the stack
   * @param type The type of the goal to push
   * @param target The target location of the goal
   */
  public boolean pushGoal(Goal.Type type, MapLocation target) { return pushGoal(new Goal(type, target)); }

  /**
   * Pushes a goal to the stack
   * @param goal The goal to push to the stack
   */
  public boolean pushGoal(Goal goal) {
    if (goalStack.push(goal)) {
      pathfinding.setTarget(goal);
      return true;
    }
    return false;
  }

  /**
   * Pops a goal off the stack, setting the next goal as the active goal, or
   * if no other goal is present, the default goal of explore
   */
  public void popGoal() { popGoal(new Goal(Goal.Type.EXPLORE, mapData.getExploreTarget())); }

  /**
   * Pops a goal off the stack, setting the next goal as the 
   * active goal, or `fallback` if there isn't one
   * @param fallback The goal to default to if there is no secondary goal
   */
  public void popGoal(Goal fallback) {
    goalStack.pop();
    if (goalStack.empty()) { goalStack.push(fallback); }
    pathfinding.setTarget(goalStack.top());
  }

  /**
   * Clears the stack and sets a new goal
   * @param type The type of the new goal to set
   * @param target The location to set as the target for the goal
   */
  public void setNewGoal(Goal.Type type, MapLocation target) { setNewGoal(new Goal(type, target)); }

  /**
   * Clears the stack and sets a new goal
   * @param goal The new goal to set
   */
  public void setNewGoal(Goal goal) {
    goalStack.clear();
    goalStack.push(goal);
    pathfinding.setTarget(goal);
  }

  /**
   * Replaces the current goal with the specified goal, preserving the rest of the stack
   * @param type The type of the new goal to set
   * @param target The location to set as the target for the goal
   */
  public void replaceTopGoal(Goal.Type type, MapLocation target) { replaceTopGoal(new Goal(type, target)); }

  /**
   * Replaces the current goal with the specified goal, preserving the rest of the stack
   * @param goal The new goal to set
   */
  public void replaceTopGoal(Goal goal) {
    goalStack.pop();
    goalStack.push(goal);
    pathfinding.setTarget(goal);
  }
}
