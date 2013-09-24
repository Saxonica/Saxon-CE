package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.CalendarValue;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * An AtomicComparer used for comparing atomic values of arbitrary item types. It encapsulates
 * a Collator that is used when the values to be compared are strings. It also supports
 * a separate method for testing equality of items, which can be used for data types that
 * are not ordered.
 *
 * @author Michael H. Kay
 *
 */

public class GenericAtomicComparer implements AtomicComparer {

    private StringCollator collator;
    private int implicitTimezone;

    /**
     * Create an GenericAtomicComparer
     * @param collator the collation to be used
     * @param implicitTimezone used when comparing dates/times.
     */

    public GenericAtomicComparer(StringCollator collator, int implicitTimezone) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
        this.implicitTimezone = implicitTimezone;
    }

    /**
     * Factory method to make a GenericAtomicComparer for values of known types
     * @param type0 primitive type of the first operand
     * @param type1 primitive type of the second operand
     * @param collator the collation to be used, if any. This is supplied as a NamedCollation object
     * which encapsulated both the collation URI and the collation itself.
     * @param implicitTimezone from the dynamic context
     * @return a GenericAtomicComparer for values of known types
     */

    public static AtomicComparer makeAtomicComparer(
            AtomicType type0, AtomicType type1, StringCollator collator, int implicitTimezone) {
        if (type0 == type1) {
            if (type0 == AtomicType.DATE_TIME ||
                    type0 == AtomicType.DATE ||
                    type0 == AtomicType.TIME ||
                    type0 == AtomicType.G_DAY ||
                    type0 == AtomicType.G_MONTH ||
                    type0 == AtomicType.G_MONTH_DAY ||
                    type0 == AtomicType.G_YEAR ||
                    type0 == AtomicType.G_YEAR_MONTH) {
                return new CalendarValueComparer(implicitTimezone);
            } else if (type0 == AtomicType.BOOLEAN ||
                    type0 == AtomicType.DAY_TIME_DURATION ||
                    type0 == AtomicType.YEAR_MONTH_DURATION ||
                    type0 == AtomicType.BASE64_BINARY ||
                    type0 == AtomicType.HEX_BINARY ||
                    type0 == AtomicType.QNAME) {
                return ComparableAtomicValueComparer.getInstance();
            }
        }

        if (type0.isPrimitiveNumeric() && type1.isPrimitiveNumeric()) {
            return ComparableAtomicValueComparer.getInstance();
        }

        if ((type0 == AtomicType.STRING ||
                type0 == AtomicType.UNTYPED_ATOMIC ||
                type0 == AtomicType.ANY_URI) &&
            (type1 == AtomicType.STRING ||
                type1 == AtomicType.UNTYPED_ATOMIC ||
                type1 == AtomicType.ANY_URI)) {
            if (collator instanceof CodepointCollator) {
                return CodepointCollatingComparer.getInstance();
            } else {
                return new CollatingAtomicComparer(collator);
            }
        }
        return new GenericAtomicComparer(collator, implicitTimezone);
    }

    public StringCollator getCollator() {
        return collator;
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    *
     * @param a the first object to be compared. It is intended that this should be an instance
     * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
     * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
     * interface.
     * @param b the second object to be compared. This must be comparable with the first object: for
     * example, if one is a string, they must both be strings.
     * @return <0 if a < b, 0 if a = b, >0 if a > b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) {

        // System.err.println("Comparing " + a.getClass() + "(" + a + ") with " + b.getClass() + "(" + b + ") using " + collator);

        if (a == null) {
            return (b == null ? 0 : -1);
        } else if (b == null) {
            return +1;
        }

        if (a instanceof StringValue && b instanceof StringValue) {
            if (collator instanceof CodepointCollator) {
                return ((CodepointCollator)collator).compareCS(a.getStringValue(), b.getStringValue());
            } else {
                return collator.compareStrings(a.getStringValue(), b.getStringValue());
            }
        } else {
            Comparable ac = (Comparable)a.getXPathComparable(true, collator, implicitTimezone);
            Comparable bc = (Comparable)b.getXPathComparable(true, collator, implicitTimezone);
            if (ac == null || bc == null) {
                throw new ClassCastException("Objects are not comparable (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ')');
            } else {
                return ac.compareTo(bc);
            }
        }
    }

    /**
    * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    *
     * @param a the first object to be compared. If it is a StringValue, the
     * collator is used to compare the values, otherwise the value must implement the equals() method.
     * @param b the second object to be compared. This must be comparable with the first object: for
     * example, if one is a string, they must both be strings.
     * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        // System.err.println("Comparing " + a.getClass() + ": " + a + " with " + b.getClass() + ": " + b);
        if (a instanceof StringValue && b instanceof StringValue) {
            return collator.comparesEqual(a.getStringValue(), b.getStringValue());
        } else if (a instanceof CalendarValue && b instanceof CalendarValue) {
            return ((CalendarValue)a).compareTo((CalendarValue)b, implicitTimezone) == 0;
        } else {
            Object ac = a.getXPathComparable(false, collator, implicitTimezone);
            Object bc = b.getXPathComparable(false, collator, implicitTimezone);
            return ac.equals(bc);
        }
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.