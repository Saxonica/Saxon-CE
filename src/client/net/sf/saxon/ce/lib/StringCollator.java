package client.net.sf.saxon.ce.lib;

/**
 * This interface represents a "collation" as defined in XPath, that is, a set of rules for comparing strings
 */
public interface StringCollator {

    /**
     * Compare two strings
     * @param o1 the first string
     * @param o2 the second string
     * @return 0 if the strings are considered equal, a negative integer if the first string is less than the second,
     * a positive integer if the first string is greater than the second
     */

    int compareStrings(String o1, String o2);

    /**
     * Compare two strings for equality. This may be more efficient than using compareStrings and
     * testing whether the result is zero, but it must give the same result
     * @param s1 the first string
     * @param s2 the second string
     * @return true if and only if the strings are considered equal,
     */

    boolean comparesEqual(String s1, String s2);

    /**
     * Get a collation key for a String. The essential property of collation keys
     * is that if (and only if) two strings are equal under the collation, then
     * comparing the collation keys using the equals() method must return true.
     * @param s the string whose collation key is required
     * @return the collation key
     */

    Object getCollationKey(String s);


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
