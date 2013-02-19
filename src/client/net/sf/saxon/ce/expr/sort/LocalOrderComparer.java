package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.om.NodeInfo;


/**
 * A Comparer used for comparing nodes in document order. This
 * comparer assumes that the nodes being compared come from the same document
 *
 * @author Michael H. Kay
 *
 */

public final class LocalOrderComparer implements NodeOrderComparer {

    private static LocalOrderComparer instance = new LocalOrderComparer();

    /**
    * Get an instance of a LocalOrderComparer. The class maintains no state
    * so this returns the same instance every time.
    */

    public static LocalOrderComparer getInstance() {
        return instance;
    }

    public int compare(NodeInfo a, NodeInfo b) {
        NodeInfo n1 = (NodeInfo)a;
        NodeInfo n2 = (NodeInfo)b;
        return n1.compareOrder(n2);
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.