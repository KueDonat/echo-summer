package com.echosummer.game.ds.adt;

/**
 * Abstract Data Type (ADT) Interface for Linked List.
 *
 * @param <T> Element type
 */
public interface ILinkedList<T> extends Iterable<T> {
    /**
     * Appends the specified element to the end of this list.
     */
    void add(T item);

    /**
     * Inserts the specified element at the specified position.
     */
    void add(int index, T item);

    /**
     * Returns the element at the specified position in this list.
     */
    T get(int index);

    /**
     * Removes the first occurrence of the specified element from this list.
     */
    boolean remove(T item);

    /**
     * Removes the element at the specified position in this list.
     */
    T remove(int index);

    /**
     * Returns true if this list contains the specified element.
     */
    boolean contains(T item);

    /**
     * Returns the number of elements in this list.
     */
    int size();

    /**
     * Tests if this list is empty.
     */
    boolean isEmpty();

    /**
     * Removes all elements from this list.
     */
    void clear();
}
