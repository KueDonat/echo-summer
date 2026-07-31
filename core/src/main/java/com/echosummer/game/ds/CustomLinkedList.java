package com.echosummer.game.ds;

import com.echosummer.game.ds.adt.ILinkedList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Custom Doubly Linked List implementation.
 *
 * @param <T> Element type
 */
public class CustomLinkedList<T> implements ILinkedList<T> {
    private static class Node<E> {
        E data;
        Node<E> prev;
        Node<E> next;

        Node(E data, Node<E> prev, Node<E> next) {
            this.data = data;
            this.prev = prev;
            this.next = next;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public CustomLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public void add(T item) {
        Node<T> newNode = new Node<>(item, tail, null);
        if (isEmpty()) {
            head = newNode;
        } else {
            tail.next = newNode;
        }
        tail = newNode;
        size++;
    }

    @Override
    public void add(int index, T item) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        if (index == size) {
            add(item);
            return;
        }
        Node<T> curr = getNode(index);
        Node<T> newNode = new Node<>(item, curr.prev, curr);
        if (curr.prev != null) {
            curr.prev.next = newNode;
        } else {
            head = newNode;
        }
        curr.prev = newNode;
        size++;
    }

    @Override
    public T get(int index) {
        return getNode(index).data;
    }

    private Node<T> getNode(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Node<T> curr;
        if (index < size / 2) {
            curr = head;
            for (int i = 0; i < index; i++) curr = curr.next;
        } else {
            curr = tail;
            for (int i = size - 1; i > index; i--) curr = curr.prev;
        }
        return curr;
    }

    @Override
    public boolean remove(T item) {
        Node<T> curr = head;
        while (curr != null) {
            if ((item == null && curr.data == null) || (item != null && item.equals(curr.data))) {
                unlink(curr);
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    @Override
    public T remove(int index) {
        Node<T> curr = getNode(index);
        unlink(curr);
        return curr.data;
    }

    private void unlink(Node<T> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
        size--;
    }

    @Override
    public boolean contains(T item) {
        Node<T> curr = head;
        while (curr != null) {
            if ((item == null && curr.data == null) || (item != null && item.equals(curr.data))) {
                return true;
            }
            curr = curr.next;
        }
        return false;
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
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                T item = current.data;
                current = current.next;
                return item;
            }
        };
    }
}
