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
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     * @return a lexical QName identifying the type
     */

    String getDisplayName();

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     * @return the base type, or null if this is xs:anyType (the root of the type hierarchy)
    */

    SchemaType getBaseType();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.