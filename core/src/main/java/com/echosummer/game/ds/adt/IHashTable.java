package com.echosummer.game.ds.adt;

import java.util.List;

/**
 * Abstract Data Type (ADT) Interface for Hash Table.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public interface IHashTable<K, V> {
    /**
     * Associates the specified value with the specified key in this map.
     */
    void put(K key, V value);

    /**
     * Returns the value to which the specified key is mapped, or null if no mapping exists.
     */
    V get(K key);

    /**
     * Removes the mapping for a key from this map if it is present.
     */
    V remove(K key);

    /**
     * Returns true if this map contains a mapping for the specified key.
     */
    boolean containsKey(K key);

    /**
     * Returns the number of key-value mappings in this map.
     */
    int size();

    /**
     * Returns true if this map contains no key-value mappings.
     */
    boolean isEmpty();

    /**
     * Returns a List view of the keys contained in this map.
     */
    List<K> keys();

    /**
     * Returns a List view of the values contained in this map.
     */
    List<V> values();

    /**
     * Removes all of the mappings from this map.
     */
    void clear();
}
