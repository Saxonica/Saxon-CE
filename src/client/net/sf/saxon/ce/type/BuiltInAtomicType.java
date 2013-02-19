package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.value.*;

/**
 * This class represents a built-in atomic type, which may be either a primitive type
 * (such as xs:decimal or xs:anyURI) or a derived type (such as xs:ID or xs:dayTimeDuration).
 */

public class BuiltInAtomicType implements AtomicType {

    int fingerprint;
    int baseFingerprint;
    int primitiveFingerprint;
    boolean ordered = false;

    public static BuiltInAtomicType ANY_ATOMIC = makeAtomicType(StandardNames.XS_ANY_ATOMIC_TYPE, AnySimpleType.getInstance(), true);

    public static BuiltInAtomicType NUMERIC = makeAtomicType(StandardNames.XS_NUMERIC, ANY_ATOMIC, true);

    public static BuiltInAtomicType STRING = makeAtomicType(StandardNames.XS_STRING, ANY_ATOMIC, true);

    public static BuiltInAtomicType BOOLEAN = makeAtomicType(StandardNames.XS_BOOLEAN, ANY_ATOMIC, true);

    public static BuiltInAtomicType DURATION = makeAtomicType(StandardNames.XS_DURATION, ANY_ATOMIC, false);

    public static BuiltInAtomicType DATE_TIME = makeAtomicType(StandardNames.XS_DATE_TIME, ANY_ATOMIC, true);

    public static BuiltInAtomicType DATE = makeAtomicType(StandardNames.XS_DATE, ANY_ATOMIC, true);

    public static BuiltInAtomicType TIME = makeAtomicType(StandardNames.XS_TIME, ANY_ATOMIC, true);

    public static BuiltInAtomicType G_YEAR_MONTH = makeAtomicType(StandardNames.XS_G_YEAR_MONTH, ANY_ATOMIC, false);

    public static BuiltInAtomicType G_MONTH = makeAtomicType(StandardNames.XS_G_MONTH, ANY_ATOMIC, false);

    public static BuiltInAtomicType G_MONTH_DAY = makeAtomicType(StandardNames.XS_G_MONTH_DAY, ANY_ATOMIC, false);

    public static BuiltInAtomicType G_YEAR = makeAtomicType(StandardNames.XS_G_YEAR, ANY_ATOMIC, false);

    public static BuiltInAtomicType G_DAY = makeAtomicType(StandardNames.XS_G_DAY, ANY_ATOMIC, false);

    public static BuiltInAtomicType HEX_BINARY = makeAtomicType(StandardNames.XS_HEX_BINARY, ANY_ATOMIC, false);

    public static BuiltInAtomicType BASE64_BINARY = makeAtomicType(StandardNames.XS_BASE64_BINARY, ANY_ATOMIC, false);

    public static BuiltInAtomicType ANY_URI = makeAtomicType(StandardNames.XS_ANY_URI, ANY_ATOMIC, true);

    public static BuiltInAtomicType QNAME = makeAtomicType(StandardNames.XS_QNAME, ANY_ATOMIC, false);

    public static BuiltInAtomicType UNTYPED_ATOMIC = makeAtomicType(StandardNames.XS_UNTYPED_ATOMIC, ANY_ATOMIC, true);

    public static BuiltInAtomicType DECIMAL = makeAtomicType(StandardNames.XS_DECIMAL, NUMERIC, true);

    public static BuiltInAtomicType FLOAT = makeAtomicType(StandardNames.XS_FLOAT, NUMERIC, true);

    public static BuiltInAtomicType DOUBLE = makeAtomicType(StandardNames.XS_DOUBLE, NUMERIC, true);

    public static BuiltInAtomicType INTEGER = makeAtomicType(StandardNames.XS_INTEGER, DECIMAL, true);

    public static BuiltInAtomicType YEAR_MONTH_DURATION = makeAtomicType(StandardNames.XS_YEAR_MONTH_DURATION, DURATION, true);

    public static BuiltInAtomicType DAY_TIME_DURATION = makeAtomicType(StandardNames.XS_DAY_TIME_DURATION, DURATION, true);

    //public static BuiltInAtomicType ID = makeAtomicType(StandardNames.XS_ID, STRING, true);

    //public static BuiltInAtomicType LANGUAGE = makeAtomicType(StandardNames.XS_LANGUAGE, STRING, true);

    //public static BuiltInAtomicType IDREF = makeAtomicType(StandardNames.XS_IDREF, STRING, true);

    static {
        
    }

    private BuiltInAtomicType(int fingerprint) {
        this.fingerprint = fingerprint;
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
        switch (fingerprint) {
        case StandardNames.XS_INTEGER:
        case StandardNames.XS_DECIMAL:
        case StandardNames.XS_DOUBLE:
        case StandardNames.XS_FLOAT:
        case StandardNames.XS_NUMERIC:
            return true;
        default:
            return false;
        }
    }

