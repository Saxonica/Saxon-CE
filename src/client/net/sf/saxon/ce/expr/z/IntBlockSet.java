package client.net.sf.saxon.ce.expr.z;

import java.io.Serializable;

/**
 * Set of int values. This implementation of IntSet uses a sorted array
 * of integer ranges.
 *
 * @author Michael Kay
 */
public class IntBlockSet extends AbstractIntSet implements Serializable, IntSet  {

    private int startPoint;
    private int endPoint;

    // Hashcode, evaluated lazily
    private int hashCode = -1;

    /**
     * Create an IntRangeSet given the start point and end point of the integer range.
     * @param startPoint the start point of the integer range
     * @param endPoint the end point of the integer range
     * @throws IllegalArgumentException if the two arrays are different lengths. Other error conditions
     * in the input are not currently detected.
     */

    public IntBlockSet(int startPoint, int endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public IntSet copy() {
        return this;
    }

    public IntSet mutableCopy() {
        return new IntRangeSet(new int[]{startPoint}, new int[]{endPoint});
    }

    public int size() {
        return endPoint - startPoint;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(int value) {
        return value >= startPoint && value <= endPoint;
    }

    public boolean remove(int value) {
        throw new UnsupportedOperationException("remove");
    }

    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    /**
     * Add an integer to the set
     * @param value the integer to be added
     * @return true if the integer was added, false if it was already present
     */

    public boolean add(int value) {
        throw new UnsupportedOperationException("add");
    }

    /**
     * Get an iterator over the values
     */

    public IntIterator iterator() {
        return mutableCopy().iterator();
    }



    public String toString() {
        return startPoint + " - " + endPoint;
    }

    /**
     * Test whether this set has exactly the same members as another set. Note that
     * IntBlockSet values are <b>NOT</b> comparable with other implementations of IntSet
     */

    public boolean equals(Object other) {
        return mutableCopy().equals(other);
    }

    /**
     * Construct a hash key that supports the equals() test
     */

    public int hashCode() {
        // Note, hashcodes are NOT the same as those used by IntHashSet and IntArraySet
        if (hashCode == -1) {
            hashCode = 0x836a89f1 ^ (startPoint + (endPoint<<3));
        }
        return hashCode;
    }

    /**
     * Get the start point of the range
     */

    public int getStartPoint() {
        return startPoint;
    }

    /**
     * Get the end point of the range
     */

    public int getEndPoint() {
        return endPoint;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.