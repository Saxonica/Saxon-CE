package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.StructuredQName;


/**
 * A qualified name: this is an abstract superclass for QNameValue and NotationValue, representing the
 * XPath primitive types xs:QName and xs:NOTATION respectively
 */

public abstract class QualifiedNameValue extends AtomicValue {

    protected StructuredQName qName;


    /**
     * Get the string value as a String. Returns the QName as a lexical QName, retaining the original
     * prefix if available.
     */

    public final String getPrimitiveStringValue() {
        return qName.getDisplayName();
    }

    /**
     * Convert to a StructuredQName
     * @return the name as a StructuredQName
     */

    public StructuredQName toStructuredQName() {
        return qName;
    }

    /**
     * Get the QName in Clark notation, that is "{uri}local" if in a namespace, or "local" otherwise
     */

    public final String getClarkName() {
        return qName.getClarkName();
    }

    /**
     * Get the local part
     */

    public final String getLocalName() {
        return qName.getLocalName();
    }

    /**
     * Get the namespace part. Returns the empty string for a name in no namespace.
     */

    public final String getNamespaceURI() {
        return qName.getNamespaceURI();
    }

    /**
     * Get the prefix. Returns the empty string if the name is unprefixed.
     */

    public final String getPrefix() {
        return qName.getPrefix();
    }

    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     *
     * @param ordered true if an ordered comparison is required. In this case the result is null if the
     *                type is unordered; in other cases the returned value will be a Comparable.
     * @param collator
     * @param context the XPath dynamic evaluation context, used in cases where the comparison is context
*                sensitive @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return (ordered ? null : this);
    }

    public int hashCode() {
        return qName.hashCode();
    }

    /**
     * The toString() method returns the name in the form QName("uri", "local")
     * @return the name in in the form QName("uri", "local")
     */

    public String toString() {
        return "QName(\"" + getNamespaceURI() + "\", \"" + getLocalName() + "\")";
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
