package com.echosummer.game.ds.adt;

/**
 * Abstract Data Type (ADT) Interface for Stack (LIFO - Last In First Out).
 *
 * @param <T> Element type
 */
public interface IStack<T> {
    /**
     * Pushes an item onto the top of this stack.
     */
    void push(T item);

    /**
     * Removes and returns the item at the top of this stack.
     */
    T pop();

    /**
     * Looks at the item at the top of this stack without removing it.
     */
    T peek();

    /**
     * Tests if this stack is empty.
     */
    boolean isEmpty();

    /**
     * Returns the number of items in this stack.
     */
    int size();

    /**
     * Removes all items from this stack.
     */
    void clear();
}
