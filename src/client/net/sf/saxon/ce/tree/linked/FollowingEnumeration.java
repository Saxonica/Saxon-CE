package client.net.sf.saxon.ce.tree.linked;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.type.Type;

final class FollowingEnumeration extends TreeEnumeration {

    private NodeImpl root;

    public FollowingEnumeration(NodeImpl node, NodeTest nodeTest) {
        super(node, nodeTest);
        root = (DocumentImpl)node.getDocumentRoot();
        // skip the descendant nodes if any
        int type = node.getNodeKind();
        if (type==Type.ATTRIBUTE || type==Type.NAMESPACE) {
            next = ((NodeImpl)node.getParent()).getNextInDocument(root);
        } else {
            do {
                next = (NodeImpl)node.getNextSibling();
                if (next==null) node = (NodeImpl)node.getParent();
            } while (next==null && node!=null);
        }
        while (!conforms(next)) {
            step();
        }
    }

    protected void step() {
        next = next.getNextInDocument(root);
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new FollowingEnumeration(start, nodeTest);
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
