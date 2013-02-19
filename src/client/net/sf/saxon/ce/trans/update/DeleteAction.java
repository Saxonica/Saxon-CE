package client.net.sf.saxon.ce.trans.update;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.trans.XPathException;
import com.google.gwt.dom.client.Node;

/**
 * A pending update action representing the effect of a delete expression
 */
public class DeleteAction extends PendingUpdateAction {

    private Node targetNode;

    /**
     * Create a DeleteAction
     * @param node the node to be deleted
     */

    public DeleteAction(Node node) {
        targetNode = node;
    }

    /**
     * Apply the pending update action to the affected node
     *
     * @param context the XPath evaluation context
     */

    public void apply(XPathContext context) throws XPathException {
        Node parent = targetNode.getParentElement();
        if (parent != null) {
            parent.removeChild(targetNode);
        }
    }

    /**
     * Get the target node of the update action
     * @return the target node, the node to which this update action applies.
     */

    public Node getTargetNode() {
        return targetNode;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
