package jottesen_test.util;

/**
 * Simple fixed size stack helper class
 */
public class Stack<T> {
  
  private T[] data;
  private int index = 0;
  
  public Stack(int max_size) {
    data = (T[]) new Object[max_size];
  }

  /**
   * Adds an item onto the stack.
   * @param item The item to be added to the stack
   * @return Whether the item was successfully added to the stack
   */
  public boolean push(T item) {
    if (index == data.length) { return false; }
    data[index++] = item;
    return true;
  }

  /**
   * Removes an item from the stack.
   * @return The item which was removed, or `null` if empty
   */
  public T pop() {
    return (index == 0 ? null : data[--index]);
  }

  /**
   * Peeks at the next item on the stack without modifying it
   * @return The top item on the stack
   */
  public T top() {
    return (index == 0 ? null : data[index - 1]);
  }

  /**
   * Returns the item at the bottom of the stack without modification.
   * This is not a conventional stack method.
   * @return The first element in the array (bottom of the stack)
   */
  public T bottom() {
    return (index == 0 ? null : data[0]);
  }

  /**
   * Returns the total capacity of the stack
   * @return The size of the data array
   */
  public int capacity() { return data.length; }

  /**
   * Returns the usage of the stack
   * @return The number of elements filled in the array
   */
  public int used() { return index; }

  /**
   * Returns the available space in the stack
   * @return The number of unused spaces in the array
   */
  public int available() { return data.length - index; }

  /**
   * Returns whether the stack is empty
   * @return Whether the stack is empty
   */
  public boolean empty() { return index == 0; }

  /**
   * Removes all elements from the stack
   */
  public void clear() { index = 0; }
}

