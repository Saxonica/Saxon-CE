package client.net.sf.saxon.ce.expr.z;

import java.io.Serializable;

/**
 * An implementation of {@link IntToIntMap} that relies on serial searching, and
 * is therefore optimized for very small map sizes
 */
public class IntToIntArrayMap implements IntToIntMap, Serializable {

    private int[] keys;
    private int[] values;
    private int used = 0;
    private int defaultValue = Integer.MIN_VALUE;

    /**
     * Create an initial empty map with default space allocation
     */

    public IntToIntArrayMap() {
        keys = new int[8];
        values = new int[8];
    }

    /**
     * Create an initial empty map with a specified initial capacity
     * @param capacity the initial capacity (the number of entries that can be held
     * before more space is allocated)
     */

    public IntToIntArrayMap(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        keys = new int[capacity];
        values = new int[capacity];
    }

    /**
     * Clear the map.
     */
    public void clear() {
        used = 0;
    }

    /**
     * Finds a key in the map.
     *
     * @param key Key
     * @return true if the key is mapped
     */
    public boolean find(int key) {
        for (int i=0; i<used; i++) {
            if (keys[i] == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the value for this key.
     *
     * @param key Key
     * @return the value, or the default value if not found.
     */
    public int get(int key) {
        for (int i=0; i<used; i++) {
            if (keys[i] == key) {
                return values[i];
            }
        }
        return defaultValue;
    }

    /**
     * Get the default value used to indicate an unused entry
     *
     * @return the value to be returned by {@link #get(int)} if no entry
     *         exists for the supplied key
     */

    public int getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get an iterator over the integer key values held in the hash map.
     * <p>The contents of the hash map must not be modified while this iterator remains in use</p>
     * @return an iterator whose next() call returns the key values (in arbitrary order)
     */

    /*@NotNull*/ public IntIterator keyIterator() {
        return new KeyIterator();
    }

    /**
     * Adds a key-value pair to the map.
     *
     * @param key   Key
     * @param value Value
     */
    public void put(int key, int value) {
        for (int i=0; i<used; i++) {
            if (keys[i] == key) {
                values[i] = value;
                return;
            }
        }
        if (used >= keys.length) {
            int[] k2 = new int[used*2];
            System.arraycopy(keys, 0, k2, 0, used);
            keys = k2;
            int[] v2 = new int[used*2];
            System.arraycopy(values, 0, v2, 0, used);
            values = v2;
        }
        keys[used] = key;
        values[used++] = value;
    }

    /**
     * Removes a key from the map.
     *
     * @param key Key to remove
     * @return true if the value was removed
     */
    public boolean remove(int key) {
        for (int i=0; i<used; i++) {
            if (keys[i] == key) {
                values[i] = defaultValue;
                return true;
            }
        }
        return false;
    }

    /**
     * Set the value to be returned to indicate an unused entry
     *
     * @param defaultValue the value to be returned by {@link #get(int)} if no entry
     *                     exists for the supplied key
     */
    public void setDefaultValue(int defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the size of the map.
     *
     * @return the size
     */
    public int size() {
        return used;
    }

    private class KeyIterator implements IntIterator, Serializable {

        private int i = 0;
        private static final long serialVersionUID = 1720894017771245276L;

        public KeyIterator() {
            i = 0;
        }

        public boolean hasNext() {
            return i < used;
        }

        public int next() {
            return keys[i++];
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.