package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.StringValue;


/**
 * Implements the fn:substring-before() function
 */
public class SubstringBefore extends CollatingFunction {

    public SubstringBefore newInstance() {
        return new SubstringBefore();
    }
   /**
     * Evaluate the function
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StringValue arg1 = (StringValue)argument[1].evaluateItem(context);
        if (arg1==null || arg1.isZeroLength()) {
            return StringValue.EMPTY_STRING;
        }

        StringValue arg0 = (StringValue)argument[0].evaluateItem(context);
        if (arg0==null || arg0.isZeroLength()) {
            return StringValue.EMPTY_STRING;
        }

        String s0 = arg0.getStringValue();
        String s1 = arg1.getStringValue();

        String result = null;
        if (stringCollator instanceof CodepointCollator) {
            // fast path for this common case
            int j = s0.indexOf(s1);
            if (j<0) {
                result = "";
            } else {
                result = s0.substring(0, j);
            }

        } else {
            doesNotSupportSubstringMatching(context);
        }
        StringValue s = StringValue.makeStringValue(result);
        if (arg0.isKnownToContainNoSurrogates()) {
            s.setContainsNoSurrogates();
        }
        return s;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
