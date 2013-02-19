package client.net.sf.saxon.ce.trans.update;

import client.net.sf.saxon.ce.expr.XPathContext;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

/**
 * A pending update action representing the effect of an insert expression
 */
public class InsertAction extends PendingUpdateAction {

    private Node content;
    private Node targetNode;
    private int position;

    public static final int FIRST = 0;
    public static final int LAST = 1;
    public static final int BEFORE = 2;
    public static final int AFTER = 3;

    /**
     * Create an InsertAction
     * @param content an HTML document node whose children represent the content sequence to be inserted
     * @param targetNode the node that defines where the new nodes will be inserted
     * @param position defines where the nodes will be inserted: before or after the target node, or as the first
     * or last child of the target node.
     */

    public InsertAction(Node content, Node targetNode, int position) {
        this.content = content;
        this.targetNode = targetNode;
        this.position = position;
    }

    /**
     * Apply the pending update action to the affected nodes
     *
     * @param context the XPath evaluation context
     */

    public void apply(XPathContext context) {
        switch (position) {
            case FIRST: {
                NodeList list = content.getChildNodes();
                int count = list.getLength();
                for (int i=count-1; i>=0; i--) {
                    targetNode.insertFirst(list.getItem(i));
                }
                break;
            }
            case LAST:{
                while (content.hasChildNodes()) {
                    targetNode.appendChild(content.getFirstChild());
                }
                break;
            }
            case BEFORE: {
                Node refNode = targetNode.getChild(position);
                NodeList list = content.getChildNodes();
                int count = list.getLength();
                for (int i=0; i < count; i++) {
                    targetNode.insertBefore(list.getItem(i), refNode);
                }
                break;
            }
            case AFTER: {
                Node refNode = targetNode.getChild(position);
                NodeList list = content.getChildNodes();
                int count = list.getLength();
                for (int i=count-1; i>=0; i--) {
                    targetNode.insertAfter(list.getItem(i), refNode);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown insert position " + position);
        }
    }

    /**
     * Get the target node of the update action
     * @return the target node, the node to which this update action applies. Returns null in the
     *         case of a delete action, which affects multiple nodes.
     */

    public Node getTargetNode() {
        return targetNode;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
