package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.regex.ARegularExpression;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;


/**
 * This class implements the replace() function for replacing
 * substrings that match a regular expression
 */

public class Replace extends SystemFunction {

    public Replace newInstance() {
        return new Replace();
    }

    /**
     * Evaluate the function in a string context
     */

    public Item evaluateItem(XPathContext c) throws XPathException {

        AtomicValue arg0 = (AtomicValue) argument[0].evaluateItem(c);
        if (arg0 == null) {
            arg0 = StringValue.EMPTY_STRING;
        }

        AtomicValue arg2 = (AtomicValue) argument[2].evaluateItem(c);
        CharSequence replacement = arg2.getStringValueCS();
        String msg = checkReplacement(replacement);
        if (msg != null) {
            dynamicError(msg, "FORX0004", c);
        }

        AtomicValue arg1 = (AtomicValue) argument[1].evaluateItem(c);

        CharSequence flags;

        if (argument.length == 3) {
            flags = "";
        } else {
            AtomicValue arg3 = (AtomicValue) argument[3].evaluateItem(c);
            flags = arg3.getStringValueCS();
        }

        try {
            ARegularExpression re = new ARegularExpression(arg1.getStringValueCS(), flags.toString(), "XP20", null);
            // check that it's not a pattern that matches ""
            if (re.matches("")) {
                dynamicError(
                        "The regular expression in replace() must not be one that matches a zero-length string",
                        "FORX0003", c);
            }
            String input = arg0.getStringValue();
            CharSequence res = re.replace(input, replacement);
            return StringValue.makeStringValue(res);
        } catch (XPathException err) {
            XPathException de = new XPathException(err);
            de.setErrorCode("FORX0002");
            de.setXPathContext(c);
            de.maybeSetLocation(getSourceLocator());
            throw de;
        }


    }


    /**
     * Check the contents of the replacement string
     *
     * @param rep the replacement string
     * @return null if the string is OK, or an error message if not
     */

    public static String checkReplacement(CharSequence rep) {
        for (int i = 0; i < rep.length(); i++) {
            char c = rep.charAt(i);
            if (c == '$') {
                if (i + 1 < rep.length()) {
                    char next = rep.charAt(++i);
                    if (next < '0' || next > '9') {
                        return "Invalid replacement string in replace(): $ sign must be followed by digit 0-9";
                    }
                } else {
                    return "Invalid replacement string in replace(): $ sign at end of string";
                }
            } else if (c == '\\') {
                if (i + 1 < rep.length()) {
                    char next = rep.charAt(++i);
                    if (next != '\\' && next != '$') {
                        return "Invalid replacement string in replace(): \\ character must be followed by \\ or $";
                    }
                } else {
                    return "Invalid replacement string in replace(): \\ character at end of string";
                }
            }
        }
        return null;
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.