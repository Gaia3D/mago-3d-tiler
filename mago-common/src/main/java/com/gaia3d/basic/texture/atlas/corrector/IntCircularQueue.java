package com.gaia3d.basic.texture.atlas.corrector;

public class IntCircularQueue {

    private final int maximumCapacity;
    private int[] values;
    private int head;
    private int size;
    private int maximumObservedSize;

    public IntCircularQueue(int initialCapacity, int maximumCapacity) {
        if (maximumCapacity <= 0) {
            throw new IllegalArgumentException("maximumCapacity must be greater than zero");
        }

        this.maximumCapacity = maximumCapacity;

        int safeInitialCapacity = Math.max(1, Math.min(initialCapacity, maximumCapacity));

        this.values = new int[safeInitialCapacity];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public int getMaximumObservedSize() {
        return maximumObservedSize;
    }

    public void add(int value) {
        if (size == values.length) {
            grow();
        }

        /*
         * La posición tail se calcula con head + size.
         *
         * Como size nunca es mayor que values.length,
         * basta una única corrección en lugar de usar %.
         */
        int tail = head + size;

        if (tail >= values.length) {
            tail -= values.length;
        }

        values[tail] = value;

        size++;

        if (size > maximumObservedSize) {
            maximumObservedSize = size;
        }
    }

    public int remove() {
        if (size == 0) {
            throw new IllegalStateException("Cannot remove from an empty queue");
        }

        int value = values[head];

        head++;

        if (head == values.length) {
            head = 0;
        }

        size--;

        return value;
    }

    private void grow() {
        int oldCapacity = values.length;

        if (oldCapacity >= maximumCapacity) {
            throw new IllegalStateException("Circular queue exceeded its maximum capacity: " + maximumCapacity);
        }

        /*
         * Utilizamos long para evitar overflow al duplicar.
         */
        long doubledCapacity = (long) oldCapacity * 2L;

        int newCapacity = (int) Math.min(maximumCapacity, Math.max(oldCapacity + 1L, doubledCapacity));

        int[] newValues = new int[newCapacity];

        /*
         * La cola puede estar dividida en dos fragmentos:
         *
         * [parte final del array] + [parte inicial del array]
         */
        int firstPartLength = Math.min(size, oldCapacity - head);

        System.arraycopy(values, head, newValues, 0, firstPartLength);

        int secondPartLength = size - firstPartLength;

        if (secondPartLength > 0) {
            System.arraycopy(values, 0, newValues, firstPartLength, secondPartLength);
        }

        values = newValues;

        head = 0;
    }
}
