package client.net.sf.saxon.ce.tree.wrapper;

import client.net.sf.saxon.ce.om.NodeInfo;

/**
 * Interface that extends NodeInfo by providing a method to get the position
 * of a node relative to its siblings.
 */

public interface SiblingCountingNode extends NodeInfo {

    /**
     * Get the index position of this node among its siblings (starting from 0)
     * @return 0 for the first child, 1 for the second child, etc.
     */
    public int getSiblingPosition();
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.