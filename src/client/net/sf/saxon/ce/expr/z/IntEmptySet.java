package client.net.sf.saxon.ce.expr.z;

/**
 * An immutable integer set containing no integers
 */
public class IntEmptySet implements IntSet {

    private static IntEmptySet THE_INSTANCE = new IntEmptySet();

    public static IntEmptySet getInstance() {
        return THE_INSTANCE;
    }


    private IntEmptySet() {
        // no action
    }

    public IntSet copy() {
        return this;
    }

    public IntSet mutableCopy() {
        return new IntHashSet();
    }

    public void clear() {
        throw new UnsupportedOperationException("IntEmptySet is immutable");
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean contains(int value) {
        return false;
    }

    public boolean remove(int value) {
        throw new UnsupportedOperationException("IntEmptySet is immutable");
    }

    public boolean add(int value) {
        throw new UnsupportedOperationException("IntEmptySet is immutable");
    }

    public IntIterator iterator() {
        return new IntIterator() {
            public boolean hasNext() {
                return false;
            }

            public int next() {
                return Integer.MIN_VALUE;
            }
        };
    }

    public IntSet union(IntSet other) {
        return other.copy();
    }

    public IntSet intersect(IntSet other) {
        return this;
    }

    public IntSet except(IntSet other) {
        return this;
    }

    public boolean containsAll(/*@NotNull*/ IntSet other) {
        return other.isEmpty();
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.



