package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.UTF16CharacterSet;
import client.net.sf.saxon.ce.tree.util.UTF8CharacterSet;
import client.net.sf.saxon.ce.value.StringValue;

import java.util.Arrays;

/**
 * This class supports the functions encode-for-uri() and iri-to-uri()
 */

public class EscapeURI extends SystemFunction {

    public EscapeURI(int operation) {
        this.operation = operation;
    }

    public EscapeURI newInstance() {
        return new EscapeURI(operation);
    }

    public static final int ENCODE_FOR_URI = 1;
    public static final int IRI_TO_URI = 2;
    public static final int HTML_URI = 3;

    public static boolean[] allowedASCII = new boolean[128];

    static {
        Arrays.fill(allowedASCII, 0, 32, false);
        Arrays.fill(allowedASCII, 33, 127, true);
        for (int c : new int[]{'"', '<', '>', '\\', '^', '`', '{', '|', '}'}) {
            allowedASCII[c] = false;
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        Item item = argument[0].evaluateItem(c);
        if (item == null) {
            return StringValue.EMPTY_STRING;
        }
        final CharSequence s = item.getStringValue();
        switch (operation) {
            case ENCODE_FOR_URI:
                return StringValue.makeStringValue(escape(s, "-_.~"));
            case IRI_TO_URI:
                return StringValue.makeStringValue(iriToUri(s));
            case HTML_URI:
                return StringValue.makeStringValue(escapeHtmlURL(s));
            default:
                throw new UnsupportedOperationException("Unknown escape operation");
        }
    }

    /**
     * Escape special characters in a URI. The characters that are %HH-encoded are
     * all non-ASCII characters
     * @param s the URI to be escaped
     * @return the %HH-encoded string
     */

    public static CharSequence iriToUri(CharSequence s) {
        // NOTE: implements a late spec change which says that characters that are illegal in an IRI,
        // for example "\", must be %-encoded.
        if (allAllowedAscii(s)) {
            // it's worth doing a prescan to avoid the cost of copying in the common all-ASCII case
            return s;
        }
        FastStringBuffer sb = new FastStringBuffer(s.length()+20);
        for (int i=0; i<s.length(); i++) {
            final char c = s.charAt(i);
            if (c>=0x7f || !allowedASCII[(int)c]) {
                escapeChar(c, ((i+1)<s.length() ? s.charAt(i+1) : ' '), sb);
            } else {
                sb.append(c);
            }
        }
        return sb;
    }

    private static boolean allAllowedAscii(CharSequence s) {
        for (int i=0; i<s.length(); i++) {
            final char c = s.charAt(i);
            if (c>=0x7f || !allowedASCII[(int)c]) {
                return false;
            }
        }
        return true;
    }


    /**
     * Escape special characters in a URI. The characters that are %HH-encoded are
     * all non-ASCII characters, plus all ASCII characters except (a) letter A-Z
     * and a-z, (b) digits 0-9, and (c) characters listed in the allowedPunctuation
     * argument
     * @param s the URI to be escaped
     * @param allowedPunctuation ASCII characters other than letters and digits that
     * should NOT be %HH-encoded
     * @return the %HH-encoded string
     */

    public static CharSequence escape(CharSequence s, String allowedPunctuation) {
        FastStringBuffer sb = new FastStringBuffer(s.length());
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if ((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9')) {
                sb.append(c);
            } else if (c<=0x20 || c>=0x7f) {
                escapeChar(c, ((i+1)<s.length() ? s.charAt(i+1) : ' '), sb);
            } else if (allowedPunctuation.indexOf(c) >= 0) {
                sb.append(c);
            } else {
                escapeChar(c, ' ', sb);
            }

        }
        return sb;
    }

    private static final String hex = "0123456789ABCDEF";

    /**
     * Escape a single character in %HH representation, or a pair of two chars representing
     * a surrogate pair
     * @param c the character to be escaped, or the first character of a surrogate pair
     * @param c2 the second character of a surrogate pair
     * @param sb the buffer to contain the escaped result
     */

    private static void escapeChar(char c, char c2, FastStringBuffer sb) {
        byte[] array = new byte[4];
        int used = UTF8CharacterSet.getUTF8Encoding(c, c2, array);
        for (int b=0; b<used; b++) {
            int v = (int)array[b] & 0xff;
            sb.append('%');
            sb.append(hex.charAt(v/16));
            sb.append(hex.charAt(v%16));
        }
    }


    /**
     * Escape a URI according to the HTML rules: that is, a non-ASCII character (specifically,
     * a character outside the range 32 - 126) is replaced by the %HH encoding of the octets in
     * its UTF-8 representation
     * @param url the URI to be escaped
     * @return the URI after escaping non-ASCII characters
     */

    public static CharSequence escapeHtmlURL(CharSequence url) {
        FastStringBuffer sb = new FastStringBuffer(url.length() + 20);
        for (int i=0; i<url.length(); i++) {
            char ch = url.charAt(i);
            if (ch<32 || ch>126) {
                char c2 = ' ';
                if (UTF16CharacterSet.isHighSurrogate(ch)) {
                    c2 = url.charAt(++i);
                }
                escapeChar(ch, c2, sb);
            } else {
                sb.append(ch);
            }
        }
        return sb;
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.