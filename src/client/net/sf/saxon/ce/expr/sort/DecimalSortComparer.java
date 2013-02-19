package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
 * An AtomicComparer used for sorting values that are known to be instances of xs:decimal (including xs:integer),
 * It also supports a separate method for getting a collation key to test equality of items
 *
 * @author Michael H. Kay
 *
 */

public class DecimalSortComparer extends ComparableAtomicValueComparer {

    private static DecimalSortComparer THE_INSTANCE = new DecimalSortComparer();

    public static DecimalSortComparer getDecimalSortComparerInstance() {
        return THE_INSTANCE;
    }

    private DecimalSortComparer() {}

    /**
     * Get a comparison key for an object. This must satisfy the rule that if two objects are equal as defined
     * by the XPath eq operator, then their comparison keys are equal as defined by the Java equals() method,
     * and vice versa. There is no requirement that the comparison keys should reflect the ordering of the
     * underlying objects.
    */

    public ComparisonKey getComparisonKey(AtomicValue a) {
        return new ComparisonKey(StandardNames.XS_NUMERIC, a);
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.