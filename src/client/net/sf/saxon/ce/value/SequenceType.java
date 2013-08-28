package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;

import java.util.HashMap;
import java.util.Map;

/**
 * SequenceType: a sequence type consists of a primary type, which indicates the type of item,
 * and a cardinality, which indicates the number of occurrences permitted. Where the primary type
 * is element or attribute, there may also be a content type, indicating the required type
 * annotation on the element or attribute content.
 */

public final class SequenceType {

    private ItemType primaryType;    // the primary type of the item, e.g. "element", "comment", or "integer"
    private int cardinality;    // the required cardinality

    private static Map pool = new HashMap(50);

    /**
     * A type that allows any sequence of items
     */

    public static final SequenceType ANY_SEQUENCE =
            makeSequenceType(AnyItemType.getInstance(), StaticProperty.ALLOWS_ZERO_OR_MORE);

    /**
     * A type that allows exactly one item, of any kind
     */

    public static final SequenceType SINGLE_ITEM =
            makeSequenceType(AnyItemType.getInstance(), StaticProperty.EXACTLY_ONE);

    /**
     * A type that allows zero or one items, of any kind
     */

    public static final SequenceType OPTIONAL_ITEM =
            makeSequenceType(AnyItemType.getInstance(), StaticProperty.ALLOWS_ZERO_OR_ONE);

    /**
     * A type that allows exactly one atomic value
     */

    public static final SequenceType SINGLE_ATOMIC =
            makeSequenceType(BuiltInAtomicType.ANY_ATOMIC,
                    StaticProperty.EXACTLY_ONE);

    /**
     * A type that allows zero or one atomic values
     */

    public static final SequenceType OPTIONAL_ATOMIC =
            makeSequenceType(BuiltInAtomicType.ANY_ATOMIC,
                    StaticProperty.ALLOWS_ZERO_OR_ONE);
    /**
     * A type that allows zero or more atomic values
     */

    public static final SequenceType ATOMIC_SEQUENCE =
            makeSequenceType(BuiltInAtomicType.ANY_ATOMIC,
                    StaticProperty.ALLOWS_ZERO_OR_MORE);

    /**
     * A type that allows a single string
     */

    public static final SequenceType SINGLE_STRING =
            makeSequenceType(BuiltInAtomicType.STRING, StaticProperty.EXACTLY_ONE);