    /**
     * Set the base type of this type
     *
     * @param baseFingerprint the namepool fingerprint of the name of the base type
     */

    public final void setBaseTypeFingerprint(int baseFingerprint) {
        this.baseFingerprint = baseFingerprint;
    }

    /**
     * Get the fingerprint of the name of this type
     *
     * @return the fingerprint. Returns an invented fingerprint for an anonymous type.
     */

    public int getFingerprint() {
        return fingerprint;
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        if (fingerprint == StandardNames.XS_NUMERIC) {
            return "numeric";
        } else {
            return StandardNames.getDisplayName(fingerprint);
        }
    }


    /**
     * Determine whether the atomic type is a primitive type.  The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration;
     * xs:untypedAtomic; and all supertypes of these (xs:anyAtomicType, xs:numeric, ...)
     *
     * @return true if the type is considered primitive under the above rules
     */

    public boolean isPrimitiveType() {
        return Type.isPrimitiveType(fingerprint);
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
        if (baseFingerprint == -1) {
            return null;
        } else {
            return BuiltInType.getSchemaType(baseFingerprint);
        }
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
            AtomicValue value = (AtomicValue)item;
            // Try to match primitive types first
            if (value.getPrimitiveType() == this) {
                return true;
            }
            AtomicType type = value.getTypeLabel();
            if (type.getFingerprint() == getFingerprint()) {
                // note, with compiled stylesheets one can have two objects representing
                // the same type, so comparing identity is not safe
                return true;
            }
            final TypeHierarchy th = config.getTypeHierarchy();
            boolean ok = th.isSubType(type, this);
            if (ok) {
                return true;
            }
            if (allowURIPromotion && getFingerprint() == StandardNames.XS_STRING && th.isSubType(type, BuiltInAtomicType.ANY_URI)) {
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
     * Get the primitive type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public int getPrimitiveType() {
        return primitiveFingerprint;
    }

    /**
     * Produce a representation of this type name for use in error messages.
     * Where this is a QName, it will use conventional prefixes
     */

    public String toString(NamePool pool) {
        return getDisplayName();
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public AtomicType getAtomizedItemType() {
        return this;
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return other.getFingerprint() == getFingerprint();
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
     * Ask whether this type is an ID type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:ID: that is, it includes types derived
     * from ID by restriction, list, or union. Note that for a node to be treated
     * as an ID, its typed value must be a *single* atomic value of type ID; the type of the
     * node, however, can still allow a list.
     */

    public boolean isIdType() {
        return fingerprint == StandardNames.XS_ID;
    }

    /**
     * Ask whether this type is an IDREF or IDREFS type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:IDREF: that is, it includes types derived
     * from IDREF or IDREFS by restriction, list, or union
     */

    public boolean isIdRefType() {
        return fingerprint == StandardNames.XS_IDREF; 
    }

    /**
     * Test whether this simple type is namespace-sensitive, that is, whether
     * it is derived from xs:QName or xs:NOTATION
     *
     * @return true if this type is derived from xs:QName or xs:NOTATION
     */

    public boolean isNamespaceSensitive() {
        return getFingerprint() == StandardNames.XS_QNAME;
    }

    /**
     * Two types are equal if they have the same fingerprint.
     * Note: it is normally safe to use ==, because we always use the static constants, one instance
     * for each built in atomic type. However, after serialization and deserialization a different instance
     * can appear.
     */

    public boolean equals(Object obj) {
        return obj instanceof BuiltInAtomicType &&
                getFingerprint() == ((BuiltInAtomicType)obj).getFingerprint();
    }

    /**
     * The fingerprint can be used as a hashcode
     */

    public int hashCode() {
        return getFingerprint();
    }


    /**
     * Internal factory method to create a BuiltInAtomicType. There is one instance for each of the
     * built-in atomic types
     *
     * @param fingerprint The name of the type
     * @param baseType    The base type from which this type is derived
     * @return the newly constructed built in atomic type
     */
    private static BuiltInAtomicType makeAtomicType(int fingerprint, SchemaType baseType, boolean ordered) {
        BuiltInAtomicType t = new BuiltInAtomicType(fingerprint);
        t.setBaseTypeFingerprint(baseType.getFingerprint());
        if (t.isPrimitiveType()) {
            t.primitiveFingerprint = fingerprint;
        } else {
            t.primitiveFingerprint = ((AtomicType)baseType).getPrimitiveType();
        }
        t.ordered = ordered;
        BuiltInType.register(fingerprint, t);
        return t;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.