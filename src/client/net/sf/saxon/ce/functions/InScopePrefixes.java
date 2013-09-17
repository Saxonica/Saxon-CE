package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.ListIterator;
import client.net.sf.saxon.ce.tree.util.NamespaceIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* This class supports the function in-scope-prefixes()
*/

public class InScopePrefixes extends SystemFunction {

    public InScopePrefixes newInstance() {
        return new InScopePrefixes();
    }

    /**
    * Iterator over the results of the expression
    */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        final NodeInfo element = (NodeInfo)argument[0].evaluateItem(context);
        final Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(element);
        final List<StringValue> prefixes = new ArrayList<StringValue>();
        prefixes.add(new StringValue("xml"));
        while (iter.hasNext()) {
            prefixes.add(new StringValue(iter.next().getPrefix()));
        }
        return new ListIterator(prefixes);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.