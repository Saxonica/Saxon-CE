package client.net.sf.saxon.ce.trace;

import java.util.Iterator;

import client.net.sf.saxon.ce.om.StructuredQName;


/**
* Information about an instruction in the stylesheet or a construct in a Query, made
* available at run-time to a TraceListener
*/

public interface InstructionInfo {
    // extends SaxonLocator in server-side Saxon
    /**
     * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
     * (values in {@link net.sf.saxon.om.StandardNames}: all less than 1024)
     * or it will be a constant in class {@link Location}.
     * @return an integer identifying the kind of construct
     */

    public int getConstructType();              

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * @return the QName of the object declared or manipulated by this instruction or expression
     */

    /*@Nullable*/ public StructuredQName getObjectName();

    /**
    * Get the system identifier (URI) of the source stylesheet or query module containing
    * the instruction. This will generally be an absolute URI. If the system
    * identifier is not known, the method may return null. In some cases, for example
    * where XML external entities are used, the correct system identifier is not
    * always retained.
     * @return the URI of the containing module
    */

    /*@Nullable*/ public String getSystemId();

    /**
    * Get the line number of the instruction in the source stylesheet module.
    * If this is not known, or if the instruction is an artificial one that does
    * not relate to anything in the source code, the value returned may be -1.
     * @return the line number of the expression within the containing module
    */

    public int getLineNumber();

    /**
     * Get the value of a particular property of the instruction. Properties
     * of XSLT instructions are generally known by the name of the stylesheet attribute
     * that defines them.
     * @param name The name of the required property
     * @return  The value of the requested property, or null if the property is not available
     */

    /*@Nullable*/ public Object getProperty(String name);

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property. The iterator may return properties whose
     * value is null.
     * @return an iterator over the properties.
     */

    public Iterator<String> getProperties();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.