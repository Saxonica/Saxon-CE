package client.net.sf.saxon.ce.expr.z;

/**
 * An implementation of IntPredicate that tests whether a given integer is a member
 * of some IntSet
 */
public class IntSetPredicate implements IntPredicate {

    private IntSet set;

    public IntSetPredicate(IntSet set) {
        if (set==null) {
            throw new NullPointerException();
        }
        this.set = set;
    }

    /**
     * Ask whether a given value matches this predicate
     *
     * @param value the value to be tested
     * @return true if the value matches; false if it does not
     */
    public boolean matches(int value) {
        return set.contains(value);
    }

    /**
     * Get the underlying IntSet
     * @return the underlying IntSet
     */

    public IntSet getIntSet() {
        return set;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
