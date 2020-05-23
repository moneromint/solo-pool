package com.moneromint.solo.utils;

public class CircularBuffer<E> {
    private final E[] data;
    private int write;
    private int size;

    @SuppressWarnings("unchecked")
    public CircularBuffer(int capacity) {
        data = (E[]) new Object[capacity];
    }

    public void add(E e) {
        data[write] = e;
        write = (write + 1) % data.length;
        size = Math.min(size + 1, data.length);
    }

    public E get(int idx) {
        if (idx >= size) {
            throw new IllegalArgumentException("idx out of range");
        }
        return data[idx];
    }

    public int getCapacity() {
        return data.length;
    }

    public int getSize() {
        return size;
    }
}
