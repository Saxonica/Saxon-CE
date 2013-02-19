package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.BooleanValue;

/**
 * Implements the fn:contains() function
 */
public class Contains extends CollatingFunction {

    public Contains newInstance() {
        return new Contains();
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(evalContains(context));
    }

    protected boolean doComparison(String s0, String s1) {
        return s0.indexOf(s1, 0) >= 0;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


