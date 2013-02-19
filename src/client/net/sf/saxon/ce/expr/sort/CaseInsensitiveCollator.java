package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.lib.StringCollator;


/**
 * A collating sequence that uses locale-dependent sorting according to the user's current locale
 */

public class CaseInsensitiveCollator implements StringCollator {

    private static CaseInsensitiveCollator theInstance = new CaseInsensitiveCollator();

    public static CaseInsensitiveCollator getInstance() {
        return theInstance;
    }

    /**
    * Compare two string objects.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are of the wrong type for this Comparer
    */

    public int compareStrings(String a, String b) {
        return String.CASE_INSENSITIVE_ORDER.compare(a, b);
    }

    /**
     * Test whether one string is equal to another, according to the rules
     * of the XPath compare() function. The result is true if and only if the
     * compare() method returns zero: but the implementation may be more efficient
     * than calling compare and testing the result for zero
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return true iff s1 equals s2
     */

    public boolean comparesEqual(String s1, String s2) {
        return s1.equalsIgnoreCase(s2);
    }

    /**
     * Get a collation key for two Strings. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     */

    public Object getCollationKey(String s) {
        return s.toLowerCase();
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.