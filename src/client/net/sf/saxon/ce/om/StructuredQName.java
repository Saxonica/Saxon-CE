package client.net.sf.saxon.ce.om;

import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;


/**
 * This class provides an economical representation of a QName triple (prefix, URI, and localname).
 * The value is stored internally as a character array containing the concatenation of URI, localname,
 * and prefix (in that order) with two integers giving the start positions of the localname and prefix.
 *
 * <p><i>Instances of this class are immutable.</i></p>
 */

public class StructuredQName {

    private final static String EMPTY_STRING = "";

    private char[] content;
    private int localNameStart;
    private int prefixStart;

    /**
     * Construct a StructuredQName from a prefix, URI, and local name. This method performs no validation.
     * @param prefix The prefix. Use an empty string to represent the null prefix.
     * @param uri The namespace URI. Use an empty string or null to represent the no-namespace
     * @param localName The local part of the name
     */

    public StructuredQName(String prefix, String uri, String localName) {
        if (uri == null) {
            uri = "";
        }
        int plen = prefix.length();
        int ulen = uri.length();
        int llen = localName.length();
        localNameStart = ulen;
        prefixStart = ulen + llen;
        content = new char[ulen + llen + plen];
        uri.getChars(0, ulen, content, 0);
        localName.getChars(0, llen, content, ulen);
        prefix.getChars(0, plen, content, ulen+llen);
    }

    /**
     * Make a structuredQName from a Namepool nameCode
     * @param pool the NamePool
     * @param nameCode a name code that has been registered in the NamePool
     */

    public StructuredQName(NamePool pool, int nameCode) {
        this(pool.getPrefix(nameCode), pool.getURI(nameCode), pool.getLocalName(nameCode));
    }

    /**
     * Make a structuredQName from a Clark name
     * @param expandedName the name in Clark notation "{uri}local" if in a namespace, or "local" otherwise.
     * The format "{}local" is also accepted for a name in no namespace.
     * @return the constructed StructuredQName
     * @throws IllegalArgumentException if the Clark name is malformed
     */

    public static StructuredQName fromClarkName(String expandedName) {
        String namespace;
        String localName;
        if (expandedName.charAt(0) == '{') {
            int closeBrace = expandedName.indexOf('}');
            if (closeBrace < 0) {
                throw new IllegalArgumentException("No closing '}' in Clark name");
            }
            namespace = expandedName.substring(1, closeBrace);
            if (closeBrace == expandedName.length()) {
                throw new IllegalArgumentException("Missing local part in Clark name");
            }
            localName = expandedName.substring(closeBrace + 1);
        } else {
            namespace = "";
            localName = expandedName;
        }
        return new StructuredQName("", namespace, localName);
    }

    /**
     * Make a structured QName from a lexical QName, using a supplied NamespaceResolver to
     * resolve the prefix
     * @param lexicalName the QName as a lexical name (prefix:local)
     * @param useDefault set to true if an absent prefix implies use of the default namespace;
     * set to false if an absent prefix implies no namespace
     * @param resolver NamespaceResolver used to look up a URI for the prefix
     * @return the StructuredQName object corresponding to this lexical QName
     * @throws XPathException if the namespace prefix is not in scope or if the value is lexically
     * invalid. Error code FONS0004 is set if the namespace prefix has not been declared; error
     * code FOCA0002 is set if the name is lexically invalid.
     */

    public static StructuredQName fromLexicalQName(CharSequence lexicalName, boolean useDefault,
                                                   NamespaceResolver resolver)
    throws XPathException {
        try {
            String[] parts = NameChecker.getQNameParts(Whitespace.trimWhitespace(lexicalName));
            String uri = resolver.getURIForPrefix(parts[0], useDefault);
            if (uri == null) {
                XPathException de = new XPathException("Namespace prefix '" + parts[0] + "' has not been declared");
                de.setErrorCode("FONS0004");
                throw de;
            }
            return new StructuredQName(parts[0], uri, parts[1]);
        } catch (QNameException e) {
            XPathException de = new XPathException(e.getMessage());
            de.setErrorCode("FOCA0002");
            throw de;
        }
    }

    /**
     * Get the prefix of the QName.
     * @return the prefix. Returns the empty string if the name is unprefixed.
     */

    public String getPrefix() {
        return new String(content, prefixStart, content.length - prefixStart);
    }

    /**
     * Get the namespace URI of the QName.
     * @return the URI. Returns the empty string to represent the no-namespace
     */

    public String getNamespaceURI() {
        if (localNameStart == 0) {
            return EMPTY_STRING;
        }
        return new String(content, 0, localNameStart);
    }

    /**
     * Get the local part of the QName
     * @return the local part of the QName
     */

    public String getLocalName() {
        return new String(content, localNameStart, prefixStart - localNameStart);
    }

    /**
     * Get the display name, that is the lexical QName in the form [prefix:]local-part
     * @return the lexical QName
     */

    public String getDisplayName() {
        if (prefixStart == content.length) {
            return getLocalName();
        } else {
            FastStringBuffer buff = new FastStringBuffer(content.length - localNameStart + 1);
            buff.append(content, prefixStart, content.length - prefixStart);
            buff.append(':');
            buff.append(content, localNameStart, prefixStart - localNameStart);
            return buff.toString();
        }
    }

    /**
     * Get the expanded QName in Clark format, that is "{uri}local" if it is in a namespace, or just "local"
     * otherwise.
     * @return the QName in Clark notation
     */

    public String getClarkName() {
        FastStringBuffer buff = new FastStringBuffer(content.length - prefixStart + 2);
        if (localNameStart > 0) {
            buff.append('{');
            buff.append(content, 0, localNameStart);
            buff.append('}');
        }
        buff.append(content, localNameStart, prefixStart - localNameStart);
        return buff.toString();
    }

    /**
     * The toString() method displays the QName as a lexical QName, that is prefix:local
     * @return the lexical QName
     */

    public String toString() {
        return getDisplayName();
    }

    /**
     * Compare two StructuredQName values for equality. This compares the URI and local name parts,
     * excluding any prefix
     */

    public boolean equals(Object other) {
        if (other instanceof StructuredQName) {
            StructuredQName sq2 = (StructuredQName)other;
            if (localNameStart != sq2.localNameStart || prefixStart != sq2.prefixStart) {
                return false;
            }
            for (int i=prefixStart-1; i>=0; i--) {
                // compare from the end of the local name to maximize chance of finding a difference quickly
                if (content[i] != sq2.content[i]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get a hashcode to reflect the equals() method
     * @return a hashcode based on the URI and local part only, ignoring the prefix.
     */

    public int hashCode() {
        int h = 0x8004a00b;
        h ^= prefixStart;
        h ^= localNameStart;
        for (int i=prefixStart-1; i>=0; i--) {
            h ^= (content[i] << (i&0x1f));
        }
        return h;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
