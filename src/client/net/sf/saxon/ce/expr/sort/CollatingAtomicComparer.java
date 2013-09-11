package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
 * An AtomicComparer used for comparing strings, untypedAtomic values, and URIs using a collation.
 * A CollatingAtomicComparer is used when it is known in advance that both operands will be of these
 * types. This enables all conversions and promotions to be bypassed: the string values of both operands
 * are simply extracted and passed to the collator for comparison.
 *
 * @author Michael H. Kay
 *
 */

public class CollatingAtomicComparer implements AtomicComparer {

    private StringCollator collator;

    /**
     * Create an GenericAtomicComparer
     * @param collator the collation to be used. If the method is called at compile time, this should
     * be a NamedCollation so that it can be cloned at run-time.
     */

    public CollatingAtomicComparer(StringCollator collator) {

        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        } else {
            this.collator = collator;
        }
    }


    public StringCollator getCollator() {
        return collator;
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should be an instance
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

        return collator.compareStrings(a.getStringValue(), b.getStringValue());
    }

    /**
    * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
    * values are compared by converting to the type of the other operand.
    * @param a the first object to be compared. It is intended that this should be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the equals() method.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        return compareAtomicValues(a, b) == 0;
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.