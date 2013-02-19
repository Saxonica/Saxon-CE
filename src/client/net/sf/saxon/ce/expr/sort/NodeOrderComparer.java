package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.om.NodeInfo;


/**
 * A Comparer used for comparing nodes in document order
 *
 * @author Michael H. Kay
 *
 */

public interface NodeOrderComparer  {

    /**
    * Compare two objects.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    */

    public int compare(NodeInfo a, NodeInfo b);

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.