package client.net.sf.saxon.ce.expr.z;

/**
 * An IntPredicate that matches a single specific integer
 */

public class IntValuePredicate implements IntPredicate {

    private int target;

    public IntValuePredicate(int target) {
        this.target = target;
    }

    /**
     * Ask whether a given value matches this predicate
     *
     * @param value the value to be tested
     * @return true if the value matches; false if it does not
     */
    public boolean matches(int value) {
        return value == target;
    }

    /**
     * Get the value matched by this predicate
     * @return the value that this predicate matches
     */

    public int getTarget() {
        return target;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
