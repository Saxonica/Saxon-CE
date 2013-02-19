package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.InscopeNamespaceResolver;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamespaceResolver;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AnyURIValue;
import client.net.sf.saxon.ce.value.StringValue;


/**
* This class supports the function namespace-uri-for-prefix()
*/

public class NamespaceForPrefix extends SystemFunction {

    public NamespaceForPrefix newInstance() {
        return new NamespaceForPrefix();
    }

    /**
     * Evaluate the function
     * @param context the XPath dynamic context
     * @return the URI corresponding to the prefix supplied in the first argument, or null
     * if the prefix is not in scope
     * @throws XPathException if a failure occurs evaluating the arguments
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo element = (NodeInfo)argument[1].evaluateItem(context);
        StringValue p = (StringValue)argument[0].evaluateItem(context);
        String prefix;
        if (p == null) {
            prefix = "";
        } else {
            prefix = p.getStringValue();
        }
        NamespaceResolver resolver = new InscopeNamespaceResolver(element);
        String uri = resolver.getURIForPrefix(prefix, true);
        if (uri == null) {
            return null;
        }
        return new AnyURIValue(uri);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.