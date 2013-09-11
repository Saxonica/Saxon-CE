package client.net.sf.saxon.ce.tree.wrapper;

import client.net.sf.saxon.ce.om.NodeInfo;

/**
 * This interface is implemented by NodeInfo implementations that act as wrappers
 * on some underlying tree. It provides a method to access the real node underlying
 * the virtual node, for use by applications that need to drill down to the
 * underlying data.
 */

public interface VirtualNode extends NodeInfo {

    /**
     * Get the node underlying this virtual node. Note that this may itself be
     * a VirtualNode; you may have to drill down through several layers of
     * wrapping.
     * <p>
     * In some cases a single VirtualNode may represent an XPath text node that maps to a sequence
     * of adjacent nodes (for example text nodes and CDATA nodes) in the underlying tree. In this case
     * the first node in this sequence is returned.
     * @return The underlying node.
     */

    public Object getUnderlyingNode();

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.