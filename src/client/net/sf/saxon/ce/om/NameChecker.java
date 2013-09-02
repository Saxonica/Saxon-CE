package client.net.sf.saxon.ce.om;

import client.net.sf.saxon.ce.expr.z.IntRangeSet;
import client.net.sf.saxon.ce.regex.GeneralUnicodeString;
import client.net.sf.saxon.ce.regex.UnicodeString;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;


/**
 * NameChecker is a utility class containing static methods to perform validation and analysis of XML names,
 * as defined in XML 1.1 or XML 1.0 5th edition.
 * The class also handles validation of characters against the XML 1.1 rules.
 */

public abstract class NameChecker  {

    /**
     * Validate a QName, and return the prefix and local name. The local name is checked
     * to ensure it is a valid NCName. The prefix is not checked, on the theory that the caller
     * will look up the prefix to find a URI, and if the prefix is invalid, then no URI will
     * be found.
     *
     * @param qname the lexical QName whose parts are required. Note that leading and trailing
     *              whitespace is not permitted
     * @return an array of two strings, the prefix and the local name. The first
     *         item is a zero-length string if there is no prefix.
     * @throws QNameException if not a valid QName.
     */

    public static String[] getQNameParts(CharSequence qname) throws QNameException {
        String[] parts = new String[2];
        int colon = -1;
        int len = qname.length();
        for (int i = 0; i < len; i++) {
            if (qname.charAt(i) == ':') {
                colon = i;
                break;
            }
        }
        if (colon < 0) {
            parts[0] = "";
            parts[1] = qname.toString();
            if (!isValidNCName(parts[1])) {
                throw new QNameException("Invalid QName " + Err.wrap(qname));
            }
        } else {
            if (colon == 0) {
                throw new QNameException("QName cannot start with colon: " + Err.wrap(qname));
            }
            if (colon == len - 1) {
                throw new QNameException("QName cannot end with colon: " + Err.wrap(qname));
            }
            parts[0] = qname.subSequence(0, colon).toString();
            parts[1] = qname.subSequence(colon + 1, len).toString();

            if (!isValidNCName(parts[1])) {
                if (!isValidNCName(parts[0])) {
                    throw new QNameException("Both the prefix " + Err.wrap(parts[0]) +
                            " and the local part " + Err.wrap(parts[1]) + " are invalid");
                }
                throw new QNameException("Invalid QName local part " + Err.wrap(parts[1]));
            }
        }
        return parts;
    }

    /**
     * Validate a QName, and return the prefix and local name. Both parts are checked
     * to ensure they are valid NCNames.
     * <p/>
     * <p><i>Used from compiled code</i></p>
     *
     * @param qname the lexical QName whose parts are required. Note that leading and trailing
     *              whitespace is not permitted
     * @return an array of two strings, the prefix and the local name. The first
     *         item is a zero-length string if there is no prefix.
     * @throws XPathException if not a valid QName.
     */

    public static String[] checkQNameParts(CharSequence qname) throws XPathException {
        try {
            String[] parts = getQNameParts(qname);
            if (parts[0].length() > 0 && !isValidNCName(parts[0])) {
                throw new XPathException("Invalid QName prefix " + Err.wrap(parts[0]));
            }
            return parts;
        } catch (QNameException e) {
            XPathException err = new XPathException(e.getMessage());
            err.setErrorCode("FORG0001");
            throw err;
        }
    }


    // Both XML 1.0e5 and XML1.1e2 have
    // [4]   	NameStartChar	   ::=   	":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] |
    //                      [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] |
    //                      [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
    // [4a]   	NameChar	   ::=   	NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]

    // XML Namespaces removes the ":" option

    private static int[] nameStartRangeStartPoints = {
            'A', '_', 'a', 0xc0, 0xd8, 0xf8, 0x370, 0x37f, 0x200c, 0x2070, 0x2c00, 0x3001, 0xf900, 0xfdf0, 0x10000
    };

    private static int[] nameStartRangeEndPoints = {
            'Z', '_', 'z', 0xd6, 0xf6, 0x2ff, 0x37d, 0x1fff, 0x200d, 0x218f, 0x2fef, 0xd7ff, 0xfdcf, 0xfffd, 0xeffff
    };

    private static IntRangeSet ncNameStartChars = new IntRangeSet(nameStartRangeStartPoints, nameStartRangeEndPoints);

    private static int[] nameRangeStartPoints = {
            '-', '.', '0', 0xb7, 0x300, 0x203f
    };

    private static int[] nameRangeEndPoints = {
            '-', '.', '9', 0xb7, 0x36f, 0x2040
    };

    private static IntRangeSet ncNameChars = new IntRangeSet(nameRangeStartPoints, nameRangeEndPoints);


    public static boolean isNCNameStartChar(int c) {
        return ncNameStartChars.contains(c);
    }

    public static boolean isNCNameChar(int c) {
        return ncNameStartChars.contains(c) || ncNameChars.contains(c);
    }

    //private static RegExp ncNamePattern = RegExp.compile("^" + ncNameStartChar + ncNameChar + "*$");

    /**
     * Validate whether a given string constitutes a valid NCName, as defined in XML Namespaces.
     *
     * @param ncName the name to be tested. Any whitespace trimming must have already been applied.
     * @return true if the name is a lexically-valid QName
     */

    public static boolean isValidNCName(CharSequence ncName) {
        int len = ncName.length();
        if (len==0) {
            return false;
        }
        UnicodeString us = GeneralUnicodeString.makeUnicodeString(ncName);
        if (!isNCNameStartChar(us.charAt(0))) {
            return false;
        }
        for (int i=1; i<len; i++) {
            if (!isNCNameChar(us.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    /**
     * Test whether a character is a valid XML character
     *
     * @param ch the character to be tested
     * @return true if this is a valid character in XML 1.1
     */

    public static boolean isValidChar(int ch) {
        // from XML 1.0 fifth edition
        // Char	   ::=   	#x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        // from XML 1.1 second edition
        // Char	   ::=   	[#x1-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]

        return (ch >= 1 && ch <= 0xd7ff) ||
                (ch >= 0xe000 && ch <= 0xfffd) ||
                (ch >= 0x10000 && ch <= 0x10ffff);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
