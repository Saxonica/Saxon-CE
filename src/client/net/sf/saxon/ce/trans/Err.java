package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;

/**
 * Class containing utility methods for handling error messages
 */

public class Err {

    public static final int ELEMENT = 1;
    public static final int ATTRIBUTE = 2;
    public static final int FUNCTION = 3;
    public static final int VALUE = 4;
    public static final int VARIABLE = 5;
    public static final int GENERAL = 6;
    public static final int URI = 7;

    /**
     * Add delimiters to represent variable information within an error message
     * @param cs the variable information to be delimited
     * @return the delimited variable information
     */
    public static String wrap(CharSequence cs) {
        return wrap(cs, GENERAL);
    }

    /**
     * Add delimiters to represent variable information within an error message
     * @param cs the variable information to be delimited
     * @param valueType the type of value, e.g. element name or attribute name
     * @return the delimited variable information
     */
    public static String wrap(CharSequence cs, int valueType) {
        if (cs == null) {
            return "(NULL)";
        }
        if (cs.length() == 0) {
            return "(zero-length-string)";
        }
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        int len = cs.length();
        for (int i=0; i<len; i++) {
            char c = cs.charAt(i);
            if (c < 32 || c > 255) {
                sb.append("\\u");
                sb.append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }
        String s;
        if (len > 30) {
            if (valueType == ELEMENT && sb.charAt(0) == '{') {
                StructuredQName qn = StructuredQName.fromClarkName(sb.toString());
                String uri = qn.getNamespaceURI();
                if (uri.length() > 15) {
                    uri = "..." + uri.substring(uri.length()-15);
                }
                s = "{" + uri + "}" + qn.getLocalName();
            } else if (valueType == URI) {
                s = "..." + sb.toString().substring(len-30);
            } else {
                s = sb.toString().substring(0, 30) + "...";
            }
        } else {
            s = sb.toString();
        }
        switch (valueType) {
            case ELEMENT:
                return "<" + s + ">";
            case ATTRIBUTE:
                return "@" + s;
            case FUNCTION:
                return s + "()";
            case VARIABLE:
                return "$" + s;
            case VALUE:
                return "\"" + s + "\"";
            default:
                return "{" + s + "}";
        }
    }

}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.