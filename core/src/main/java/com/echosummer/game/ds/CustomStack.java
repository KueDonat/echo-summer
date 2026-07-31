package com.echosummer.game.ds;

import com.echosummer.game.ds.adt.IStack;
import java.util.EmptyStackException;

/**
 * Custom LIFO Stack implementation using a singly linked node chain.
 *
 * @param <T> Element type
 */
public class CustomStack<T> implements IStack<T>, Iterable<T> {
    private static class Node<E> {
        E data;
        Node<E> next;

        Node(E data, Node<E> next) {
            this.data = data;
            this.next = next;
        }
    }

    private Node<T> top;
    private int size;

    public CustomStack() {
        this.top = null;
        this.size = 0;
    }

    @Override
    public void push(T item) {
        top = new Node<>(item, top);
        size++;
    }

    @Override
    public T pop() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        T data = top.data;
        top = top.next;
        size--;
        return data;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return top.data;
    }

    @Override
    public boolean isEmpty() {
        return top == null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        top = null;
        size = 0;
    }

    public java.util.List<T> toList() {
        java.util.List<T> list = new java.util.ArrayList<>();
        Node<T> curr = top;
        while (curr != null) {
            list.add(curr.data);
            curr = curr.next;
        }
        return list;
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return toList().iterator();
    }
}
