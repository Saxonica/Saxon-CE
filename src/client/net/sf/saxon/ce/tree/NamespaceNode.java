package client.net.sf.saxon.ce.tree;

import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.event.ReceiverOptions;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.*;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.NamespaceIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AbstractNode;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NamespaceNode extends AbstractNode implements NodeInfo {

    NodeInfo element;
    NamespaceBinding nsBinding;
    int position;
    StructuredQName qName;

    /**
     * Create a namespace node
     *
     * @param element  the parent element of the namespace node
     * @param nscode   the namespace code, representing the prefix and URI of the namespace binding
     * @param position maintains document order among namespace nodes for the same element
     */

    public NamespaceNode(NodeInfo element, NamespaceBinding nscode, int position) {
        this.element = element;
        this.nsBinding = nscode;
        this.position = position;
        qName = null;  // evaluated lazily
    }

    /**
     * Get the kind of node. This will be a value such as Type.ELEMENT or Type.ATTRIBUTE
     *
     * @return an integer identifying the kind of node. These integer values are the
     *         same as those used in the DOM
     */

    public int getNodeKind() {
        return Type.NAMESPACE;
    }

    /**
     * Determine whether this is the same node as another node.
     * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b).
     * This method has the same semantics as isSameNode() in DOM Level 3, but
     * works on Saxon NodeInfo objects rather than DOM Node objects.
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *         the same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        return other instanceof NamespaceNode &&
                element.isSameNodeInfo(((NamespaceNode) other).element) &&
                nsBinding == ((NamespaceNode) other).nsBinding;

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
        return element.hashCode() ^ (position << 13);
    }

    /**
     * Get the System ID for the node.
     *
     * @return the System Identifier of the entity in the source document
     *         containing the node, or null if not known. Note this is not the
     *         same as the base URI: the base URI can be modified by xml:base, but
     *         the system ID cannot.
     */

    public String getSystemId() {
        return element.getSystemId();
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. This will be the same as the System ID unless xml:base has been used.
     *
     * @return the base URI of the node
     */

    public String getBaseURI() {
        return null;    // the base URI of a namespace node is the empty sequence
    }

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this
     *              node
     * @return -1 if this node precedes the other node, +1 if it follows the
     *         other node, or 0 if they are the same node. (In this case,
     *         isSameNode() will always return true, and the two nodes will
     *         produce the same result for generateId())
     */

    public int compareOrder(NodeInfo other) {
        if (other instanceof NamespaceNode && element.isSameNodeInfo(((NamespaceNode) other).element)) {
            // alternative: return Integer.signum(position - ((NamespaceNodeI)other).position);
            int c = position - ((NamespaceNode) other).position;
            if (c == 0) {
                return 0;
            }
            if (c < 0) {
                return -1;
            }
            return +1;
        } else if (element.isSameNodeInfo(other)) {
            return +1;
        } else {
            return element.compareOrder(other);
        }
    }

    /**
     * Get the index position of this node among its siblings (starting from 0)
     *
     * @return 0 for the first child, 1 for the second child, etc.
     */
    public int getSiblingPosition() {
        return 1;
    }

    /**
     * Return the string value of the node. The interpretation of this depends on the type
     * of node. For a namespace node, it is the namespace URI.
     *
     * @return the string value of the node
     */

    public String getStringValue() {
        return nsBinding.getURI();
    }


    public StructuredQName getNodeName() {
        if (qName == null) {
            if (nsBinding.getPrefix().isEmpty()) {
                return null;
            } else {
                qName = new StructuredQName("", "", nsBinding.getPrefix());
            }
        }
        return qName;
    }

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns "". Unlike the DOM
     *         interface, this returns the full name in the case of a non-namespaced name.
     */

    public String getLocalPart() {
        return nsBinding.getPrefix();
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. Since the name of a namespace
     *         node is always an NCName (the namespace prefix), this method always returns "".
     */

    public String getURI() {
        return "";
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node. For a node with no name, return
     *         an empty string.
     */

    public String getDisplayName() {
        return getLocalPart();
    }

    /**
     * Get the NodeInfo object representing the parent of this node
     *
     * @return the parent of this node; null if this node has no parent
     */

    public NodeInfo getParent() {
        return element;
    }

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     * that match a given NodeTest
     *
     * @param axisNumber an integer identifying the axis; one of the constants
     *                   defined in class net.sf.saxon.om.Axis
     * @param nodeTest   A pattern to be matched by the returned nodes; nodes
     *                   that do not match this pattern are not included in the result
     * @return a NodeEnumeration that scans the nodes reached by the axis in
     *         turn.
     * @throws UnsupportedOperationException if the namespace axis is
     *                                       requested and this axis is not supported for this implementation.
     * @see client.net.sf.saxon.ce.om.Axis
     */

    public UnfailingIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        switch (axisNumber) {
            case Axis.ANCESTOR:
                return element.iterateAxis(Axis.ANCESTOR_OR_SELF, nodeTest);

            case Axis.ANCESTOR_OR_SELF:
                if (nodeTest.matches(this)) {
                    return new PrependIterator(this, element.iterateAxis(Axis.ANCESTOR_OR_SELF, nodeTest));
                } else {
                    return element.iterateAxis(Axis.ANCESTOR_OR_SELF, nodeTest);
                }

            case Axis.ATTRIBUTE:
            case Axis.CHILD:
            case Axis.DESCENDANT:
            case Axis.DESCENDANT_OR_SELF:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PRECEDING_SIBLING:
                return EmptyIterator.getInstance();

            case Axis.FOLLOWING:
                return Navigator.newAxisFilter(
                        new Navigator.FollowingEnumeration(this),
                        nodeTest);

            case Axis.PARENT:
                return Navigator.filteredSingleton(element, nodeTest);

            case Axis.PRECEDING:
                return Navigator.newAxisFilter(
                        new Navigator.PrecedingEnumeration(this, false),
                        nodeTest);

            case Axis.SELF:
                return Navigator.filteredSingleton(this, nodeTest);

            default:
                throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
     * Get the root node of the tree containing this node
     *
     * @return the NodeInfo representing the top-level ancestor of this node.
     *         This will not necessarily be a document node
     */

    public NodeInfo getRoot() {
        return element.getRoot();
    }

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *         node is part of a tree that does not have a document node as its
     *         root, return null.
     */

    public DocumentInfo getDocumentRoot() {
        return element.getDocumentRoot();
    }

    /**
     * Determine whether the node has any children. <br />
     * Note: the result is equivalent to <br />
     * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
     *
     * @return True if the node has one or more children
     */

    public boolean hasChildNodes() {
        return false;
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @param buffer buffer to hold a string that uniquely identifies this node, across all
     *               documents.
     */

    public void generateId(FastStringBuffer buffer) {
        element.generateId(buffer);
        buffer.append("n");
        buffer.append(Integer.toString(position));
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return element.getDocumentNumber();
    }

    /**
     * Copy this node to a given outputter
     *
     * @param out         the Receiver to which the node should be copied
     * @param copyOptions a selection of the options defined
     */

    public void copy(Receiver out, int copyOptions) throws XPathException {
        out.namespace(nsBinding, ReceiverOptions.REJECT_DUPLICATES);
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

    /**
     * Get the typed value of the item
     *
     * @return the typed value of the item. In general this will be a sequence
     */

    public AtomicValue getTypedValue() {
        return new StringValue(getStringValue());
    }

    /**
     * Factory method to create an iterator over the in-scope namespace nodes of an element
     *
     * @param element the node whose namespaces are required
     * @param test    used to filter the returned nodes
     * @return an iterator over the namespace nodes that satisfy the test
     */

    public static UnfailingIterator makeIterator(final NodeInfo element, NodeTest test) {
        List<NamespaceNode> nodes = new ArrayList<NamespaceNode>();
        Iterator<NamespaceBinding> bindings = NamespaceIterator.iterateNamespaces(element);
        int position = 0;
        while (bindings.hasNext()) {
            NamespaceNode node = new NamespaceNode(element, bindings.next(), position++);
            if (test.matches(node)) {
                nodes.add(node);
            }
        }
        NamespaceNode node = new NamespaceNode(element, NamespaceBinding.XML, position);
        if (test.matches(node)) {
            nodes.add(node);
        }
        return new ListIterator(nodes);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
