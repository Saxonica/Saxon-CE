package client.net.sf.saxon.ce.type;

/**
 * This class has a singleton instance which represents the XML Schema built-in type xs:anyType,
 * also known as the urtype.
 *
 * See XML Schema 1.1 Part 1 section 3.4.7
 */

public final class AnyType extends BuiltInType implements SchemaType {

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
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return "xs:anyType";
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.