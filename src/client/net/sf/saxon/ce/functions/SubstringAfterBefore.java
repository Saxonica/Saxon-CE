package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * Implements the fn:substring-after() function
 */
public class SubstringAfterBefore extends CollatingFunction {

    public static final int AFTER = 1;
    public static final int BEFORE = 2;

    public SubstringAfterBefore(int operation) {
        this.operation = operation;
    }

    public SubstringAfterBefore newInstance() {
        return new SubstringAfterBefore(operation);
    }
    /**
     * Evaluate the function
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        if (!(stringCollator instanceof CodepointCollator)) {
            doesNotSupportSubstringMatching(context);
        }

        String result;
        StringValue arg1 = (StringValue)argument[0].evaluateItem(context);
        StringValue arg2 = (StringValue)argument[1].evaluateItem(context);
        if (arg1 == null) {
            arg1 = StringValue.EMPTY_STRING;
        }
        if (arg2 == null) {
            arg2 = StringValue.EMPTY_STRING;
        }
        String s1 = arg1.getStringValue();
        String s2 = arg2.getStringValue();

        int index = s1.indexOf(s2);

        if (index < 0) {
            return StringValue.EMPTY_STRING;

        } else {

            if (operation == AFTER) {
                // substring-after()
                if (s2.isEmpty()) {
                    return arg1;
                }
                if (s1.isEmpty()) {
                    return StringValue.EMPTY_STRING;
                }

                result = s1.substring(index + s2.length());

            } else {
                // substring-before()

                if (s1.isEmpty() || s2.isEmpty()) {
                    return StringValue.EMPTY_STRING;
                }

                result = s1.substring(0, index);

            }
        }
        StringValue s = StringValue.makeStringValue(result);
        if (arg1.isKnownToContainNoSurrogates()) {
            s.setContainsNoSurrogates();
        }
        return s;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.