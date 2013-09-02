package client.net.sf.saxon.ce.expr.z;

/**
 * An immutable integer set containing a single integer
 */
public class IntSingletonSet implements IntSet {

    private int value;

    public IntSingletonSet(int value) {
        this.value = value;
    }

    public int getMember() {
        return value;
    }

    public void clear() {
        throw new UnsupportedOperationException("IntSingletonSet is immutable");
    }

    public IntSet copy() {
        return this;
    }

    public IntSet mutableCopy() {
        IntHashSet intHashSet = new IntHashSet();
        intHashSet.add(value);
        return intHashSet;
    }

    public int size() {
        return 1;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean contains(int value) {
        return this.value == value;
    }

    public boolean remove(int value) {
        throw new UnsupportedOperationException("IntSingletonSet is immutable");
    }

    public boolean add(int value) {
        throw new UnsupportedOperationException("IntSingletonSet is immutable");
    }

    public IntIterator iterator() {
        return new IntIterator() {

            boolean gone = false;
            public boolean hasNext() {
                return !gone;
            }

            public int next() {
                gone = true;
                return value;
            }
        };
    }

    public IntSet union(IntSet other) {
        IntSet n = other.mutableCopy();
        n.add(value);
        return n;
    }

    public IntSet intersect(IntSet other) {
        if (other.contains(value)) {
            return this;
        } else {
            return new IntHashSet();  // return empty set
        }
    }

    public IntSet except(IntSet other) {
        if (other.contains(value)) {
            return new IntHashSet();  // return empty set
        } else {
            return this;
        }
    }

    public boolean containsAll(/*@NotNull*/ IntSet other) {
        if (other.size() > 1) {
            return false;
        }
        IntIterator ii = other.iterator();
        while (ii.hasNext()) {
            if (value != ii.next()) {
                return false;
            }
        }
        return true;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

