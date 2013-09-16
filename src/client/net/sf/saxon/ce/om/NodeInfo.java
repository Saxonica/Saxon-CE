package client.net.sf.saxon.ce.om;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;

/**
 * The NodeInfo interface represents a node in Saxon's implementation of the XPath 2.0 data model.
 * <p>
 * Note that several NodeInfo objects may represent the same node. To test node identity, the
 * method {@link #isSameNodeInfo(NodeInfo)} should be used. An exception to this rule applies for
 * document nodes, where the correspondence between document nodes and DocumentInfo objects is one to
 * one. NodeInfo objects are never reused: a given NodeInfo object represents the same node for its entire
 * lifetime.
 * <p>
 * This is the primary interface for accessing trees in Saxon, and it forms part of the public
 * Saxon API. The only subclass of NodeInfo that applications should normally use is {@link DocumentInfo},
 * which represents a document node. Methods that form part of the public API are (since Saxon 8.4)
 * labelled with a JavaDoc "since" tag: classes and methods that have no such label should not be
 * regarded as stable interfaces.
 * <p>
 * The interface represented by this class is at a slightly higher level than the abstraction described
 * in the W3C data model specification, in that it includes support for the XPath axes, rather than exposing
 * the lower-level properties (such as "parent" and "children") directly. All navigation within trees,
 * except for a few convenience methods, is done by following the axes using the iterateAxis method.
 * This allows different implementations of the XPath tree model to implement axis navigation in different ways.
 * Some implementations may choose to use the helper methods provided in class {@link client.net.sf.saxon.ce.tree.util.Navigator}.
 * <p>
 * Note that the stability of this interface applies to classes that use the interface,
 * not to classes that implement it. The interface may be extended in future to add new methods.
 * <p>
 * New implementations of NodeInfo are advised also to implement the methods in interface
 * ExtendedNodeInfo, which will be moved into this interface at some time in the future.
 *
 * @author Michael H. Kay
 * @since 8.4. Extended with three extra methods, previously in ExtendedNodeInfo, in 9.1
 */

public interface NodeInfo extends Item, Sequence {

    /**
     * Get the kind of node. This will be a value such as {@link client.net.sf.saxon.ce.type.Type#ELEMENT}
     * or {@link client.net.sf.saxon.ce.type.Type#ATTRIBUTE}. There are seven kinds of node: documents, elements, attributes,
     * text, comments, processing-instructions, and namespaces.
     *
     * @return an integer identifying the kind of node. These integer values are the
     * same as those used in the DOM
     * @see client.net.sf.saxon.ce.type.Type
     * @since 8.4
     */

    public int getNodeKind();

    /**
     * Determine whether this is the same node as another node.
     * <p>
     * Note that two different NodeInfo instances can represent the same conceptual node.
     * Therefore the "==" operator should not be used to test node identity. The equals()
     * method should give the same result as isSameNodeInfo(), but since this rule was introduced
     * late it might not apply to all implementations.
     * <p>
     * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b).
     * <p>
     * This method has the same semantics as isSameNode() in DOM Level 3, but
     * works on Saxon NodeInfo objects rather than DOM Node objects.
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *      the same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other);

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

    public boolean equals(Object other);

    /**
     * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
     * (represent the same node) then they must have the same hashCode()
     * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
     * should therefore be aware that third party implementations of the NodeInfo interface may
     * not implement the correct semantics.
     */

    public int hashCode();

    /**
     * Get the System ID for the node. Note this is not the
     * same as the base URI: the base URI can be modified by xml:base, but
     * the system ID cannot. The base URI is used primarily for resolving
     * relative URIs within the content of the document. The system ID is
     * used primarily in conjunction with a line number, for identifying the
     * location of elements within the source XML, in particular when errors
     * are found. For a document node, the System ID represents the value of
     * the document-uri property as defined in the XDM data model.
     *
     * @return the System Identifier of the entity in the source document
     * containing the node, or null if not known or not applicable.
     * @since 8.4
     */

    public String getSystemId();

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. This will be the same as the System ID unless xml:base has been used. Where the
     * node does not have a base URI of its own, the base URI of its parent node is returned.
     *
     * @return the base URI of the node. This may be null if the base URI is unknown, including the case
     * where the node has no parent.
     * @since 8.4
     */

    public String getBaseURI();

    /**
     * Determine the relative position of this node and another node, in document order.
     * <p>
     * The other node must always be in the same tree; the effect of calling this method
     * when the two nodes are in different trees is undefined. To obtain a global ordering
     * of nodes, the application should first compare the result of getDocumentNumber(),
     * and only if the document number is the same should compareOrder() be called.
     *
     * @param other The other node, whose position is to be compared with this
     *      node
     * @return -1 if this node precedes the other node, +1 if it follows the
     *     other node, or 0 if they are the same node. (In this case,
     *     isSameNode() will always return true, and the two nodes will
     *     produce the same result for generateId())
     * @since 8.4
     */

    public int compareOrder(NodeInfo other);

