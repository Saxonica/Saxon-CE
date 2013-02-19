package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.GroupIterator;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* Implements the XSLT function current-group()
*/

public class CurrentGroup extends SystemFunction {

    public CurrentGroup newInstance() {
        return new CurrentGroup();
    }

    /**
     * Determine the dependencies
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CURRENT_GROUP;
    }

    /**
    * Return an iteration over the result sequence
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        GroupIterator gi = c.getCurrentGroupIterator();
        if (gi==null) {
            return EmptyIterator.getInstance();
        }
        return gi.iterateCurrentGroup();
    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
