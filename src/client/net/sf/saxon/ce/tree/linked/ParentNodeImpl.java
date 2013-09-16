package client.net.sf.saxon.ce.tree.linked;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;

/**
  * ParentNodeImpl is an implementation of a non-leaf node (specifically, an Element node
  * or a Document node)
  * @author Michael H. Kay
  */


abstract class ParentNodeImpl extends NodeImpl {

    private Object children = null;       // null for no children
                                          // a NodeImpl for a single child
                                          // a NodeImpl[] for >1 child

    private int sequence;               // sequence number allocated during original tree creation.
                                          // set to -1 for nodes added subsequently by XQuery update

    /**
     * Get the node sequence number (in document order). Sequence numbers are monotonic but not
     * consecutive. In the current implementation, parent nodes (elements and document nodes) have a zero
     * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
     * the top word the same as their owner and the bottom half reflecting their relative position.
     * For nodes added by XQUery Update, the sequence number is -1L
     * @return the sequence number if there is one, or -1L otherwise.
    */

    protected final int[] getSequenceNumber() {
        return new int[]{getRawSequenceNumber(), 0};
    }

    protected final int getRawSequenceNumber() {
        return sequence;
    }

    protected final void setRawSequenceNumber(int seq) {
        sequence = seq;
    }

    /**
     * Set the children of this node
     * @param children null if there are no children, a single NodeInfo if there is one child, an array of NodeInfo
     * if there are multiple children
     */

    protected final void setChildren(Object children) {
        this.children = children;
    }

    /**
    * Determine if the node has any children.
    */

    public final boolean hasChildNodes() {
        return (children!=null);
    }

    private final static NodeImpl[] EMPTY_NODE_LIST = new NodeImpl[0];

    /**
     * Get all children of this node, as an array
     * @return an array containing all the children
     */

    public final NodeImpl[] allChildren() {
        if (children==null) {
            return EMPTY_NODE_LIST;
        } else if (children instanceof NodeImpl) {
            return new NodeImpl[]{(NodeImpl)children};
        } else {
            return (NodeImpl[])children;
        }
    }


    /**
    * Get the first child node of the element
    * @return the first child node of the required type, or null if there are no children
    */

    public final NodeInfo getFirstChild() {
        if (children==null) return null;
        if (children instanceof NodeImpl) return (NodeImpl)children;
        return ((NodeImpl[])children)[0];
    }

    /**
    * Get the last child node of the element
    * @return the last child of the element, or null if there are no children
    */

    public final NodeInfo getLastChild() {
        if (children==null) return null;
        if (children instanceof NodeImpl) return (NodeImpl)children;
        NodeImpl[] n = (NodeImpl[])children;
        return n[n.length-1];
    }

    /**
     * Get the nth child node of the element (numbering from 0)
     * @param n identifies the required child
     * @return the last child of the element, or null if there is no n'th child
    */

    protected final NodeImpl getNthChild(int n) {
        if (children==null) return null;
        if (children instanceof NodeImpl) {
            return (n==0 ? (NodeImpl)children : null);
        }
        NodeImpl[] nodes = (NodeImpl[])children;
        if (n<0 || n>=nodes.length) return null;
        return nodes[n];
    }


    /**
    * Return the string-value of the node, that is, the concatenation
    * of the character content of all descendent elements and text nodes.
    * @return the accumulated character content of the element, including descendant elements.
    */

    public String getStringValue()  {
        FastStringBuffer sb = null;

        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            if (next instanceof TextImpl) {
                if (sb==null) {
                    sb = new FastStringBuffer(FastStringBuffer.SMALL);
                }
                sb.append(next.getStringValue());
            }
            next = next.getNextInDocument(this);
        }
        if (sb==null) return "";
        return sb.toString();
    }

    /**
     * Add a child node to this node. For system use only. Note: normalizing adjacent text nodes
     * is the responsibility of the caller.
     * @param node the node to be added as a child of this node. This must be an instance of
     * {@link client.net.sf.saxon.ce.tree.linked.NodeImpl}. It will be modified as a result of this call (by setting its
     * parent property and sibling position)
     * @param index the position where the child is to be added
    */

    protected synchronized void addChild(NodeImpl node, int index) {
        NodeImpl[] c;
        if (children == null) {
            c = new NodeImpl[10];
        } else if (children instanceof NodeImpl) {
            c = new NodeImpl[10];
            c[0] = (NodeImpl)children;
        } else {
            c = (NodeImpl[])children;
        }
        if (index >= c.length) {
            NodeImpl[] kids = new NodeImpl[c.length * 2];
            System.arraycopy(c, 0, kids, 0, c.length);
            c = kids;
        }
        c[index] = node;
        node.setRawParent(this);
        node.setSiblingPosition(index);
        children = c;
    }

    /**
     * Compact the space used by this node
     * @param size the number of actual children
     */

    public synchronized void compact(int size) {
        if (size==0) {
            children = null;
        } else if (size==1) {
            if (children instanceof NodeImpl[]) {
                children = ((NodeImpl[])children)[0];
            }
        } else {
            NodeImpl[] kids = new NodeImpl[size];
            System.arraycopy(children, 0, kids, 0, size);
            children = kids;
        }
    }



}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
