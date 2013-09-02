package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.NoDynamicContextException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

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
    private transient XPathContext context;
    private BuiltInAtomicType itemType;

    /**
     * Factory method to get an atomic comparer suitable for sorting or for grouping (operations in which
     * NaN is considered equal to NaN)
     * @param collator Collating comparer to be used when comparing strings. This argument may be null
     * if the itemType excludes the possibility of comparing strings. If the method is called at compile
     * time, this should be a NamedCollation so that it can be cloned at run-time.
     * @param itemType the primitive item type of the values to be compared
     * @param context Dynamic context (may be an EarlyEvaluationContext)
     * @return a suitable AtomicComparer
     */

    public static AtomicComparer makeSortComparer(StringCollator collator, BuiltInAtomicType itemType, XPathContext context) {
        if (itemType == BuiltInAtomicType.STRING ||
                itemType == BuiltInAtomicType.UNTYPED_ATOMIC ||
                itemType == BuiltInAtomicType.ANY_URI) {
            if (collator instanceof CodepointCollator) {
                return CodepointCollatingComparer.getInstance();
            } else {
                return new CollatingAtomicComparer(collator);
            }
        } else if (itemType == BuiltInAtomicType.INTEGER || itemType == BuiltInAtomicType.DECIMAL) {
                return DecimalSortComparer.getDecimalSortComparerInstance();
        } else if (itemType == BuiltInAtomicType.DOUBLE || itemType == BuiltInAtomicType.FLOAT || itemType == BuiltInAtomicType.NUMERIC) {
                return DoubleSortComparer.getInstance();
        } else if (itemType == BuiltInAtomicType.DATE_TIME || itemType == BuiltInAtomicType.DATE || itemType == BuiltInAtomicType.TIME) {
                return new CalendarValueComparer(context);
        } else {
            // use the general-purpose comparer that handles all types
            return new AtomicSortComparer(collator, itemType, context);
        }

    }

    protected AtomicSortComparer(StringCollator collator, BuiltInAtomicType itemType, XPathContext context) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
        this.context = context;
        this.itemType = itemType;
    }

    public StringCollator getCollator() {
        return collator;
    }

    /**
     * Supply the dynamic context in case this is needed for the comparison
     *
     * @param context the dynamic evaluation context
     * @return either the original AtomicComparer, or a new AtomicComparer in which the context
     *         is known. The original AtomicComparer is not modified
     */

    public AtomicComparer provideContext(XPathContext context) {
        return new AtomicSortComparer(collator, itemType, context);
    }

    /**
     * Get the underlying StringCollator
     * @return the underlying collator
     */

    public StringCollator getStringCollator() {
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
            return ((UntypedAtomicValue)a).compareTo(b, collator, context);
        } else if (b instanceof UntypedAtomicValue) {
            return -((UntypedAtomicValue)b).compareTo(a, collator, context);
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
            Comparable ac = (Comparable)a.getXPathComparable(true, collator, context);
            Comparable bc = (Comparable)b.getXPathComparable(true, collator, context);
            if (ac == null || bc == null) {
                return compareNonComparables(a, b);
            } else {
                return ac.compareTo(bc);
            }
        }
    }

    /**
     * Compare two values that are known to be non-comparable. In the base class this method
     * throws a ClassCastException. In a subclass it is overridden to return
     * {@link AtomicValue#INDETERMINATE_ORDERING}
     */

    protected int compareNonComparables(AtomicValue a, AtomicValue b) {
        throw new ClassCastException("Values are not comparable (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ')');
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