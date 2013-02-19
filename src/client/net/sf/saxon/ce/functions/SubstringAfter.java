package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * Implements the fn:substring-after() function
 */
public class SubstringAfter extends CollatingFunction {

    public SubstringAfter newInstance() {
        return new SubstringAfter();
    }
    /**
     * Evaluate the function
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StringValue arg1 = (StringValue)argument[0].evaluateItem(context);
        StringValue arg2 = (StringValue)argument[1].evaluateItem(context);

        if (arg1 == null) {
            arg1 = StringValue.EMPTY_STRING;
        }
        if (arg2 == null) {
            arg2 = StringValue.EMPTY_STRING;
        }
        if (arg2.isZeroLength()) {
            return arg1;
        }
        if (arg1.isZeroLength()) {
            return StringValue.EMPTY_STRING;
        }

        String s1 = arg1.getStringValue();
        String s2 = arg2.getStringValue();

        String result = null;
        if (stringCollator instanceof CodepointCollator) {
            // fast path for this common case
            int i = s1.indexOf(s2);
            if (i < 0) {
                result = "";
            } else {
                result = s1.substring(i + s2.length());
            }

        } else {
            doesNotSupportSubstringMatching(context);
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