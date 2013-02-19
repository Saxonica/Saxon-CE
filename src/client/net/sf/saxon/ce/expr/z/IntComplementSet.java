package client.net.sf.saxon.ce.expr.z;

/**
 * An immutable integer set containing all int values except those in an excluded set
 */
public class IntComplementSet implements IntSet {

    private IntSet exclusions;

    public IntComplementSet(IntSet exclusions) {
        this.exclusions = exclusions.copy();
    }

    public IntSet copy() {
        return new IntComplementSet(exclusions);
    }

    public IntSet mutableCopy() {
        return copy();
    }

    public void clear() {
        throw new UnsupportedOperationException("IntComplementSet cannot be emptied");
    }

    public int size() {
        return Integer.MAX_VALUE - exclusions.size();
    }

    public boolean isEmpty() {
        return size() != 0;
    }

    public boolean contains(int value) {
        return !exclusions.contains(value);
    }

    public boolean remove(int value) {
        boolean b = contains(value);
        if (b) {
            exclusions.add(value);
        }
        return b;
    }

    public boolean add(int value) {
        boolean b = contains(value);
        if (!b) {
            exclusions.remove(value);
        }
        return b;
    }

    public IntIterator iterator() {
        throw new UnsupportedOperationException("Cannot enumerate an infinite set");
    }

    public IntSet union(IntSet other) {
        return new IntComplementSet(exclusions.except(other));
    }

    public IntSet intersect(IntSet other) {
        if (other.isEmpty()) {
            return IntEmptySet.getInstance();
        } else if (other == IntUniversalSet.getInstance()) {
            return copy();
        } else if (other instanceof IntComplementSet) {
            return new IntComplementSet(exclusions.union(((IntComplementSet)other).exclusions));
        } else {
            return other.intersect(this);
        }
    }

    public IntSet except(IntSet other) {
        return new IntComplementSet(exclusions.union(other));
    }

    public boolean containsAll(/*@NotNull*/ IntSet other) {
        if (other.size() > 1) {
            return false;
        }
        IntIterator ii = other.iterator();
        while (ii.hasNext()) {
            if (exclusions.contains(ii.next())) {
                return false;
            }
        }
        return true;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.



