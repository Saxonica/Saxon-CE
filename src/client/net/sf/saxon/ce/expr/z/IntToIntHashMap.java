package client.net.sf.saxon.ce.expr.z;

import java.io.Serializable;

/**
 * A hash table that maps int keys to int values.
 */
public class IntToIntHashMap implements Serializable, IntToIntMap {

    /**
     * Initializes a map with a capacity of 8 and a load factor of 0,25.
     */
    public IntToIntHashMap() {
        this(8, 0.25);
    }

    /**
     * Initializes a map with the given capacity and a load factor of 0,25.
     *
     * @param capacity the initial capacity.
     */
    public IntToIntHashMap(int capacity) {
        this(capacity, 0.25);
    }

    /**
     * Constructs a new map with initial capacity, and load factor.
     * <p/>
     * The capacity is the number of keys that can be mapped without resizing
     * the arrays in which keys and values are stored. For efficiency, only
     * a fraction of the elements in those arrays are used. That fraction is
     * the specified load factor. The initial length of the arrays equals the
     * smallest power of two not less than the ratio capacity/factor. The
     * capacity of the map is increased, as necessary. The maximum number
     * of keys that can be mapped is 2^30.
     *
     * @param capacity the initial capacity.
     * @param factor   the load factor.
     */
    public IntToIntHashMap(int capacity, double factor) {
        _factor = factor;
        setCapacity(capacity);
    }

    /**
     * Set the value to be returned to indicate an unused entry
     * @param defaultValue the value to be returned by {@link #get(int)} if no entry
     * exists for the supplied key
     */
    public void setDefaultValue(int defaultValue) {
        _defaultValue = defaultValue;
    }

    /**
     * Get the default value used to indicate an unused entry
     * @return the value to be returned by {@link #get(int)} if no entry
     * exists for the supplied key
     */

    public int getDefaultValue() {
        return _defaultValue;
    }

    /**
     * Clears the map.
     */
    public void clear() {
        _n = 0;
        for (int i = 0; i < _nmax; ++i) {
            _filled[i] = false;
        }
    }

    /**
     * Finds a key in the map.
     *
     * @param key Key
     * @return true if the key is mapped
     */
    public boolean find(int key) {
        return _filled[indexOf(key)];
    }

    /**
     * Gets the value for this key.
     *
     * @param key Key
     * @return the value, or the default value if not found.
     */
    public int get(int key) {
        int i = indexOf(key);
        return _filled[i] ? _value[i] : _defaultValue;
    }

    /**
     * Gets the size of the map.
     *
     * @return the size
     */
    public int size() {
        return _n;
    }

    /**
     * Removes a key from the map.
     *
     * @param key Key to remove
     * @return true if the value was removed
     */
    public boolean remove(int key) {
        // Knuth, v. 3, 527, Algorithm R.
        int i = indexOf(key);
        if (!_filled[i]) {
            return false;
        }
        --_n;
        for (; ;) {
            _filled[i] = false;
            int j = i;
            int r;
            do {
                i = (i - 1) & _mask;
                if (!_filled[i]) {
                    return true;
                }
                r = hash(_key[i]);
            } while ((i <= r && r < j) || (r < j && j < i) || (j < i && i <= r));
            _key[j] = _key[i];
            _value[j] = _value[i];
            _filled[j] = _filled[i];
        }
    }

    /**
     * Adds a key-value pair to the map.
     *
     * @param key   Key
     * @param value Value
     */
    public void put(int key, int value) {
        int i = indexOf(key);
        if (_filled[i]) {
            _value[i] = value;
        } else {
            _key[i] = key;
            _value[i] = value;
            _filled[i] = true;
            grow();
        }
    }

    /**
     * Get an iterator over the integer key values held in the hash map
     * @return an iterator whose next() call returns the key values (in arbitrary order)
     */

    /*@NotNull*/ public IntIterator keyIterator() {
        return new IntToIntHashMapKeyIterator();
    }


    ///////////////////////////////////////////////////////////////////////////
    // private

    private static final int NBIT = 30; // NMAX = 2^NBIT
    private static final int NMAX = 1 << NBIT; // maximum number of keys mapped
    private double _factor; // 0.0 <= _factor <= 1.0
    private int _defaultValue = Integer.MAX_VALUE;
    private int _nmax; // 0 <= _nmax = 2^nbit <= 2^NBIT = NMAX
    private int _n; // 0 <= _n <= _nmax <= NMAX
    private int _nlo; // _nmax*_factor (_n<=_nlo, if possible)
    private int _nhi; //  NMAX*_factor (_n< _nhi, if possible)
    private int _shift; // _shift = 1 + NBIT - nbit (see function hash() below)
    private int _mask; // _mask = _nmax - 1
    private int[] _key; // array[_nmax] of keys
    //@SuppressWarnings(value = {"unchecked"})
    private int[] _value; // array[_nmax] of values
    private boolean[] _filled; // _filled[i]==true iff _key[i] is mapped

    private int hash(int key) {
        // Knuth, v. 3, 509-510. Randomize the 31 low-order bits of c*key
        // and return the highest nbits (where nbits <= 30) bits of these.
        // The constant c = 1327217885 approximates 2^31 * (sqrt(5)-1)/2.
        return ((1327217885 * key) >> _shift) & _mask;
    }

    private int indexOf(int key) {
        int i = hash(key);
        while (_filled[i]) {
            if (_key[i] == key) {
                return i;
            }
            i = (i - 1) & _mask;
        }
        return i;
    }

    private void grow() {
        ++_n;
        if (_n > NMAX) {
            throw new RuntimeException("number of keys mapped exceeds " + NMAX);
        }
        if (_nlo < _n && _n <= _nhi) {
            setCapacity(_n);
        }
    }

    private void setCapacity(int capacity) {
        if (capacity < _n) {
            capacity = _n;
        }
        double factor = (_factor < 0.01) ? 0.01 : (_factor > 0.99) ? 0.99 : _factor;
        int nbit, nmax;
        for (nbit = 1, nmax = 2; nmax * factor < capacity && nmax < NMAX; ++nbit, nmax *= 2) {
            // no-op
        }
        int nold = _nmax;
        if (nmax == nold) {
            return;
        }
        _nmax = nmax;
        _nlo = (int)(nmax * factor);
        _nhi = (int)(NMAX * factor);
        _shift = 1 + NBIT - nbit;
        _mask = nmax - 1;
        int[] key = _key;
        int[] value = _value;
        boolean[] filled = _filled;
        _n = 0;
        _key = new int[nmax];
        // semantically equivalent to _value = new V[nmax]
        _value = new int[nmax];
        _filled = new boolean[nmax];
        if (key != null) {
            for (int i = 0; i < nold; ++i) {
                if (filled[i]) {
                    put(key[i], value[i]);
                }
            }
        }
    }

     /**
     * Iterator over keys
     */
    private class IntToIntHashMapKeyIterator implements IntIterator, Serializable {

        private int i = 0;
        private static final long serialVersionUID = -5978261613309710617L;

        public IntToIntHashMapKeyIterator() {
            i = 0;
        }

        public boolean hasNext() {
            while (i < _key.length) {
                if (_filled[i]) {
                    return true;
                } else {
                    i++;
                }
            }
            return false;
        }

        public int next() {
            return _key[i++];
        }
    }

}

// Contributors: Dave Hale (Landmark Graphics), Dominique Devienne (Landmark Graphics), Michael Kay (Saxonica)

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

