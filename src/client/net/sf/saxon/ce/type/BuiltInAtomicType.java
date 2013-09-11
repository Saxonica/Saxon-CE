package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.value.*;

/**
 * This class represents a built-in atomic type, which may be either a primitive type
 * (such as xs:decimal or xs:anyURI) or a derived type (such as xs:ID or xs:dayTimeDuration).
 */

public class BuiltInAtomicType extends BuiltInType implements SchemaType, ItemType {

    String localName;
    SchemaType baseType;
    boolean ordered = false;

    public static BuiltInAtomicType ANY_ATOMIC = makeAtomicType("anyAtomicType", AnySimpleType.getInstance(), true);

    public static BuiltInAtomicType NUMERIC = makeAtomicType("numeric", ANY_ATOMIC, true);

    public static BuiltInAtomicType STRING = makeAtomicType("string", ANY_ATOMIC, true);

    public static BuiltInAtomicType BOOLEAN = makeAtomicType("boolean", ANY_ATOMIC, true);

    public static BuiltInAtomicType DURATION = makeAtomicType("duration", ANY_ATOMIC, false);

    public static BuiltInAtomicType DATE_TIME = makeAtomicType("dateTime", ANY_ATOMIC, true);

    public static BuiltInAtomicType DATE = makeAtomicType("date", ANY_ATOMIC, true);

    public static BuiltInAtomicType TIME = makeAtomicType("time", ANY_ATOMIC, true);

    public static BuiltInAtomicType G_YEAR_MONTH = makeAtomicType("gYearMonth", ANY_ATOMIC, false);

    public static BuiltInAtomicType G_MONTH = makeAtomicType("gMonth", ANY_ATOMIC, false);

    public static BuiltInAtomicType G_MONTH_DAY = makeAtomicType("gMonthDay", ANY_ATOMIC, false);

    public static BuiltInAtomicType G_YEAR = makeAtomicType("gYear", ANY_ATOMIC, false);

    public static BuiltInAtomicType G_DAY = makeAtomicType("gDay", ANY_ATOMIC, false);

    public static BuiltInAtomicType HEX_BINARY = makeAtomicType("hexBinary", ANY_ATOMIC, false);

    public static BuiltInAtomicType BASE64_BINARY = makeAtomicType("base64Binary", ANY_ATOMIC, false);

    public static BuiltInAtomicType ANY_URI = makeAtomicType("anyURI", ANY_ATOMIC, true);

    public static BuiltInAtomicType QNAME = makeAtomicType("QName", ANY_ATOMIC, false);

    public static BuiltInAtomicType UNTYPED_ATOMIC = makeAtomicType("untypedAtomic", ANY_ATOMIC, true);

    public static BuiltInAtomicType DECIMAL = makeAtomicType("decimal", NUMERIC, true);

    public static BuiltInAtomicType FLOAT = makeAtomicType("float", NUMERIC, true);

    public static BuiltInAtomicType DOUBLE = makeAtomicType("double", NUMERIC, true);

    public static BuiltInAtomicType INTEGER = makeAtomicType("integer", DECIMAL, true);

    public static BuiltInAtomicType YEAR_MONTH_DURATION = makeAtomicType("yearMonthDuration", DURATION, true);

    public static BuiltInAtomicType DAY_TIME_DURATION = makeAtomicType("dayTimeDuration", DURATION, true);


    static {
        
    }

    private BuiltInAtomicType(String localName) {
        this.localName = localName;
    }


    /**
     * Determine whether the atomic type is ordered, that is, whether less-than and greater-than comparisons
     * are permitted
     *
     * @return true if ordering operations are permitted
     */

    public boolean isOrdered() {
        return ordered;
    }


    /**
     * Determine whether the atomic type is numeric
     *
     * @return true if the type is a built-in numeric type
     */

    public boolean isPrimitiveNumeric() {
        return this == NUMERIC || this == INTEGER || baseType == NUMERIC;
    }


    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return "xs:" + localName;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    public final SchemaType getBaseType() {
        return baseType;
    }

    /**
     * Test whether a given item conforms to this type
     *
     * @param item              The item to be tested
     * @param allowURIPromotion true if we regard a URI as effectively a subtype of String
     * @param config            the Saxon configuration (used to locate the type hierarchy cache)
     * @return true if the item is an instance of this type; false otherwise
     */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        if (item instanceof AtomicValue) {
            ItemType type = ((AtomicValue)item).getItemType();
            if (type == this) {
                return true;
            }
            final TypeHierarchy th = TypeHierarchy.getInstance();
            if (th.isSubType(type, this)) {
                return true;
            }
            if (allowURIPromotion && this == STRING && th.isSubType(type, BuiltInAtomicType.ANY_URI)) {
                // allow promotion from anyURI to string
                return true;
            }
        }
        return false;
    }

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     *
     * @param th the type hierarchy cache, not used in this implementation
     * @return the supertype, or null if this type is item()
     */

    public ItemType getSuperType(TypeHierarchy th) {
        SchemaType base = getBaseType();
        if (base instanceof AnySimpleType) {
            return AnyItemType.getInstance();
        } else {
            return (ItemType)base;
        }
    }

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public ItemType getPrimitiveItemType() {
        return this;    // all atomic types in Saxon-CE are primitive
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public BuiltInAtomicType getAtomizedItemType() {
        return this;
    }

    public String toString() {
        return getDisplayName();
    }

    /**
     * Test whether this Simple Type is an atomic type
     *
     * @return true, this is an atomic type
     */

    public boolean isAtomicType() {
        return true;
    }

    /**
     * Internal factory method to create a BuiltInAtomicType. There is one instance for each of the
     * built-in atomic types
     *
     * @param localName The name of the type within the XSD namespace
     * @param baseType    The base type from which this type is derived
     * @param ordered True if an ordering relationship is defined for this type
     * @return the newly constructed built in atomic type
     */
    private static BuiltInAtomicType makeAtomicType(String localName, SchemaType baseType, boolean ordered) {
        BuiltInAtomicType t = new BuiltInAtomicType(localName);
        t.baseType = baseType;
        t.ordered = ordered;
        BuiltInType.register(localName, t);
        return t;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.