package client.net.sf.saxon.ce.expr.z;

import java.io.Serializable;

/**
 *  Interface defining a map from integers to integers
 */
public interface IntToIntMap extends Serializable {
    /**
     * Set the value to be returned to indicate an unused entry
     * @param defaultValue the value to be returned by {@link #get(int)} if no entry
     * exists for the supplied key
     */
    void setDefaultValue(int defaultValue);

    /**
     * Get the default value used to indicate an unused entry
     * @return the value to be returned by {@link #get(int)} if no entry
     * exists for the supplied key
     */

    int getDefaultValue();

    /**
     * Clear the map.
     */
    void clear();

    /**
     * Finds a key in the map.
     *
     * @param key Key
     * @return true if the key is mapped
     */
    boolean find(int key);

    /**
     * Gets the value for this key.
     *
     * @param key Key
     * @return the value, or the default value if not found.
     */
    int get(int key);

    /**
     * Gets the size of the map.
     *
     * @return the size
     */
    int size();

    /**
     * Removes a key from the map.
     *
     * @param key Key to remove
     * @return true if the value was removed
     */
    boolean remove(int key);

    /**
     * Adds a key-value pair to the map.
     *
     * @param key   Key
     * @param value Value
     */
    void put(int key, int value);

    /**
     * Get an iterator over the integer key values held in the hash map
     * @return an iterator whose next() call returns the key values (in arbitrary order)
     */

    /*@NotNull*/ IntIterator keyIterator();
}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.