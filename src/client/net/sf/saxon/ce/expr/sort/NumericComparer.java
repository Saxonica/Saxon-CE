package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.type.StringToDouble;

/**
 * A Comparer used for comparing sort keys when data-type="number". The items to be
 * compared are converted to numbers, and the numbers are then compared directly. NaN values
 * compare equal to each other, and equal to an empty sequence, but less than anything else.
 * <p/>
 * This class is used in XSLT only, so there is no need to handle XQuery's "empty least" vs
 * "empty greatest" options.
 *
 * @author Michael H. Kay
 *
 */

public class NumericComparer implements AtomicComparer {

    private static NumericComparer THE_INSTANCE = new NumericComparer();

    public static NumericComparer getInstance() {
        return THE_INSTANCE;
    }

    protected NumericComparer() {
    }

    public StringCollator getCollator() {
        return null;
    }

    /**
    * Compare two Items by converting them to numbers and comparing the numeric values. If either
    * value cannot be converted to a number, it is treated as NaN, and compares less that the other
    * (two NaN values compare equal).
    * @param a the first Item to be compared.
    * @param b the second Item to be compared.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not Items
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) {
        double d1, d2;

        if (a instanceof NumericValue) {
            d1 = ((NumericValue)a).getDoubleValue();
        } else if (a == null) {
            d1 = Double.NaN;
        } else {
            try {
                d1 = StringToDouble.stringToNumber(a.getStringValue());
            } catch (NumberFormatException err) {
                d1 = Double.NaN;
            }
        }

        if (b instanceof NumericValue) {
            d2 = ((NumericValue)b).getDoubleValue();
        } else if (b == null) {
            d2 = Double.NaN;
        } else {
            try {
                d2 = StringToDouble.stringToNumber(b.getStringValue());
            } catch (NumberFormatException err) {
                d2 = Double.NaN;
            }
        }

        if (Double.isNaN(d1)) {
            if (Double.isNaN(d2)) {
                return 0;
            } else {
                return -1;
            }
        }
        if (Double.isNaN(d2)) {
            return +1;
        }
        if (d1 < d2) return -1;
        if (d1 > d2) return +1;
        return 0;

    }

    /**
     * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
     * values are compared by converting to the type of the other operand.
     *
     * @param a the first object to be compared.
     * @param b the second object to be compared.
     * @return true if the values are equal, false if not
     * @throws ClassCastException if the objects are not comparable
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        return compareAtomicValues(a, b) == 0;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.