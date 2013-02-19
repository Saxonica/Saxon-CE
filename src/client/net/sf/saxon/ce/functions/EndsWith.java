package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.BooleanValue;

/**
 * Implements the fn:ends-with() function
 */
public class EndsWith extends CollatingFunction {

    public EndsWith newInstance() {
        return new EndsWith();
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(evalContains(context));
    }

    protected boolean doComparison(String s0, String s1) {
        return s0.endsWith(s1);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.