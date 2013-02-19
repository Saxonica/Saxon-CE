package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * Callback interface used to evaluate sort keys. An instance of this class is passed to the
 * SortedIterator, and is used whenever a sort key value needs to be computed.
 */

public interface SortKeyEvaluator {

    /**
     * Evaluate the n'th sort key of the context item
     */

    public Item evaluateSortKey(int n, XPathContext context) throws XPathException;
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
