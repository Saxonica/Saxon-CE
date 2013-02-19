package client.net.sf.saxon.ce.tree.linked;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.NodeTest;

final class ChildEnumeration extends TreeEnumeration {

    public ChildEnumeration(NodeImpl node, NodeTest nodeTest) {
        super(node, nodeTest);
        next = (NodeImpl)node.getFirstChild();
        while (!conforms(next)) {
            step();
        }
    }

    protected void step() {
        next = (NodeImpl)next.getNextSibling();
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new ChildEnumeration(start, nodeTest);
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
