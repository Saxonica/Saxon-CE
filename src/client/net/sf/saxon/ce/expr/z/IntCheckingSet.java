package client.net.sf.saxon.ce.expr.z;

/**
 * An immutable integer set where membership is tested algorithmically
 */
public abstract class IntCheckingSet implements IntSet {

    public void clear() {
        throw new UnsupportedOperationException("IntCheckingSet is immutable");
    }

    public IntSet copy() {
        return this;
    }

    public IntSet mutableCopy() {
        throw new UnsupportedOperationException("IntCheckingSet cannot be copied");
    }

    public int size() {
        return Integer.MAX_VALUE;
    }

    public boolean isEmpty() {
        return false;
    }

    public abstract boolean contains(int value);

    public boolean remove(int value) {
        throw new UnsupportedOperationException("IntCheckingSet is immutable");
    }

    public boolean add(int value) {
        throw new UnsupportedOperationException("IntCheckingSet is immutable");
    }

    public IntIterator iterator() {
        throw new UnsupportedOperationException("Cannot iterate over IntCheckingSet");
    }

    public IntSet union(final IntSet other) {
        final IntSet is = this;
        return new IntCheckingSet() {
            @Override
            public boolean contains(int value) {
                return is.contains(value) || other.contains(value);
            }
        };
    }

    public IntSet intersect(final IntSet other) {
        final IntSet is = this;
        return new IntCheckingSet() {
            @Override
            public boolean contains(int value) {
                return is.contains(value) && other.contains(value);
            }
        };
    }

    public IntSet except(final IntSet other) {
        final IntSet is = this;
        return new IntCheckingSet() {
            @Override
            public boolean contains(int value) {
                return is.contains(value) && !other.contains(value);
            }
        };
    }

    public boolean containsAll(/*@NotNull*/ IntSet other) {
        IntIterator ii = other.iterator();
        while (ii.hasNext()) {
            if (!contains(ii.next())) {
                return false;
            }
        }
        return true;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

