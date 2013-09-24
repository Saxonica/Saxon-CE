package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;

/**
 * SequenceType: a sequence type consists of a primary type, which indicates the type of item,
 * and a cardinality, which indicates the number of occurrences permitted. Where the primary type
 * is element or attribute, there may also be a content type, indicating the required type
 * annotation on the element or attribute content.
 */

public final class SequenceType {

    private ItemType primaryType;    // the primary type of the item, e.g. "element", "comment", or "integer"
    private int cardinality;    // the required cardinality

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
            makeSequenceType(AtomicType.ANY_ATOMIC,
                    StaticProperty.EXACTLY_ONE);

    /**
     * A type that allows zero or one atomic values
     */

    public static final SequenceType OPTIONAL_ATOMIC =
            makeSequenceType(AtomicType.ANY_ATOMIC,
                    StaticProperty.ALLOWS_ZERO_OR_ONE);
    /**
     * A type that allows zero or more atomic values
     */

    public static final SequenceType ATOMIC_SEQUENCE =
            makeSequenceType(AtomicType.ANY_ATOMIC,
                    StaticProperty.ALLOWS_ZERO_OR_MORE);

    /**
     * A type that allows a single string
     */

    public static final SequenceType SINGLE_STRING =
            makeSequenceType(AtomicType.STRING, StaticProperty.EXACTLY_ONE);

    /**
     * A type that allows a single untyped atomic
     */

    public static final SequenceType SINGLE_UNTYPED_ATOMIC =
            makeSequenceType(AtomicType.UNTYPED_ATOMIC, StaticProperty.EXACTLY_ONE);


    /**
     * A type that allows a single optional integer
     */

    public static final SequenceType OPTIONAL_INTEGER =
            makeSequenceType(AtomicType.INTEGER, StaticProperty.ALLOWS_ZERO_OR_ONE);

    public static final SequenceType SINGLE_INTEGER =
            makeSequenceType(AtomicType.INTEGER, StaticProperty.EXACTLY_ONE);

    /**
     * A type that allows an optional numeric value
     */

    public static final SequenceType OPTIONAL_NUMERIC =
            makeSequenceType(AtomicType.NUMERIC,
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
     * Construct an instance of SequenceType.
     *
     * @param primaryType The item type
     * @param cardinality The required cardinality
     * @return the corresponding sequence type
     */

    public static SequenceType makeSequenceType(ItemType primaryType, int cardinality) {
        return new SequenceType(primaryType, cardinality);
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