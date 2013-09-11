package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AnyURIValue;

/**
* This class supports the base-uri() function in XPath 2.0
*/

public class BaseURI extends SystemFunction {

    public BaseURI newInstance() {
        return new BaseURI();
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(c);
        if (node==null) {
            return null;
        }
        String s = node.getBaseURI();
        if (s == null) {
            return null;
        }
        return new AnyURIValue(s);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
