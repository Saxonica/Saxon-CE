package client.net.sf.saxon.ce.expr.z;

/**
 * Abstract superclass containing helper methods for various implementations of IntSet
 */
public abstract class AbstractIntSet implements IntSet {

    /**
     * Test if this set is a superset of another set
     * @param other the other set
     * @return true if every item in the other set is also in this set
     */

    public boolean containsAll(IntSet other) {
        if (other == IntUniversalSet.getInstance() || (other instanceof IntComplementSet)) {
            return false;
        }
        IntIterator it = other.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Form a new set that is the union of two IntSets.
     * @param other the second set
     * @return the union of the two sets
     */

    public IntSet union(IntSet other) {
        if (other == IntUniversalSet.getInstance()) {
            return other;
        }
        if (this.isEmpty()) {
            return other.copy();
        }
        if (other.isEmpty()) {
            return this.copy();
        }
        if (other instanceof IntComplementSet) {
            return other.union(this);
        }
        if (other instanceof IntCheckingSet) {
            return other.union(this);
        }
        IntHashSet n = new IntHashSet(this.size() + other.size());
        IntIterator it = iterator();
        while (it.hasNext()) {
            n.add(it.next());
        }
        it = other.iterator();
        while (it.hasNext()) {
            n.add(it.next());
        }
        return n;
    }

    /**
     * Form a new set that is the intersection of two IntSets.
     * @param other the second set
     * @return the intersection of the two sets
     */

    public IntSet intersect(IntSet other) {
        if (this.isEmpty() || other.isEmpty()) {
            return new IntHashSet(); // return empty set
        }
        IntHashSet n = new IntHashSet(size());
        IntIterator it = iterator();
        while (it.hasNext()) {
            int v = it.next();
            if (other.contains(v)) {
                n.add(v);
            }
        }
        return n;
    }

    /**
     * Form a new set that is the difference of this set and another set.
     * The result will either be an immutable object, or a newly constructed object.
     * @param other the second set
     * @return the intersection of the two sets
     */


    public IntSet except(IntSet other) {
        IntHashSet n = new IntHashSet(size());
        IntIterator it = iterator();
        while (it.hasNext()) {
            int v = it.next();
            if (!other.contains(v)) {
                n.add(v);
            }
        }
        return n;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
