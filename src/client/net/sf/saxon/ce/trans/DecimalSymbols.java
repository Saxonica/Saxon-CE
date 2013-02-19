package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.om.StandardNames;

import java.util.Arrays;
import java.util.HashMap;

/**
 * This class is modelled on Java's DecimalFormatSymbols, but it allows the use of any
 * Unicode character to represent symbols such as the decimal point and the grouping
 * separator, whereas DecimalFormatSymbols restricts these to a char (1-65535). Since
 * this is essentially a data structure with no behaviour, we don't bother with getter
 * and setter methods but just expose the fields
 */
public class DecimalSymbols  {

    public int decimalSeparator = '.';
    public int groupingSeparator = ',';
    public int digit ='#';
    public int minusSign = '-';
    public int percent = '%';
    public int permill = '\u2030';
    public int zeroDigit = '0';
    public int patternSeparator = ';';
    public String infinity = "Infinity";
    public String NaN = "NaN";

    /**
     * Check that no character is used in more than one role
     * @throws XPathException
     */

    public void checkDistinctRoles() throws XPathException {
        HashMap<Integer, String> map = new HashMap(20);
        map.put(decimalSeparator, StandardNames.DECIMAL_SEPARATOR);

        if (map.get(groupingSeparator) != null) {
            duplicate(StandardNames.GROUPING_SEPARATOR, map.get(groupingSeparator));
        }
        map.put(groupingSeparator, StandardNames.GROUPING_SEPARATOR);

        if (map.get(percent) != null) {
            duplicate(StandardNames.PERCENT, map.get(percent));
        }
        map.put(percent, StandardNames.PERCENT);

        if (map.get(permill) != null) {
            duplicate(StandardNames.PER_MILLE, map.get(permill));
        }
        map.put(permill, StandardNames.PER_MILLE);

        if (map.get(zeroDigit) != null) {
            duplicate(StandardNames.ZERO_DIGIT, map.get(zeroDigit));
        }
        map.put(zeroDigit, StandardNames.ZERO_DIGIT);

        if (map.get(digit) != null) {
            duplicate(StandardNames.DIGIT, map.get(digit));
        }
        map.put(digit, StandardNames.DIGIT);

        if (map.get(patternSeparator) != null) {
            duplicate(StandardNames.PATTERN_SEPARATOR, map.get(patternSeparator));
        }
        //map.put(patternSeparator, StandardNames.PATTERN_SEPARATOR);
    }

    /**
     * Report that a character is used in more than one role
     * @param role1  the first role
     * @param role2  the second role
     * @throws XPathException (always)
     */

    private void duplicate(String role1, String role2) throws XPathException {
        throw new XPathException("The same character is used as the " + role1 +
                " and as the " + role2);
    }

    /**
     * Check that the character declared as a zero-digit is indeed a valid zero-digit
     * @return false if it is not a valid zero-digit
     */

    public boolean isValidZeroDigit() throws XPathException {
        return (Arrays.binarySearch(zeroDigits, zeroDigit) >= 0);
    }

    static int[] zeroDigits = {0x0030, 0x0660, 0x06f0, 0x0966, 0x09e6, 0x0a66, 0x0ae6, 0x0b66, 0x0be6, 0x0c66,
                               0x0ce6, 0x0d66, 0x0e50, 0x0ed0, 0x0f20, 0x1040, 0x17e0, 0x1810, 0x1946, 0x19d0,
                               0xff10, 0x104a0, 0x1d7ce, 0x1d7d8, 0x1d7e2, 0x1d7ec, 0x1d7f6};

    /**
     * Test if two sets of decimal format symbols are the same
     * @param obj the other set of symbols
     * @return true if the same characters/strings are assigned to each role in both sets of symbols
     */

    public boolean equals(Object obj) {
        if (!(obj instanceof DecimalSymbols)) {
            return false;
        }
        DecimalSymbols o = (DecimalSymbols)obj;
        return decimalSeparator == o.decimalSeparator &&
                groupingSeparator == o.groupingSeparator &&
                digit == o.digit &&
                minusSign == o.minusSign &&
                percent == o.percent &&
                permill == o.permill &&
                zeroDigit == o.zeroDigit &&
                patternSeparator == o.patternSeparator &&
                infinity.equals(o.infinity) &&
                NaN.equals(o.NaN);
    }

    public int hashCode() {
        return decimalSeparator + (37*groupingSeparator) + (41*digit);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

