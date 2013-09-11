package client.net.sf.saxon.ce.tree.util;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.UntypedAtomicValue;

/**
 * A node (implementing the NodeInfo interface) representing an attribute, text node,
 * comment, processing instruction, or namespace that has no parent (and of course no children).
 * Exceptionally it is also used (during whitespace stripping) to represent a standalone element.
 *
 * <p>In general this class does not impose constraints defined in the data model: that is the responsibility
 * of the client. For example, the class does not prevent you from creating a comment or text node that has
 * a name or a non-trivial type annotation.</p>
 * 
 * @author Michael H. Kay
 */

public final class Orphan implements NodeInfo {

    private int kind;
    private StructuredQName qName = null;
    private CharSequence stringValue;
    private String systemId;

    /**
     * Create an Orphan node
     */

    public Orphan() {
    }

    /**
     * Set the node kind
     * @param kind the kind of node, for example {@link Type#ELEMENT} or {@link Type#ATTRIBUTE}
     */

    public void setNodeKind(int kind) {
        this.kind = kind;
    }

    /**
     * Set the name of the node
     * @param nameCode the the name of the node
     */

    public void setNodeName(StructuredQName nameCode) {
        this.qName = nameCode;
    }

    /**
     * Set the string value of the node
     * @param stringValue the string value of the node
     */

    public void setStringValue(CharSequence stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Set the base URI of the node
     * @param systemId the base URI of the node
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Return the kind of node.
    * @return one of the values Type.ELEMENT, Type.TEXT, Type.ATTRIBUTE, etc.
    */

    public int getNodeKind() {
        return kind;
    }

    /**
     * Get the typed value of the node
     * @return an iterator over the items making up the typed value
    */

    public AtomicValue getTypedValue() {
        switch (getNodeKind()) {
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return new StringValue(stringValue);
            default:
                 return new UntypedAtomicValue(stringValue);
        }
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        return this==other;
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
      * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
      * (represent the same node) then they must have the same hashCode()
      * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics.
      */

     public int hashCode() {
         return super.hashCode();
     }


    /**
    * Get the System ID for the node.
    * @return the System Identifier of the entity in the source document containing the node,
    * or null if not known. Note this is not the same as the base URI: the base URI can be
    * modified by xml:base, but the system ID cannot.
    */

    public String getSystemId() {
        return systemId;
    }

    /**
    * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
    * in the node. This will be the same as the System ID unless xml:base has been used.
    */

    public String getBaseURI() {
        if (kind == Type.PROCESSING_INSTRUCTION) {
            return systemId;
        } else {
            return null;
        }
    }

    /**
    * Determine the relative position of this node and another node, in document order.
    * The other node will always be in the same document.
    * @param other The other node, whose position is to be compared with this node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
    */

    public int compareOrder(NodeInfo other) {

        // are they the same node?
        if (this.isSameNodeInfo(other)) {
            return 0;
        }
        return (this.hashCode() < other.hashCode() ? -1 : +1);
    }

    /**
    * Return the string value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
        return stringValue.toString();
    }

    /**
     * Get the name of the node
     *
     * @return the name of the node, as a StructuredQName. Return null for an unnamed node.
     */
    public StructuredQName getNodeName() {
        return qName;
    }

    /**
    * Get the local part of the name of this node. This is the name after the ":" if any.
    * @return the local part of the name. For an unnamed node, returns "".
    */

    public String getLocalPart() {
        return (qName == null ? "" : qName.getLocalName());
    }

    /**
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
    * @return The URI of the namespace of this node. For an unnamed node, return null.
    * For a node with an empty prefix, return an empty string.
    */

    public String getURI() {
        return (qName == null ? "" : qName.getNamespaceURI());
    }

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        return (qName == null ? "" : qName.getDisplayName());
    }

    /**
    * Get the NodeInfo object representing the parent of this node
     * @return null - an Orphan has no parent.
    */

    public NodeInfo getParent() {
        return null;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be searched, e.g. Axis.CHILD or Axis.ANCESTOR
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public UnfailingIterator iterateAxis(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ANCESTOR_OR_SELF:
            case Axis.DESCENDANT_OR_SELF:
            case Axis.SELF:
                return SingletonIterator.makeIterator(this);
            case Axis.ANCESTOR:
            case Axis.ATTRIBUTE:
            case Axis.CHILD:
            case Axis.DESCENDANT:
            case Axis.FOLLOWING:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PARENT:
            case Axis.PRECEDING:
            case Axis.PRECEDING_SIBLING:
            case Axis.PRECEDING_OR_ANCESTOR:
                return EmptyIterator.getInstance();
            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }


    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be searched, e.g. Axis.CHILD or Axis.ANCESTOR
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public UnfailingIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        switch (axisNumber) {
            case Axis.ANCESTOR_OR_SELF:
            case Axis.DESCENDANT_OR_SELF:
            case Axis.SELF:
                return Navigator.filteredSingleton(this, nodeTest);
            case Axis.ANCESTOR:
            case Axis.ATTRIBUTE:
            case Axis.CHILD:
            case Axis.DESCENDANT:
            case Axis.FOLLOWING:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PARENT:
            case Axis.PRECEDING:
            case Axis.PRECEDING_SIBLING:
            case Axis.PRECEDING_OR_ANCESTOR:
                return EmptyIterator.getInstance();
            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
    * Get the root node of this tree (not necessarily a document node).
    * Always returns this node in the case of an Orphan node.
    */

    public NodeInfo getRoot() {
        return this;
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document, or null if the
    * node is not part of a document. Always null for an Orphan node.
    */

    public DocumentInfo getDocumentRoot() {
        return null;
    }

    /**
    * Determine whether the node has any children.
    * @return false - an orphan node never has any children
    */

    public boolean hasChildNodes() {
        return false;
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     * @param buffer a buffer, into which will be placed
     * a string that uniquely identifies this node, within this
     * document. The calling code prepends information to make the result
     * unique across all documents.
     */

    public void generateId(FastStringBuffer buffer) {
        buffer.append('Q');
        buffer.append(Integer.toString(hashCode()));
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return hashCode() & 0xffffff;
        // lose the top bits because we need to subtract these values for comparison
    }

    /**
    * Copy this node to a given outputter (deep copy)
    */

    public void copy(Receiver out, int copyOptions) throws XPathException {
        Navigator.copy(this, out, copyOptions);
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

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
