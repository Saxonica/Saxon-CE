package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.value.Whitespace;

/**
 * This class converts a string to an xs:double according to the rules in XML Schema 1.0
 */
public abstract class StringToDouble {

    /**
     * Convert a string to a double.
     * @param s the String to be converted
     * @return a double representing the value of the String
     * @throws NumberFormatException if the value cannot be converted
    */

    public static double stringToNumber(CharSequence s) throws NumberFormatException {
        // first try to parse simple numbers by hand (it's cheaper)
        boolean containsDisallowedChars = false;
        boolean containsWhitespace = false;
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case ' ':
                case '\n':
                case '\t':
                case '\r':
                    containsWhitespace = true;
                    break;
                case 'x':
                case 'X':
                case 'f':
                case 'F':
                case 'd':
                case 'D':
                case 'n':
                case 'N':
                    containsDisallowedChars = true;
                    break;
                default:
                    break;
            }
        }
        String n = (containsWhitespace ? Whitespace.trimWhitespace(s).toString() : s.toString());
        if ("INF".equals(n)) {
            return Double.POSITIVE_INFINITY;
        } else if ("-INF".equals(n)) {
            return Double.NEGATIVE_INFINITY;
        } else if ("NaN".equals(n)) {
            return Double.NaN;
        } else if (containsDisallowedChars) {
            // reject strings containing characters such as (x, f, d) allowed in Java but not in XPath,
            // and other representations of NaN and Infinity such as 'Infinity'
            throw new NumberFormatException("invalid floating point value: " + s);
        } else {
            return Double.parseDouble(n);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.



