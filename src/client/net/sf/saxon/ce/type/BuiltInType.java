package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.om.StandardNames;

import java.util.HashMap;

/**
 * This non-instantiable class acts as a register of Schema objects containing all the built-in types:
 * that is, the types defined in the "xs" namespace.
 *
 * <p>Previously called BuiltInSchemaFactory; but its original function has largely been moved to the two
 * classes {@link BuiltInAtomicType}
 */

public abstract class BuiltInType  {

    /**
    * Table of all built in types
    */

    private static HashMap<Integer, SchemaType> lookup = new HashMap(20);

    /**
     * Class is never instantiated
     */

    private BuiltInType() {
    }

    static {
        register(StandardNames.XS_ANY_SIMPLE_TYPE, AnySimpleType.getInstance());
        register(StandardNames.XS_ANY_TYPE, AnyType.getInstance());
        register(StandardNames.XS_UNTYPED, Untyped.getInstance());
    }

    /**
     * Get the schema type with a given fingerprint
     * @param fingerprint the fingerprint representing the name of the required type
     * @return the SchemaType object representing the given type, if known, otherwise null
     */

    public static SchemaType getSchemaType(int fingerprint) {
        SchemaType st = lookup.get(fingerprint);
        if (st == null) {
            // this means the method has been called before doing the static initialization of BuiltInAtomicType
            // or BuiltInListType. So force it now
            if (BuiltInAtomicType.DOUBLE == null) {
                // no action, except to force the initialization to run
            }
            st = lookup.get(fingerprint);
        }
        return st;                  
    }

    /**
     * Method for internal use to register a built in type with this class
     * @param fingerprint the fingerprint of the type name
     * @param type the SchemaType representing the built in type
     */

    static void register(int fingerprint, SchemaType type) {
        lookup.put(fingerprint, type);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.