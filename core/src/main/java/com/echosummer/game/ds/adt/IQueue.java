package com.echosummer.game.ds.adt;

/**
 * Abstract Data Type (ADT) Interface for Queue (FIFO - First In First Out).
 *
 * @param <T> Element type
 */
public interface IQueue<T> {
    /**
     * Inserts the specified element into this queue.
     */
    void enqueue(T item);

    /**
     * Retrieves and removes the head of this queue.
     */
    T dequeue();

    /**
     * Retrieves, but does not remove, the head of this queue.
     */
    T peek();

    /**
     * Tests if this queue is empty.
     */
    boolean isEmpty();

    /**
     * Returns the number of elements in this queue.
     */
    int size();

    /**
     * Removes all elements from this queue.
     */
    void clear();
}
