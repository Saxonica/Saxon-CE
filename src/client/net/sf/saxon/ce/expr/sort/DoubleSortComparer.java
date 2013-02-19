package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.NumericValue;

/**
 * An AtomicComparer used for sorting values that are known to be numeric.
 * It also supports a separate method for getting a collation key to test equality of items.
 * This comparator treats NaN values as equal to each other, and less than any other value.
 *
 * @author Michael H. Kay
 *
 */

public class DoubleSortComparer implements AtomicComparer {

    private static DoubleSortComparer THE_INSTANCE = new DoubleSortComparer();

    /**
     * Get the singular instance of this class
     * @return the singular instance 
     */
    
    public static DoubleSortComparer getInstance() {
        return THE_INSTANCE;
    }
    
    private DoubleSortComparer() {

    }

    public StringCollator getCollator() {
        return null;
    }

    /**
     * Supply the dynamic context in case this is needed for the comparison
     *
     * @param context the dynamic evaluation context
     * @return either the original AtomicComparer, or a new AtomicComparer in which the context
     *         is known. The original AtomicComparer is not modified
     */

    public AtomicComparer provideContext(XPathContext context) {
        return this;
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. 
    * @param a the first object to be compared. It is intended that this should normally be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) {
        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (b == null) {
            return +1;
        }

        NumericValue an = (NumericValue)a;
        NumericValue bn = (NumericValue)b;

        if (an.isNaN()) {
            return (bn.isNaN() ? 0 : -1);
        } else if (bn.isNaN()) {
            return +1;
        }

        return an.compareTo(bn);
    }

    /**
     * Test whether two values compare equal. Note that for this comparer, NaN is considered equal to itself
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        return compareAtomicValues(a, b) == 0;
    }

    /**
     * Get a comparison key for an object. This must satisfy the rule that if two objects are equal as defined
     * by the XPath eq operator, then their comparison keys are equal as defined by the Java equals() method,
     * and vice versa. There is no requirement that the comparison keys should reflect the ordering of the 
     * underlying objects.
    */

    public ComparisonKey getComparisonKey(AtomicValue a) {
        if (((NumericValue)a).isNaN()) {
            // Deal with NaN specially. For sorting and similar operations, NaN is considered equal to itself
            return new ComparisonKey(StandardNames.XS_NUMERIC, AtomicSortComparer.COLLATION_KEY_NaN);
        } else {
            return new ComparisonKey(StandardNames.XS_NUMERIC, a);
        }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.