    /**
     * Return the string value of the node as defined in the XPath data model.
     * <p>
     * The interpretation of this depends on the type
     * of node. For an element it is the accumulated character content of the element,
     * including descendant elements.
     * <p>
     * This method returns the string value as if the node were untyped. Unlike the string value
     * accessor in the XPath 2.0 data model, it does not report an error if the element has a complex
     * type, instead it returns the concatenation of the descendant text nodes as it would if the element
     * were untyped.
     *
     * @return the string value of the node
     * @since 8.4
     */

    public String getStringValue();

    /**
     * Get the name of the node
     * @return the name of the node, as a StructuredQName. Return null for an unnamed node.
     */

    public StructuredQName getNodeName();

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns "". Unlike the DOM
     * interface, this returns the full name in the case of a non-namespaced name.
     * @since 8.4
     */

    public String getLocalPart();

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node,
     *     or for an element or attribute that is not in a namespace, or for a processing
     *     instruction, returns an empty string.
     * @since 8.4
     */

    public String getURI();

    /**
     * Get the display name of this node, in the form of a lexical QName.
     * For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node. For a node with no name, returns
     *     an empty string.
     * @since 8.4
     */

    public String getDisplayName();

    /**
     * Get the NodeInfo object representing the parent of this node
     *
     * @return the parent of this node; null if this node has no parent
     * @since 8.4
     */

    public NodeInfo getParent();

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     * that match a given NodeTest
     *
     * @exception UnsupportedOperationException if the namespace axis is
     *      requested and this axis is not supported for this implementation.
     * @param axisNumber an integer identifying the axis; one of the constants
     *      defined in class {@link client.net.sf.saxon.ce.om.Axis}
     * @param nodeTest A condition to be satisfied by the returned nodes; nodes
     *      that do not satisfy this condition are not included in the result
     * @return an AxisIterator that delivers the nodes reached by the axis in
     *     turn.  The nodes are returned in axis order (document order for a forwards
     *     axis, reverse document order for a reverse axis).
     * @see client.net.sf.saxon.ce.om.Axis
     * @since 8.4
     */

    public UnfailingIterator iterateAxis(byte axisNumber, NodeTest nodeTest);

    /**
     * Get the root node of the tree containing this node
     *
     * @return the NodeInfo representing the top-level ancestor of this node.
     *     This will not necessarily be a document node. If this node has no parent,
     *     then the method returns this node.
     * @since 8.4
     */

    public NodeInfo getRoot();

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *     node is part of a tree that does not have a document node as its
     *     root, returns null.
     * @since 8.4
     */

    public DocumentInfo getDocumentRoot();

    /**
     * Determine whether the node has any children.
     * <p>
     * Note: the result is equivalent to <br />
     * <code>iterateAxis(Axis.CHILD).next() != null</code>
     *
     * @return True if the node has one or more children
     * @since 8.4
     */

    public boolean hasChildNodes();

    /**
     * Construct a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @param buffer a buffer which will be updated to hold a string
     *     that uniquely identifies this node, across all documents.
     * @since 8.7
     *     <p>Changed in Saxon 8.7 to generate the ID value in a client-supplied buffer</p>
     */

    public void generateId(FastStringBuffer buffer);

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     * @return the document number of the document containing this node
     * @since 8.4
     */

    public int getDocumentNumber();

    /**
     * Get the index position of this node among its siblings (starting from 0)
     * @return 0 for the first child, 1 for the second child, etc.
     */

    public int getSiblingPosition();

    /**
     * Copy this node to a given Receiver.
     * <p>
     * This method is primarily for internal use. It should not be considered a stable
     * part of the Saxon API.
     *
     * @param out the Receiver to which the node should be copied. It is the caller's
     *     responsibility to ensure that this Receiver is open before the method is called
     *     (or that it is self-opening), and that it is closed after use.
     * @param copyOptions a selection of the options defined in {@link CopyOptions}
     * @exception XPathException
     */

    public void copy(Receiver out, int copyOptions) throws XPathException;

    /**
     * Don't copy any namespace nodes.
     */

    public static final int NO_NAMESPACES = 0;

    /**
     * Copy namespaces declared (or undeclared) on this element, but not namespaces inherited from a parent element
     */
    public static final int LOCAL_NAMESPACES = 1;

    /**
     * Copy all in-scope namespaces
     */
    public static final int ALL_NAMESPACES = 2;

    /**
     * Get all namespace declarations and undeclarations defined on this element.
     * <p>
     * This method is intended primarily for internal use. User applications needing
     * information about the namespace context of a node should use <code>iterateAxis(Axis.NAMESPACE)</code>.
     * (However, not all implementations support the namespace axis, whereas all implementations are
     * required to support this method.)
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     * may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     * this element. For a node other than an element, return null. Otherwise, the returned array is a
     * sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     * top half word of each namespace code represents the prefix, the bottom half represents the URI.
     * If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     * The XML namespace is never included in the list. If the supplied array is larger than required,
     * then the first unused entry will be set to -1.
     * <p>
     * For a node other than an element, the method returns null.</p>
     */

    public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer);

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
