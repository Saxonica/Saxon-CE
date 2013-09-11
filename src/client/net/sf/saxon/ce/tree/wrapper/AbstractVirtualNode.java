package client.net.sf.saxon.ce.tree.wrapper;

import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.value.AtomicValue;


/**
 * AbstractVirtualNode is an abstract superclass for VirtualNode implementations in which
 * the underlying node is itself a Saxon NodeInfo.
 */

public abstract class AbstractVirtualNode implements VirtualNode {

    protected NodeInfo node;
    protected AbstractVirtualNode parent;     // null means unknown
    protected AbstractVirtualNode docWrapper;

    /**
     * Get the underlying node, to implement the VirtualNode interface
     */

    public NodeInfo getUnderlyingNode() {
        return node;
    }

    /**
     * Return the type of node.
     *
     * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
     */

    public int getNodeKind() {
        return node.getNodeKind();
    }

    /**
     * Get the typed value of the item
     */

    public AtomicValue getTypedValue() {
        return node.getTypedValue();
    }

    /**
     * Determine whether this is the same node as another node. <br />
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (other instanceof AbstractVirtualNode) {
            return node.isSameNodeInfo(((AbstractVirtualNode) other).node);
        } else {
            return node.isSameNodeInfo(other);
        }
    }

    /**
     * The equals() method compares nodes for identity. It is defined to give the same result
     * as isSameNodeInfo().
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *         the same node in the tree.
     * @since 8.7 Previously, the effect of the equals() method was not defined. Callers
     *        should therefore be aware that third party implementations of the NodeInfo interface may
     *        not implement the correct semantics. It is safer to use isSameNodeInfo() for this reason.
     *        The equals() method has been defined because it is useful in contexts such as a Java Set or HashMap.
     */

    public boolean equals(Object other) {
        return other instanceof NodeInfo && isSameNodeInfo((NodeInfo) other);
    }

    /**
     * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
     * (represent the same node) then they must have the same hashCode()
     *
     * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
     *        should therefore be aware that third party implementations of the NodeInfo interface may
     *        not implement the correct semantics.
     */

    public int hashCode() {
        return node.hashCode() ^ 0x3c3c3c3c;
    }

    /**
     * Get the System ID for the node.
     *
     * @return the System Identifier of the entity in the source document containing the node,
     *         or null if not known. Note this is not the same as the base URI: the base URI can be
     *         modified by xml:base, but the system ID cannot.
     */

    public String getSystemId() {
        return node.getSystemId();
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. In the JDOM model, base URIs are held only an the document level. We don't
     * currently take any account of xml:base attributes.
     */

    public String getBaseURI() {
        return node.getBaseURI();
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

    public int compareOrder(NodeInfo other) {
        if (other instanceof AbstractVirtualNode) {
            return node.compareOrder(((AbstractVirtualNode) other).node);
        } else {
            return node.compareOrder(other);
        }
    }

    /**
     * Return the string value of the node. The interpretation of this depends on the type
     * of node. For an element it is the accumulated character content of the element,
     * including descendant elements.
     *
     * @return the string value of the node
     */

    public final String getStringValue() {
        // default implementation returns the string value of the base node
        return node.getStringValue();
    }

    /**
     * Get node name as an expanded QName
     */

    public StructuredQName getNodeName() {
        return node.getNodeName();
    }

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns null, except for
     *         un unnamed namespace node, which returns "".
     */

    public String getLocalPart() {
        return node.getLocalPart();
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node, return null.
     *         For a node with an empty prefix, return an empty string.
     */

    public String getURI() {
        return node.getURI();
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node.
     *         For a node with no name, return an empty string.
     */

    public String getDisplayName() {
        return node.getDisplayName();
    }

    /**
     * Return an iteration over the nodes reached by the given axis from this node
     *
     * @param axisNumber the axis to be used
     * @param nodeTest   A pattern to be matched by the returned nodes
     * @return a SequenceIterator that scans the nodes reached by the axis in turn.
     */

    public UnfailingIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        return new Navigator.AxisFilter(iterateAxis(axisNumber), nodeTest);
    }

    /**
     * Get the root node - always a document node with this tree implementation
     *
     * @return the NodeInfo representing the containing document
     */

    public NodeInfo getRoot() {
        return docWrapper;
    }

    /**
     * Get the root (document) node
     *
     * @return the DocumentInfo representing the containing document
     */

    public DocumentInfo getDocumentRoot() {
        return (DocumentInfo) docWrapper;
    }

    /**
     * Determine whether the node has any children. <br />
     * Note: the result is equivalent to <br />
     * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
     */

    public boolean hasChildNodes() {
        return node.hasChildNodes();
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @param buffer a buffer, into which will be placed
     *               a string that uniquely identifies this node, within this
     *               document. The calling code prepends information to make the result
     *               unique across all documents.
     */

    public void generateId(FastStringBuffer buffer) {
        // Note: giving the node the same ID as its underlying node is slightly questionable; depends on usage
        node.generateId(buffer);
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return docWrapper.getDocumentNumber();
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
        return node.getDeclaredNamespaces(buffer);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.