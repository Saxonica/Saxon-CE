package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.NoDynamicContextException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.UntypedAtomicValue;

/**
 * An AtomicComparer used for comparing atomic values of arbitrary item types. It encapsulates
 * a collator that is used when the values to be compared are strings. It also supports
 * a separate method for testing equality of items, which can be used for data types that
 * are not ordered.
 *
 * The AtomicSortComparer is identical to the GenericAtomicComparer except for its handling
 * of NaN: it treats NaN values as lower than any other value, and as equal to each other.
 *
 * @author Michael H. Kay
 *
 */

public class AtomicSortComparer implements AtomicComparer {

    private StringCollator collator;
    private int implicitTimezone;

    /**
     * Factory method to get an atomic comparer suitable for sorting or for grouping (operations in which
     * NaN is considered equal to NaN)
     * @param collator Collating comparer to be used when comparing strings. This argument may be null
     * if the itemType excludes the possibility of comparing strings. If the method is called at compile
     * time, this should be a NamedCollation so that it can be cloned at run-time.
     * @param itemType the primitive item type of the values to be compared
     * @param implicitTimezone from the Dynamic context
     * @return a suitable AtomicComparer
     */

    public static AtomicComparer makeSortComparer(StringCollator collator, AtomicType itemType, int implicitTimezone) {
        if (itemType == AtomicType.STRING ||
                itemType == AtomicType.UNTYPED_ATOMIC ||
                itemType == AtomicType.ANY_URI) {
            if (collator instanceof CodepointCollator) {
                return CodepointCollatingComparer.getInstance();
            } else {
                return new CollatingAtomicComparer(collator);
            }
        } else if (itemType == AtomicType.INTEGER || itemType == AtomicType.DECIMAL ||
                    itemType == AtomicType.DOUBLE || itemType == AtomicType.FLOAT ||
                    itemType == AtomicType.NUMERIC) {
                return ComparableAtomicValueComparer.getInstance();
        } else if (itemType == AtomicType.DATE_TIME || itemType == AtomicType.DATE || itemType == AtomicType.TIME) {
                return new CalendarValueComparer(implicitTimezone);
        } else {
            // use the general-purpose comparer that handles all types
            return new AtomicSortComparer(collator, itemType, implicitTimezone);
        }

    }

    protected AtomicSortComparer(StringCollator collator, AtomicType itemType, int implicitTimezone) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
        this.implicitTimezone = implicitTimezone;
    }

    public StringCollator getCollator() {
        return collator;
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should normally be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) throws NoDynamicContextException {

        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (b == null) {
            return +1;
        }

        // System.err.println("Comparing " + a.getClass() + "(" + a + ") with " + b.getClass() + "(" + b + ") using " + collator);

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator);
        } else if (b instanceof UntypedAtomicValue) {
            return -((UntypedAtomicValue)b).compareTo(a, collator);
        } else if (a.isNaN()) {
            return (b.isNaN() ? 0 : -1);
        } else if (b.isNaN()) {
            return +1;
        } else if (a instanceof StringValue && b instanceof StringValue) {
            if (collator instanceof CodepointCollator) {
                return ((CodepointCollator)collator).compareCS(a.getStringValue(), b.getStringValue());
            } else {
                return collator.compareStrings(a.getStringValue(), b.getStringValue());
            }
        } else {
            Comparable ac = (Comparable)a.getXPathComparable(true, collator, implicitTimezone);
            Comparable bc = (Comparable)b.getXPathComparable(true, collator, implicitTimezone);
            if (ac == null || bc == null) {
                throw new ClassCastException("Values are not comparable (" +
                                Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ')');
            } else {
                return ac.compareTo(bc);
            }
        }
    }

    /**
     * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
     * values are compared by converting to the type of the other operand.
     *
     * @param a the first object to be compared. It is intended that this should be an instance
     *          of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
     *          collator is used to compare the values, otherwise the value must implement the equals() method.
     * @param b the second object to be compared. This must be comparable with the first object: for
     *          example, if one is a string, they must both be strings.
     * @return true if the values are equal, false if not
     * @throws ClassCastException if the objects are not comparable
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) throws NoDynamicContextException {
        return compareAtomicValues(a, b) == 0;
    }

    protected static StructuredQName COLLATION_KEY_NaN =
            new StructuredQName("saxon", "http://saxon.sf.net/collation-key", "NaN");
        // The logic here is to choose a value that compares equal to itself but not equal to any other
        // number. We use StructuredQName because it has a simple equals() method.

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.