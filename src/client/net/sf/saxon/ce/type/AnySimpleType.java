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

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.