package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.functions.Component;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;

/**
 * A QName value. This implements the so-called "triples proposal", in which the prefix is retained as
 * part of the value. The prefix is not used in any operation on a QName other than conversion of the
 * QName to a string.
 */

public class QNameValue extends AtomicValue {

    protected StructuredQName qName;


    /**
     * Constructor for a QName that is known to be valid. No validation takes place.
     * @param prefix The prefix part of the QName (not used in comparisons). Use "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use "" to represent the non-namespace.
     * @param localName The local part of the QName
     */

    public QNameValue(String prefix, String uri, String localName) {
        qName = new StructuredQName(prefix, uri, localName);
    }

    /**
     * Constructor starting from a StructuredQName
     * @param name the QName
     */

    public QNameValue(StructuredQName name) {
        qName = name;
    }


    /**
     * Constructor. This constructor validates that the local part is a valid NCName.
     *
     * @param prefix The prefix part of the QName (not used in comparisons). Use "" to represent the
     * default prefix (but null is also accepted).
     * Note that the prefix is not checked for lexical correctness, because in most cases
     * it will already have been matched against in-scope namespaces. Where necessary the caller must
     * check the prefix.
     * @param uri The namespace part of the QName. Use null to represent the non-namespace (but "" is also
     * accepted).
     * @param localName The local part of the QName
     * @throws XPathException if the local part of the name is malformed or if the name has a null
     * namespace with a non-empty prefix
     */

    public QNameValue(String prefix, String uri, String localName, boolean validate) throws XPathException {
        if (!NameChecker.isValidNCName(localName)) {
            XPathException err = new XPathException("Malformed local name in QName: '" + localName + '\'');
            err.setErrorCode("FORG0001");
            throw err;
        }
        prefix = (prefix==null ? "" : prefix);
        uri = ("".equals(uri) ? null : uri);
        if (uri == null && prefix.length() != 0) {
            XPathException err = new XPathException("QName has null namespace but non-empty prefix");
            err.setErrorCode("FOCA0002");
            throw err;
        }
        qName = new StructuredQName(prefix, uri, localName);
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public AtomicType getItemType() {
        return AtomicType.QNAME;
    }

    /**
     * Convert a QName to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convert(AtomicType requiredType) {
        if (requiredType == AtomicType.ANY_ATOMIC || requiredType == AtomicType.QNAME) {
            return this;
        } else if (requiredType == AtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == AtomicType.STRING) {
            return new StringValue(getStringValue());
        } else {
            return new ValidationFailure("Cannot convert QName to " + requiredType.getDisplayName(), "XPTY0004");
        }
    }

    /**
     * Get a component. Returns a zero-length string if the namespace-uri component is
     * requested and is not present.
     * @param part either Component.LOCALNAME or Component.NAMESPACE indicating which
     * component of the value is required
     * @return either the local name or the namespace URI, in each case as a StringValue
     */

    public AtomicValue getComponent(int part) {
        if (part == Component.LOCALNAME) {
            return new StringValue(getLocalName());
        } else if (part == Component.NAMESPACE) {
            return new AnyURIValue(getNamespaceURI());
        } else if (part == Component.PREFIX) {
            String prefix = getPrefix();
            if (prefix.length() == 0) {
                return null;
            } else {
                return new StringValue(prefix);
            }
        } else {
            throw new UnsupportedOperationException("Component of QName must be URI, Local Name, or Prefix");
        }
    }

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
     * @param implicitTimezone
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, int implicitTimezone) {
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


    /**
     * Determine if two QName values are equal. This comparison ignores the prefix part
     * of the value.
     * @throws ClassCastException if they are not comparable
     */

    public boolean equals(Object other) {
        return other instanceof QNameValue && qName.equals(((QNameValue)other).qName);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
