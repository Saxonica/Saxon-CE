package client.net.sf.saxon.ce.type;

import java.util.HashMap;

/**
 * This non-instantiable class acts as a register of Schema objects containing all the built-in types:
 * that is, the types defined in the "xs" namespace.
 *
 * <p>Previously called BuiltInSchemaFactory; but its original function has largely been moved to the two
 * classes {@link BuiltInAtomicType}
 */

public class BuiltInType  {

    /**
    * Table of all built in types
    */

    private static HashMap<String, BuiltInType> lookup = new HashMap(20);

    protected BuiltInType() {
    }

    static {
        register("anySimpleType", AnySimpleType.getInstance());
        register("anyType", AnyType.getInstance());
        register("untyped", Untyped.getInstance());
    }

    /**
     * Get the schema type with a given fingerprint
     * @param localName the local name of the type, in the XSD namespace
     * @return the SchemaType object representing the given type, if known, otherwise null
     */

    public static BuiltInType getSchemaType(String localName) {
        BuiltInType st = lookup.get(localName);
        if (st == null) {
            // this means the method has been called before doing the static initialization of BuiltInAtomicType
            // or BuiltInListType. So force it now
            if (BuiltInAtomicType.DOUBLE == null) {
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

    static void register(String localName, BuiltInType type) {
        lookup.put(localName, type);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.