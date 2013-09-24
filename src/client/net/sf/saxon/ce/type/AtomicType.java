package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.value.*;

import java.util.HashMap;

/**
 * This class represents a built-in atomic type, which may be either a primitive type
 * (such as xs:decimal or xs:anyURI) or a derived type (such as xs:ID or xs:dayTimeDuration).
 */

public class AtomicType implements ItemType {

    private static HashMap<String, AtomicType> lookup = new HashMap<String, AtomicType>(20);

    /**
     * Internal factory method to create a BuiltInAtomicType. There is one instance for each of the
     * built-in atomic types
     *
     * @param localName The name of the type within the XSD namespace
     * @param baseType    The base type from which this type is derived
     * @param ordered True if an ordering relationship is defined for this type
     * @return the newly constructed built in atomic type
     */
    private static AtomicType makeAtomicType(String localName, ItemType baseType, boolean ordered) {
        AtomicType t = new AtomicType(localName);
        t.baseType = baseType;
        t.ordered = ordered;
        register(localName, t);
        return t;
    }

    public static boolean isRecognizedName(String localName) {
        return getSchemaType(localName) != null ||
                "anyType".equals(localName) ||
                "untyped".equals(localName) ||
                "anySimpleType".equals(localName);
    }

    /**
     * Get the schema type with a given fingerprint
     * @param localName the local name of the type, in the XSD namespace
     * @return the SchemaType object representing the given type, if known, otherwise null
     */

    public static AtomicType getSchemaType(String localName) {
        AtomicType st = lookup.get(localName);
        if (st == null) {
            // this means the method has been called before doing the static initialization of BuiltInAtomicType
            // or BuiltInListType. So force it now
            if (AtomicType.DOUBLE == null) {
                // no action, except to force the initialization to run
            }
            st = lookup.get(localName);
        }
        return st;
    }

    /**
     * Method for internal use to register a built in type with this class
     * @param localName the type name within the XSD namespace
     * @param type the SchemaType representing the built in type
     */

    static void register(String localName, AtomicType type) {
        lookup.put(localName, type);
    }

    public static AtomicType ANY_ATOMIC = makeAtomicType("anyAtomicType", AnyItemType.getInstance(), true);

    public static AtomicType NUMERIC = makeAtomicType("numeric", ANY_ATOMIC, true);

    public static AtomicType STRING = makeAtomicType("string", ANY_ATOMIC, true);

    public static AtomicType BOOLEAN = makeAtomicType("boolean", ANY_ATOMIC, true);

    public static AtomicType DURATION = makeAtomicType("duration", ANY_ATOMIC, false);

    public static AtomicType DATE_TIME = makeAtomicType("dateTime", ANY_ATOMIC, true);

    public static AtomicType DATE = makeAtomicType("date", ANY_ATOMIC, true);

    public static AtomicType TIME = makeAtomicType("time", ANY_ATOMIC, true);

    public static AtomicType G_YEAR_MONTH = makeAtomicType("gYearMonth", ANY_ATOMIC, false);

    public static AtomicType G_MONTH = makeAtomicType("gMonth", ANY_ATOMIC, false);

    public static AtomicType G_MONTH_DAY = makeAtomicType("gMonthDay", ANY_ATOMIC, false);

    public static AtomicType G_YEAR = makeAtomicType("gYear", ANY_ATOMIC, false);

    public static AtomicType G_DAY = makeAtomicType("gDay", ANY_ATOMIC, false);

    public static AtomicType HEX_BINARY = makeAtomicType("hexBinary", ANY_ATOMIC, false);

    public static AtomicType BASE64_BINARY = makeAtomicType("base64Binary", ANY_ATOMIC, false);

    public static AtomicType ANY_URI = makeAtomicType("anyURI", ANY_ATOMIC, true);

    public static AtomicType QNAME = makeAtomicType("QName", ANY_ATOMIC, false);

    public static AtomicType UNTYPED_ATOMIC = makeAtomicType("untypedAtomic", ANY_ATOMIC, true);

    public static AtomicType DECIMAL = makeAtomicType("decimal", NUMERIC, true);

    public static AtomicType FLOAT = makeAtomicType("float", NUMERIC, true);

    public static AtomicType DOUBLE = makeAtomicType("double", NUMERIC, true);

    public static AtomicType INTEGER = makeAtomicType("integer", DECIMAL, true);

    public static AtomicType YEAR_MONTH_DURATION = makeAtomicType("yearMonthDuration", DURATION, true);

    public static AtomicType DAY_TIME_DURATION = makeAtomicType("dayTimeDuration", DURATION, true);




    String localName;
    ItemType baseType;
    boolean ordered = false;

    private AtomicType(String localName) {
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
     * Test whether a given item conforms to this type
     * @param item  The item to be tested
     * @return true if the item is an instance of this type; false otherwise
     */

    public boolean matchesItem(Item item) {
        if (item instanceof AtomicValue) {
            ItemType type = ((AtomicValue)item).getItemType();
            do {
                if (type == this) {
                    return true;
                }
                type = type.getSuperType();
            } while (type != null);
        }
        return false;
    }

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     *
     * @return the supertype, or null if this type is item()
     */

    public ItemType getSuperType() {
        return baseType;
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public AtomicType getAtomizedItemType() {
        return this;
    }

    public String toString() {
        return getDisplayName();
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.