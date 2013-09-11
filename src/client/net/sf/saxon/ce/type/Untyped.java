package client.net.sf.saxon.ce.type;

/**
 * This class has a singleton instance which represents the complex type xs:untyped,
 * used for elements that have not been validated.
 */

public final class Untyped extends BuiltInType implements SchemaType {

    private static Untyped theInstance = new Untyped();

    /**
     * Private constructor
     */
    private Untyped() {
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return "xs:untyped";
    }

    /**
     * Returns the base type that this type inherits from.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     */

    public SchemaType getBaseType() {
        return AnyType.getInstance();
    }


    /**
     * Get the singular instance of this class
     *
     * @return the singular object representing xs:anyType
     */

    public static Untyped getInstance() {
        return theInstance;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.