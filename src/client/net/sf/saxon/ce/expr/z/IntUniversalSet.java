package client.net.sf.saxon.ce.expr.z;

/**
 * An immutable integer set containing every integer
 */
public class IntUniversalSet implements IntSet {

    private static IntUniversalSet THE_INSTANCE = new IntUniversalSet();

    public static IntUniversalSet getInstance() {
        return THE_INSTANCE;
    }


    private IntUniversalSet() {
        // no action
    }

    public IntSet copy() {
        return this;
    }

    public IntSet mutableCopy() {
        return new IntComplementSet(new IntHashSet());
    }

    public void clear() {
        throw new UnsupportedOperationException("IntUniversalSet is immutable");
    }

    public int size() {
        return Integer.MAX_VALUE;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean contains(int value) {
        return true;
    }

    public boolean remove(int value) {
        throw new UnsupportedOperationException("IntUniversalSet is immutable");
    }

    public boolean add(int value) {
        throw new UnsupportedOperationException("IntUniversalSet is immutable");
    }

    public IntIterator iterator() {
        throw new UnsupportedOperationException("Cannot enumerate an infinite set");
    }

    public IntSet union(IntSet other) {
        return this;
    }

    public IntSet intersect(IntSet other) {
        return other.copy();
    }

    public IntSet except(IntSet other) {
        if (other instanceof IntUniversalSet) {
            return new IntHashSet();  // return empty set
        } else {
            return new IntComplementSet(other.copy());
        }
    }

    public boolean containsAll(/*@NotNull*/ IntSet other) {
        return true;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


