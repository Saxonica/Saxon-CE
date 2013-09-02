package client.net.sf.saxon.ce.type;

/**
 * This class has a singleton instance which represents the XML Schema built-in type xs:anySimpleType
 */

public final class AnySimpleType extends BuiltInType implements SchemaType {

    private static AnySimpleType theInstance = new AnySimpleType();

    /**
     * Private constructor
     */
    private AnySimpleType() {
    }

    /**
     * Ask whether this type is an ID type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:ID: that is, it includes types derived
     * from ID by restriction, list, or union. Note that for a node to be treated
     * as an ID, its typed value must be a *single* atomic value of type ID; the type of the
     * node, however, can still allow a list.
     */

    public boolean isIdType() {
        return false;
    }

    /**
     * Ask whether this type is an IDREF or IDREFS type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:IDREF: that is, it includes types derived
     * from IDREF or IDREFS by restriction, list, or union
     */

    public boolean isIdRefType() {
        return false;
    }

    /**
     * Get the singular instance of this class
     * @return the singular object representing xs:anyType
     */

    public static AnySimpleType getInstance() {
        return theInstance;
    }

    /**
     * Get the base type
     * @return AnyType
     */

    public SchemaType getBaseType() {
        return AnyType.getInstance();
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return "xs:anySimpleType";
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return (other instanceof AnySimpleType);
    }

    /**
     * Test whether this Simple Type is an atomic type
     * @return false, this is not (necessarily) an atomic type
     */

    public boolean isAtomicType() {
        return false;
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.