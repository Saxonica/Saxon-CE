package client.net.sf.saxon.ce.tree.wrapper;

import client.net.sf.saxon.ce.om.NodeInfo;

/**
 * Callback to create a VirtualNode that wraps a given NodeInfo
 */
public interface WrappingFunction {

    /**
     * Factory method to wrap a node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The underlying node
     * @param parent      The wrapper for the parent of the node (null if unknown)
     * @return            The new wrapper for the supplied node
     */

    public VirtualNode makeWrapper(NodeInfo node, VirtualNode parent);


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
