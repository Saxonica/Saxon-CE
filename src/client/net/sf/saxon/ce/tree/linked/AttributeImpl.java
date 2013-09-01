package client.net.sf.saxon.ce.tree.linked;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.Type;

/**
  * A node in the "linked" tree representing an attribute. Note that this is
  * generated only "on demand", when the attribute is selected by a path expression.<P>
  *
  * <p>It is possible for multiple AttributeImpl objects to represent the same attribute node.
  * The identity of an attribute node is determined by the identity of the element, and the index
  * position of the attribute within the element. Index positions are not reused when an attribute
  * is deleted, and are retained when an attribute is renamed.</p>
  *
  * <p>This object no longer caches information such as the name code and string value, because
  * these would become invalid when the element node is modified.</p>
  *
  * @author Michael H. Kay
  */

final class AttributeImpl extends NodeImpl {

    /**
    * Construct an Attribute node for the n'th attribute of a given element
    * @param element The element containing the relevant attribute
    * @param index The index position of the attribute starting at zero
    */

    public AttributeImpl(ElementImpl element, int index) {
        setRawParent(element);
        setSiblingPosition(index);
    }

	/**
	* Get the name of the node
	*/

    public StructuredQName getNodeName() {
        if (getRawParent() == null || getSiblingPosition() == -1) {
            // implies this node is deleted
            return null;
        }
        return ((ElementImpl)getRawParent()).getAttributeList().getStructuredQName(getSiblingPosition());
    }

    /**
     * Get the type annotation of this node, if any
     */

    public int getTypeAnnotation() {
        return StandardNames.XS_UNTYPED_ATOMIC;
    }

    /**
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (!(other instanceof AttributeImpl)) {
            return false;
        }
        if (this==other) {
            return true;
        }
        AttributeImpl otherAtt = (AttributeImpl)other;
        return getRawParent().isSameNodeInfo(otherAtt.getRawParent()) && getSiblingPosition() == otherAtt.getSiblingPosition();
    }

     /**
      * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
      * (represent the same node) then they must have the same hashCode()
      * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics.
      */

     public int hashCode() {
         return getRawParent().hashCode() ^ (getSiblingPosition()<<16);
     }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
    * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
    * the top word the same as their owner and the bottom half reflecting their relative position.
    */

    protected int[] getSequenceNumber() {
        return new int[]{getRawParent().getRawSequenceNumber(), 0x8000 + getSiblingPosition()};
        // note the 0x8000 is to leave room for namespace nodes
    }

    /**
    * Return the type of node.
    * @return Node.ATTRIBUTE
    */

    public final int getNodeKind() {
        return Type.ATTRIBUTE;
    }

    /**
    * Return the character value of the node.
    * @return the attribute value
    */

    public String getStringValue() {
        return ((ElementImpl)getRawParent()).getAttributeList().getValue(getSiblingPosition());
    }

    /**
    * Get next sibling - not defined for attributes
    */

    public NodeInfo getNextSibling() {
        return null;
    }

    /**
    * Get previous sibling - not defined for attributes
    */

    public NodeInfo getPreviousSibling() {
        return null;
    }

    /**
    * Get the previous node in document order (skipping attributes)
    */

    public NodeImpl getPreviousInDocument() {
        return (NodeImpl)getParent();
    }

    /**
    * Get the next node in document order (skipping attributes)
    */

    public NodeImpl getNextInDocument(NodeImpl anchor) {
        if (anchor==this) return null;
        return ((NodeImpl)getParent()).getNextInDocument(anchor);
    }

    /**
     * Get sequential key. Returns key of owning element with the attribute index as a suffix
     * @param buffer a buffer to which the generated ID will be written
     */

    public void generateId(FastStringBuffer buffer) {
        getParent().generateId(buffer);
        buffer.append('a');
        buffer.append(Integer.toString(getSiblingPosition()));
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int copyOptions) throws XPathException {
		StructuredQName name = getNodeName();
        out.attribute(name, getStringValue());
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
