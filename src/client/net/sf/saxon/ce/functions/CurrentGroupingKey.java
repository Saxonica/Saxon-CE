package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.GroupIterator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* Implements the XSLT function current-grouping-key()
*/

public class CurrentGroupingKey extends SystemFunction {

    public CurrentGroupingKey newInstance() {
        return new CurrentGroupingKey();
    }

    /**
     * Determine the dependencies
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CURRENT_GROUP;
    }

    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext c) throws XPathException {
        GroupIterator gi = c.getCurrentGroupIterator();
        if (gi==null) {
            return null;
        }
        return gi.getCurrentGroupingKey();
     }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.