    public static final SequenceType OPTIONAL_STRING =
            makeSequenceType(BuiltInAtomicType.STRING, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType STRING_SEQUENCE =
            makeSequenceType(BuiltInAtomicType.STRING, StaticProperty.ALLOWS_ZERO_OR_MORE);


    /**
     * A type that allows a single untyped atomic
     */

    public static final SequenceType SINGLE_UNTYPED_ATOMIC =
            makeSequenceType(BuiltInAtomicType.UNTYPED_ATOMIC, StaticProperty.EXACTLY_ONE);


    /**
     * A type that allows a single optional integer
     */

    public static final SequenceType OPTIONAL_INTEGER =
            makeSequenceType(BuiltInAtomicType.INTEGER, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_INTEGER =
            makeSequenceType(BuiltInAtomicType.INTEGER, StaticProperty.EXACTLY_ONE);

    public static final SequenceType INTEGER_SEQUENCE =
            makeSequenceType(BuiltInAtomicType.INTEGER, StaticProperty.ALLOWS_ZERO_OR_MORE);


    /**
     * A type that allows an optional numeric value
     */

    public static final SequenceType OPTIONAL_NUMERIC =
            makeSequenceType(BuiltInAtomicType.NUMERIC,
                    StaticProperty.ALLOWS_ZERO_OR_ONE);

    /**
     * A type that allows zero or one nodes
     */

    public static final SequenceType OPTIONAL_NODE =
            makeSequenceType(AnyNodeTest.getInstance(), StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_NODE =
            makeSequenceType(AnyNodeTest.getInstance(), StaticProperty.EXACTLY_ONE);

    public static final SequenceType NODE_SEQUENCE =
            makeSequenceType(AnyNodeTest.getInstance(), StaticProperty.ALLOWS_ZERO_OR_MORE);


    public static final SequenceType OPTIONAL_DOCUMENT =
            makeSequenceType(NodeKindTest.DOCUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_ELEMENT =
            makeSequenceType(NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE);

    public static final SequenceType ELEMENT_SEQUENCE =
            makeSequenceType(NodeKindTest.ELEMENT, StaticProperty.ALLOWS_ZERO_OR_MORE);

    /**
     * A type that allows a sequence of zero or more numeric values
     */

    public static final SequenceType NUMERIC_SEQUENCE =
            makeSequenceType(BuiltInAtomicType.NUMERIC, StaticProperty.ALLOWS_ZERO_OR_MORE);

    public static final SequenceType OPTIONAL_BOOLEAN =
            makeSequenceType(BuiltInAtomicType.BOOLEAN, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_BOOLEAN =
            makeSequenceType(BuiltInAtomicType.BOOLEAN, StaticProperty.EXACTLY_ONE);

    public static final SequenceType OPTIONAL_DATE =
            makeSequenceType(BuiltInAtomicType.DATE, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_DATE =
            makeSequenceType(BuiltInAtomicType.DATE, StaticProperty.EXACTLY_ONE);

    public static final SequenceType OPTIONAL_DATE_TIME =
            makeSequenceType(BuiltInAtomicType.DATE_TIME, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_DATE_TIME =
            makeSequenceType(BuiltInAtomicType.DATE_TIME, StaticProperty.EXACTLY_ONE);

    public static final SequenceType OPTIONAL_TIME =
            makeSequenceType(BuiltInAtomicType.TIME, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_TIME =
            makeSequenceType(BuiltInAtomicType.TIME, StaticProperty.EXACTLY_ONE);

    public static final SequenceType OPTIONAL_DURATION =
            makeSequenceType(BuiltInAtomicType.DURATION, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType OPTIONAL_DAY_TIME_DURATION =
            makeSequenceType(BuiltInAtomicType.DAY_TIME_DURATION, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_DAY_TIME_DURATION =
            makeSequenceType(BuiltInAtomicType.DAY_TIME_DURATION, StaticProperty.EXACTLY_ONE);

    public static final SequenceType OPTIONAL_ANY_URI =
            makeSequenceType(BuiltInAtomicType.ANY_URI, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_ANY_URI =
            makeSequenceType(BuiltInAtomicType.ANY_URI, StaticProperty.EXACTLY_ONE);

    public static final SequenceType OPTIONAL_QNAME =
            makeSequenceType(BuiltInAtomicType.QNAME, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType OPTIONAL_DECIMAL =
            makeSequenceType(BuiltInAtomicType.DECIMAL, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_QNAME =
            makeSequenceType(BuiltInAtomicType.QNAME, StaticProperty.EXACTLY_ONE);

    public static final SequenceType SINGLE_DOUBLE =
            makeSequenceType(BuiltInAtomicType.DOUBLE, StaticProperty.EXACTLY_ONE);


    /**
     * A type that only permits the empty sequence
     */

    public static final SequenceType EMPTY_SEQUENCE =
            makeSequenceType(EmptySequenceTest.getInstance(), StaticProperty.EMPTY);

    /**
     * Construct an instance of SequenceType. This is a private constructor: all external
     * clients use the factory method makeSequenceType(), to allow object pooling.
     *
     * @param primaryType The item type
     * @param cardinality The required cardinality
     */
    private SequenceType(ItemType primaryType, int cardinality) {
        this.primaryType = primaryType;
        if (primaryType instanceof EmptySequenceTest) {
            this.cardinality = StaticProperty.EMPTY;
        } else {
            this.cardinality = cardinality;
        }
    }

    /**
     * Construct an instance of SequenceType. This is a factory method: it maintains a
     * pool of SequenceType objects to reduce the amount of object creation.
     *
     * @param primaryType The item type
     * @param cardinality The required cardinality
     * @return the corresponding sequence type
     */

    public static SequenceType makeSequenceType(ItemType primaryType, int cardinality) {

        if (!(primaryType instanceof BuiltInAtomicType)) {
            return new SequenceType(primaryType, cardinality);
        }

        // For each ItemType, there is an array of 8 SequenceTypes, one for each possible
        // cardinality (including impossible cardinalities, such as "0 or many"). The pool
        // is a static HashMap that obtains this array, given an ItemType. The array contains null
        // entries for cardinalities that have not been requested.

        SequenceType[] array = (SequenceType[]) pool.get(primaryType);
        if (array == null) {
            array = new SequenceType[8];
            pool.put(primaryType, array);
        }
        int code = StaticProperty.getCardinalityCode(cardinality);
        if (array[code] == null) {
            SequenceType s = new SequenceType(primaryType, cardinality);
            array[code] = s;
            return s;
        } else {
            return array[code];
        }
    }

    /**
     * Get the "primary" part of this required type. E.g. for type element(*, xs:date) the "primary type" is element()
     *
     * @return The item type code of the primary type
     */
    public ItemType getPrimaryType() {
        return primaryType;
    }

    /**
     * Get the cardinality component of this SequenceType. This is one of the constants Cardinality.EXACTLY_ONE,
     * Cardinality.ONE_OR_MORE, etc
     *
     * @return the required cardinality
     * @see client.net.sf.saxon.ce.value.Cardinality
     */
    public int getCardinality() {
        return cardinality;
    }

    /**
     * Determine whether a given value is a valid instance of this SequenceType
     *
     * @param value the value to be tested
     * @return true if the value is a valid instance of this type
     */

    public boolean matches(Value value, Configuration config) throws XPathException {
        int length = value.getLength();
        if (length > 1 && !Cardinality.allowsMany(cardinality)) {
            return false;
        }
        if (length == 0 && !Cardinality.allowsZero(cardinality)) {
            return false;
        }
        SequenceIterator iter = value.iterate();
        while (true) {
            Item item = iter.next();
            if (item == null) {
                return true;
            }
            if (!primaryType.matchesItem(item, false, config)) {
                return false;
            }
        }
    }

    /**
     * Return a string representation of this SequenceType
     *
     * @return the string representation as an instance of the XPath
     *         SequenceType construct
     */
    public String toString() {
        String s = primaryType.toString();
        if (cardinality == StaticProperty.ALLOWS_ONE_OR_MORE) {
            s = s + '+';
        } else if (cardinality == StaticProperty.ALLOWS_ZERO_OR_MORE) {
            s = s + '*';
        } else if (cardinality == StaticProperty.ALLOWS_ZERO_OR_ONE) {
            s = s + '?';
        }
        return s;
    }

    /**
     * Returns a hash code value for the object.
     */
    public int hashCode() {
        return primaryType.hashCode() ^ cardinality;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof SequenceType &&
                this.primaryType.equals(((SequenceType) obj).primaryType) &&
                this.cardinality == ((SequenceType) obj).cardinality;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.