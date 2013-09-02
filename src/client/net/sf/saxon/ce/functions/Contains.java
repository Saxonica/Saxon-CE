package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.*;

/**
 * Implements the fn:contains() function, also starts-with() and ends-with()
 */
public class Contains extends CollatingFunction {

    public final static int CONTAINS = 0;
    public final static int STARTS_WITH = 1;
    public final static int ENDS_WITH = 2;

    public Contains(int op) {
        operation = op;
    }

    public Contains newInstance() {
        return new Contains(operation);
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        boolean result;
        StringValue arg1 = (StringValue)argument[1].evaluateItem(context);
        if (arg1==null || arg1.isZeroLength()) {
            result = true;
        } else {

            StringValue arg0 = (StringValue)argument[0].evaluateItem(context);
            if (arg0==null || arg0.isZeroLength()) {
                result = false;
            } else {

                String s0 = arg0.getStringValue();
                String s1 = arg1.getStringValue();

                StringCollator collator = getCollator(2, context);
                if (collator instanceof CodepointCollator) {
                    switch (operation) {
                        case CONTAINS:
                            result = s0.indexOf(s1, 0) >= 0;
                            break;
                        case STARTS_WITH:
                            result = s0.startsWith(s1, 0);
                            break;
                        case ENDS_WITH:
                        default:
                            result = s0.endsWith(s1);
                    }
                } else {
                    doesNotSupportSubstringMatching(context);
                    result = false;

                }
            }
        }
        return BooleanValue.get(result);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


