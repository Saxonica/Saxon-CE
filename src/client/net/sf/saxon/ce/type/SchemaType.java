package client.net.sf.saxon.ce.type;

/**
 * SchemaType is an interface implemented by all schema types: simple and complex types, built-in and
 * user-defined types.
 *
 * <p>There is a hierarchy of interfaces that extend SchemaType, representing the top levels of the schema
 * type system: SimpleType and ComplexType, with SimpleType further subdivided into List, Union, and Atomic
 * types.</p>
 *
 * <p>The implementations of these interfaces are organized into a different hierarchy: on the one side,
 * built-in types such as AnyType, AnySimpleType, and the built-in atomic types and list types; on the other
 * side, user-defined types defined in a schema.</p>
 */

public interface SchemaType {

    /**
     * Get the fingerprint of the name of this type
     * @return the fingerprint. Returns an invented fingerprint for an anonymous type.
     */

    int getFingerprint();

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     * @return a lexical QName identifying the type
     */

    String getDisplayName();

    /**
     * Test whether this SchemaType is an atomic type
     * @return true if this SchemaType is an atomic type
     */

    boolean isAtomicType();

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     * @return the base type, or null if this is xs:anyType (the root of the type hierarchy)
     * @throws IllegalStateException if this type is not valid.
    */

    SchemaType getBaseType();

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     * @param other the other type
     * @return true if this is the same type as other
     */

    boolean isSameType(SchemaType other);

    /**
     * Ask whether this type is an ID type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:ID: that is, it includes types derived
     * from ID by restriction, list, or union. Note that for a node to be treated
     * as an ID in XSD 1.0, its typed value must be a *single* atomic value of type ID; the type of the
     * node, however, can still allow a list. But in XSD 1.1, a list of IDs is permitted
     */

    public boolean isIdType();

    /**
     * Ask whether this type is an IDREF or IDREFS type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:IDREF: that is, it includes types derived
     * from IDREF or IDREFS by restriction, list, or union
     */

    public boolean isIdRefType();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.