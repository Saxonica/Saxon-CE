package client.net.sf.saxon.ce.tree.linked;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.event.Builder;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.tree.NamespaceNode;
import client.net.sf.saxon.ce.tree.iter.*;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AbstractNode;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.UntypedAtomicValue;


/**
 * A node in the "linked" tree representing any kind of node except a namespace node.
 * Specific node kinds are represented by concrete subclasses.
 *
 * @author Michael H. Kay
 */

public abstract class NodeImpl extends AbstractNode implements NodeInfo {

    private ParentNodeImpl parent;
    private int index;
    /**
     * Chararacteristic letters to identify each type of node, indexed using the node type
     * values. These are used as the initial letter of the result of generate-id()
     */

    public static final char[] NODE_LETTER =
            {'x', 'e', 'a', 't', 'x', 'x', 'x', 'p', 'c', 'r', 'x', 'x', 'x', 'n'};

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return getPhysicalRoot().getDocumentNumber();
    }


    /**
     * Get the index position of this node among its siblings (starting from 0)
     * @return 0 for the first child, 1 for the second child, etc. Returns -1 for a node
     * that has been deleted.
     */
    public final int getSiblingPosition() {
        return index;
    }

    /**
     * Set the index position. For internal use only
     * @param index the position of the node among its siblings, counting from zero.
     */

    protected final void setSiblingPosition(int index) {
        this.index = index;
    }

    /**
     * Get the typed value of this node.
     * If there is no type annotation, we return the string value, as an instance
     * of xs:untypedAtomic
     */

    public AtomicValue getTypedValue() {
        return new UntypedAtomicValue(getStringValue());
    }

     /**
     * Set the system ID of this node. This method is provided so that a NodeInfo
     * implements the javax.xml.transform.Source interface, allowing a node to be
     * used directly as the Source of a transformation
     */

    public void setSystemId(String uri) {
        // overridden in DocumentImpl and ElementImpl
        ((NodeImpl)getParent()).setSystemId(uri);
    }

    /**
     * Determine whether this is the same node as another node
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        // default implementation: differs for attribute and namespace nodes
        return this == other;
    }

   /**
      * The equals() method compares nodes for identity. It is defined to give the same result
      * as isSameNodeInfo().
      * @param other the node to be compared with this node
      * @return true if this NodeInfo object and the supplied NodeInfo object represent
      *      the same node in the tree.
      * @since 8.7 Previously, the effect of the equals() method was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics. It is safer to use isSameNodeInfo() for this reason.
      * The equals() method has been defined because it is useful in contexts such as a Java Set or HashMap.
      */

   public boolean equals(Object other) {
       return other instanceof NodeInfo && isSameNodeInfo((NodeInfo)other);
   }

    /**
     * Get the name of the node
     *
     * @return the name of the node, as a StructuredQName. Return null for an unnamed node.
     */
    public StructuredQName getNodeName() {
        return null; // default implementation for unnamed nodes
    }

    /**
     * Get a character string that uniquely identifies this node within this document
     * (The calling code will prepend a document identifier)
     */

    public void generateId(FastStringBuffer buffer) {
        parent.generateId(buffer);
        buffer.append(NODE_LETTER[getNodeKind()]);
        buffer.append(Integer.toString(index));
    }

    /**
     * Get the system ID for the node. Default implementation for child nodes.
     */

    public String getSystemId() {
        return parent.getSystemId();
    }

    /**
     * Get the base URI for the node. Default implementation for child nodes.
     */

    public String getBaseURI() {
        return parent.getBaseURI();
    }

    /**
     * Get the node sequence number (in document order). Sequence numbers are monotonic but not
     * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
     * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
     * the top word the same as their owner and the bottom half reflecting their relative position.
     * This is the default implementation for child nodes.
     * For nodes added by XQuery Update, the sequence number is -1L
     * @return the sequence number if there is one as an array containing two integers
     */

    protected int[] getSequenceNumber() {
        NodeImpl prev = this;
        for (int i = 0; ; i++) {
            if (prev instanceof ParentNodeImpl) {
                int[] prevseq = prev.getSequenceNumber();
                return new int[]{prevseq[0], prevseq[1] + 0x10000 + i};
                // note the 0x10000 is to leave room for namespace and attribute nodes.
            }
            prev = prev.getPreviousInDocument();
        }

    }

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this node
     * @return -1 if this node precedes the other node, +1 if it follows the other
     *         node, or 0 if they are the same node. (In this case, isSameNode() will always
     *         return true, and the two nodes will produce the same result for generateId())
     */

    public final int compareOrder(NodeInfo other) {
        if (other instanceof NamespaceNode) {
            return 0 - other.compareOrder(this);
        }
        int[] a = getSequenceNumber();
        int[] b = ((NodeImpl)other).getSequenceNumber();
        if (a[0] < b[0]) {
            return -1;
        }
        if (a[0] > b[0]) {
            return +1;
        }
        if (a[1] < b[1]) {
            return -1;
        }
        if (a[1] > b[1]) {
            return +1;
        }
        return 0;
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return getPhysicalRoot().getConfiguration();
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For the null namespace, return an
     *         empty string. For an unnamed node, return the empty string.
     */

    public String getURI() {
        StructuredQName qName = getNodeName();
        return (qName == null ? "" : qName.getNamespaceURI());
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node.
     *         For a node with no name, return an empty string.
     */

    public String getDisplayName() {
        StructuredQName qName = getNodeName();
        return (qName == null ? "" : qName.getDisplayName());
    }

    /**
     * Get the local name of this node.
     *
     * @return The local name of this node.
     *         For a node with no name, return "",.
     */

    public String getLocalPart() {
        StructuredQName qName = getNodeName();
        return (qName == null ? "" : qName.getLocalName());
    }

    /**
     * Find the parent node of this node.
     *
     * @return The Node object describing the containing element or root node.
     */

    public final NodeInfo getParent() {
        if (parent instanceof DocumentImpl && ((DocumentImpl)parent).isImaginary()) {
            return null;
        }
        return parent;
    }

    /**
     * Get the raw value of the parent pointer. This will usually be the same as the parent node
     * in the XDM model, but in the case of a parentless element it will be a pointer to the "imaginary"
     * document node which is not properly part of the tree.
     */

    protected final ParentNodeImpl getRawParent() {
        return parent;
    }

    /**
     * Set the raw parent pointer
     */

    protected final void setRawParent(ParentNodeImpl parent) {
        this.parent = parent;
    }

    /**
     * Get the previous sibling of the node
     *
     * @return The previous sibling node. Returns null if the current node is the first
     *         child of its parent.
     */

    public NodeInfo getPreviousSibling() {
        if (parent == null) {
            return null;
        }
        return parent.getNthChild(index - 1);
    }


    /**
     * Get next sibling node
     *
     * @return The next sibling node of the required type. Returns null if the current node is the last
     *         child of its parent.
     */

    public NodeInfo getNextSibling() {
        if (parent == null) {
            return null;
        }
        return parent.getNthChild(index + 1);
    }

    /**
     * Get first child - default implementation used for leaf nodes
     *
     * @return null
     */

    public NodeInfo getFirstChild() {
        return null;
    }

    /**
     * Get last child - default implementation used for leaf nodes
     *
     * @return null
     */

    public NodeInfo getLastChild() {
        return null;
    }

    /**
     * Return an enumeration over the nodes reached by the given axis from this node
     *
     * @param axisNumber The axis to be iterated over
     * @param nodeTest   A pattern to be matched by the returned nodes
     * @return an AxisIterator that scans the nodes reached by the axis in turn.
     */

    public UnfailingIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {

        switch (axisNumber) {
            case Axis.ANCESTOR:
                return new SteppingIterator(this, new Navigator.ParentFunction(nodeTest), false);

            case Axis.ANCESTOR_OR_SELF:
                return new SteppingIterator(this, new Navigator.ParentFunction(nodeTest), true);

            case Axis.ATTRIBUTE:
                if (getNodeKind() != Type.ELEMENT) {
                    return EmptyIterator.getInstance();
                }
                AttributeCollection atts = ((ElementImpl)this).getAttributeList();
                if (nodeTest instanceof NameTest) {
                    int index = atts.findByStructuredQName(nodeTest.getRequiredNodeName());
                    if (index < 0) {
                        return EmptyIterator.getInstance();
                    } else {
                        AttributeImpl a = new AttributeImpl(((ElementImpl)this), index);
                        return SingletonIterator.makeIterator(a);
                    }
                } else {
                    AttributeImpl[] nodes = new AttributeImpl[atts.getLength()];
                    for (int i=0; i<atts.getLength(); i++) {
                        nodes[i] = new AttributeImpl(((ElementImpl)this), i);
                    }
                    return Navigator.newAxisFilter(new ArrayIterator(nodes), nodeTest);
                }

            case Axis.CHILD:
                if (this instanceof ParentNodeImpl) {
                    UnfailingIterator all = new ArrayIterator(((ParentNodeImpl)this).allChildren());
                    if (nodeTest == AnyNodeTest.getInstance()) {
                        return all;
                    } else {
                        return Navigator.newAxisFilter(all, nodeTest);
                    }
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT:
                if (getNodeKind() == Type.DOCUMENT &&
                        nodeTest instanceof NameTest &&
                        nodeTest.getRequiredNodeKind() == Type.ELEMENT) {
                    return ((DocumentImpl)this).getAllElements(nodeTest.getRequiredNodeName());
                } else if (hasChildNodes()) {
                    return new SteppingIterator(this, new NextDescendantFunction(this, nodeTest), false);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT_OR_SELF:
                return new SteppingIterator(this, new NextDescendantFunction(this, nodeTest), true);

            case Axis.FOLLOWING:
                return Navigator.newAxisFilter(new Navigator.FollowingEnumeration(this), nodeTest);

            case Axis.FOLLOWING_SIBLING:
                return new SteppingIterator(this, new NextSiblingFunction(nodeTest), false);

            case Axis.NAMESPACE:
                if (getNodeKind() != Type.ELEMENT) {
                    return EmptyIterator.getInstance();
                }
                return NamespaceNode.makeIterator(this, nodeTest);

            case Axis.PARENT:
                NodeInfo parent = getParent();
                if (parent == null) {
                    return EmptyIterator.getInstance();
                }
                return Navigator.filteredSingleton(parent, nodeTest);

            case Axis.PRECEDING:
                return Navigator.newAxisFilter(new Navigator.PrecedingEnumeration(this, false), nodeTest);

            case Axis.PRECEDING_SIBLING:
                return new SteppingIterator(this, new PrecedingSiblingFunction(nodeTest), false);

            case Axis.SELF:
                return Navigator.filteredSingleton(this, nodeTest);

            default:
                throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
     * Get the root node
     * @return the NodeInfo representing the logical root of the tree. For this tree implementation the
     * root will either be a document node or an element node.
     */

    public NodeInfo getRoot() {
        NodeInfo parent = getParent();
        if (parent == null) {
            return this;
        } else {
            return parent.getRoot();
        }
    }

    /**
     * Get the root (document) node
     * @return the DocumentInfo representing the containing document. If this
     *     node is part of a tree that does not have a document node as its
     *     root, returns null.
     */

    public DocumentInfo getDocumentRoot() {
        NodeInfo parent = getParent();
        if (parent == null) {
            return null;
        } else {
            return parent.getDocumentRoot();
        }
    }

    /**
     * Get the physical root of the tree. This may be an imaginary document node: this method
     * should be used only when control information held at the physical root is required
     * @return the document node, which may be imaginary. In the case of a node that has been detached
     * from the tree by means of a delete() operation, this method returns null.
     */

    public DocumentImpl getPhysicalRoot() {
        ParentNodeImpl up = parent;
        while (up != null && !(up instanceof DocumentImpl)) {
            up = up.getRawParent();
        }
        return (DocumentImpl)up;
    }

    /**
     * Get the next node in document order
     *
     * @param anchor the scan stops when it reaches a node that is not a descendant of the specified
     *               anchor node
     * @return the next node in the document, or null if there is no such node
     */

    public NodeImpl getNextInDocument(NodeImpl anchor) {
        // find the first child node if there is one; otherwise the next sibling node
        // if there is one; otherwise the next sibling of the parent, grandparent, etc, up to the anchor element.
        // If this yields no result, return null.

        NodeImpl next = (NodeImpl)getFirstChild();
        if (next != null) {
            return next;
        }
        if (this == anchor) {
            return null;
        }
        next = (NodeImpl)getNextSibling();
        if (next != null) {
            return next;
        }
        NodeImpl parent = this;
        while (true) {
            parent = (NodeImpl)parent.getParent();
            if (parent == null) {
                return null;
            }
            if (parent == anchor) {
                return null;
            }
            next = (NodeImpl)parent.getNextSibling();
            if (next != null) {
                return next;
            }
        }
    }


    /**
     * Get the previous node in document order
     *
     * @return the previous node in the document, or null if there is no such node
     */

    public NodeImpl getPreviousInDocument() {

        // finds the last child of the previous sibling if there is one;
        // otherwise the previous sibling element if there is one;
        // otherwise the parent, up to the anchor element.
        // If this reaches the document root, return null.

        NodeImpl prev = (NodeImpl)getPreviousSibling();
        if (prev != null) {
            return prev.getLastDescendantOrSelf();
        }
        return (NodeImpl)getParent();
    }

    private NodeImpl getLastDescendantOrSelf() {
        NodeImpl last = (NodeImpl)getLastChild();
        if (last == null) {
            return this;
        }
        return last.getLastDescendantOrSelf();
    }

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer) {
        return null;
    }

    // implement DOM Node methods

    /**
     * Determine whether the node has any children.
     *
     * @return <code>true</code> if the node has any children,
     *         <code>false</code> if the node has no children.
     */

    public boolean hasChildNodes() {
        return getFirstChild() != null;
    }


    /**
     * Get a Builder suitable for building nodes that can be attached to this document.
     * @return a new Builder that constructs nodes using the same object model implementation
     * as this one, suitable for attachment to this tree
     */    

    public Builder newBuilder() {
        return getPhysicalRoot().newBuilder();
    }


    private static class NextDescendantFunction implements SteppingIterator.SteppingFunction {
        private NodeImpl anchor;
        private NodeTest predicate;
        public NextDescendantFunction(NodeImpl anchor, NodeTest predicate) {
            this.anchor = anchor;
            this.predicate = predicate;
        }
        public Item step(Item current) {
            return ((NodeImpl)current).getNextInDocument(anchor);
        }

        public boolean conforms(Item current) {
            return predicate.matches((NodeInfo)current);
        }
    }

    private static class PrecedingSiblingFunction implements SteppingIterator.SteppingFunction {
        private NodeTest predicate;
        public PrecedingSiblingFunction(NodeTest predicate) {
            this.predicate = predicate;
        }
        public Item step(Item current) {
            return ((NodeImpl)current).getPreviousSibling();
        }

        public boolean conforms(Item current) {
            return predicate.matches((NodeInfo)current);
        }
    }

    private static class NextSiblingFunction implements SteppingIterator.SteppingFunction {
        private NodeTest predicate;
        public NextSiblingFunction(NodeTest predicate) {
            this.predicate = predicate;
        }
        public Item step(Item current) {
            return ((NodeImpl)current).getNextSibling();
        }

        public boolean conforms(Item current) {
            return predicate.matches((NodeInfo)current);
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
