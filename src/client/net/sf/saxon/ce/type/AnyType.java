package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.om.StandardNames;

/**
 * This class has a singleton instance which represents the XML Schema built-in type xs:anyType,
 * also known as the urtype.
 *
 * See XML Schema 1.1 Part 1 section 3.4.7
 */

public final class AnyType implements SchemaType {

    private static AnyType theInstance = new AnyType();

    /**
     * Private constructor
     */
    private AnyType() {
        super();
    }

    /**
     * Get the singular instance of this class
     * @return the singular object representing xs:anyType
     */

    public static AnyType getInstance() {
        return theInstance;
    }


    /**
     * Get the base type
     * @return null (this is the root of the type hierarchy)
     */

    public SchemaType getBaseType() {
        return null;
    }

    /**
     * Test whether this SchemaType is an atomic type
     * @return true if this SchemaType is an atomic type
     */

    public boolean isAtomicType() {
        return false;
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
     * Get the fingerprint of the name of this type
     * @return the fingerprint.
     */

    public int getFingerprint() {
        return StandardNames.XS_ANY_TYPE;
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return "xs:anyType";
    }


    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return (other instanceof AnyType);
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.