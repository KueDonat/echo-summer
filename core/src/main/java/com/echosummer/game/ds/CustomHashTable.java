package com.echosummer.game.ds;

import com.echosummer.game.ds.adt.IHashTable;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Hash Table implementation using separate chaining buckets for collision resolution.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class CustomHashTable<K, V> implements IHashTable<K, V> {
    private static class Entry<K, V> {
        K key;
        V value;
        Entry<K, V> next;

        Entry(K key, V value, Entry<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final int DEFAULT_CAPACITY = 31;
    private static final float LOAD_FACTOR = 0.75f;

    private Entry<K, V>[] buckets;
    private int capacity;
    private int size;

    @SuppressWarnings("unchecked")
    public CustomHashTable(int initialCapacity) {
        this.capacity = initialCapacity;
        this.buckets = new Entry[capacity];
        this.size = 0;
    }

    public CustomHashTable() {
        this(DEFAULT_CAPACITY);
    }

    private int getBucketIndex(K key) {
        if (key == null) return 0;
        int hashCode = key.hashCode();
        return Math.abs(hashCode % capacity);
    }

    @Override
    public void put(K key, V value) {
        if ((float) size / capacity >= LOAD_FACTOR) {
            rehash();
        }

        int index = getBucketIndex(key);
        Entry<K, V> head = buckets[index];

        while (head != null) {
            if ((key == null && head.key == null) || (key != null && key.equals(head.key))) {
                head.value = value;
                return;
            }
            head = head.next;
        }

        Entry<K, V> newEntry = new Entry<>(key, value, buckets[index]);
        buckets[index] = newEntry;
        size++;
    }

    @Override
    public V get(K key) {
        int index = getBucketIndex(key);
        Entry<K, V> head = buckets[index];
        while (head != null) {
            if ((key == null && head.key == null) || (key != null && key.equals(head.key))) {
                return head.value;
            }
            head = head.next;
        }
        return null;
    }

    @Override
    public V remove(K key) {
        int index = getBucketIndex(key);
        Entry<K, V> head = buckets[index];
        Entry<K, V> prev = null;

        while (head != null) {
            if ((key == null && head.key == null) || (key != null && key.equals(head.key))) {
                if (prev != null) {
                    prev.next = head.next;
                } else {
                    buckets[index] = head.next;
                }
                size--;
                return head.value;
            }
            prev = head;
            head = head.next;
        }
        return null;
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public List<K> keys() {
        List<K> keyList = new ArrayList<>();
        for (Entry<K, V> bucket : buckets) {
            Entry<K, V> curr = bucket;
            while (curr != null) {
                keyList.add(curr.key);
                curr = curr.next;
            }
        }
        return keyList;
    }

    @Override
    public List<V> values() {
        List<V> valList = new ArrayList<>();
        for (Entry<K, V> bucket : buckets) {
            Entry<K, V> curr = bucket;
            while (curr != null) {
                valList.add(curr.value);
                curr = curr.next;
            }
        }
        return valList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void clear() {
        capacity = DEFAULT_CAPACITY;
        buckets = new Entry[capacity];
        size = 0;
    }

    @SuppressWarnings("unchecked")
    private void rehash() {
        int oldCapacity = capacity;
        Entry<K, V>[] oldBuckets = buckets;

        capacity = oldCapacity * 2 + 1;
        buckets = new Entry[capacity];
        size = 0;

        for (int i = 0; i < oldCapacity; i++) {
            Entry<K, V> curr = oldBuckets[i];
            while (curr != null) {
                put(curr.key, curr.value);
                curr = curr.next;
            }
        }
    }
}
