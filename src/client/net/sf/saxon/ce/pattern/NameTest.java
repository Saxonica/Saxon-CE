package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A NameTest matches the node kind and the namespace URI and the local
  * name.
  *
  * @author Michael H. Kay
  */

public class NameTest extends NodeTest {

	private int nodeKind;
	private StructuredQName qName;

    /**
     * Create a NameTest to match nodes by name
     *
     * @param nodeKind the kind of node, for example {@link client.net.sf.saxon.ce.type.Type#ELEMENT}
     * @param uri the namespace URI of the required nodes. Supply "" to match nodes that are in
     * no namespace
     * @param localName the local name of the required nodes. Supply "" to match unnamed nodes
     * @since 9.0
     */

	public NameTest(int nodeKind, String uri, String localName) {
		this.nodeKind = nodeKind;
		this.qName = new StructuredQName("", uri, localName);
	}

    /**
     * Create a NameTest to match nodes by their nameCode allocated from the NamePool
     * @param nodeKind the kind of node, for example {@link Type#ELEMENT}
     * @param qName the required name of the node
     */

	public NameTest(int nodeKind, StructuredQName qName) {
		this.nodeKind = nodeKind;
		this.qName = qName;
	}

	/**
	 * Create a NameTest for nodes of the same type and name as a given node
     * @param node the node whose node kind and node name will form the basis of the NameTest
	*/

	public NameTest(NodeInfo node) {
		this.nodeKind = node.getNodeKind();
		this.qName = node.getNodeName();
	}

    /**
     * Test whether this node test is satisfied by a given node
     * @param nodeKind The type of node to be matched
     * @param qName identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeKind, StructuredQName qName) {
        return nodeKind == this.nodeKind && qName.equals(qName);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        if (node.getNodeKind() != nodeKind) {
            return false;
        }
        StructuredQName name = node.getNodeName();
        return (name==null ? qName==null : name.equals(qName));
    }

    /**
     * Test whether the NameTest matches a given QName
     * @param name the QName to be matched
     * @return true if the name matches
     */

    public boolean matches(StructuredQName name) {
        return (name==null ? qName==null : name.equals(qName));
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return 0.0;
    }

	/**
	* Get the fingerprint required
	*/

	public StructuredQName getRequiredNodeName() {
		return qName;
	}

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Type.NODE
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getRequiredNodeKind() {
        return nodeKind;
    }

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     * <p>
     * In fact the concept of "supertype" is not really well-defined, because the types
     * form a lattice rather than a hierarchy. The only real requirement on this function
     * is that it returns a type that strictly subsumes this type, ideally as narrowly
     * as possible.
     * @return the supertype, or null if this type is item()
     */

    public ItemType getSuperType() {
        return NodeKindTest.makeNodeKindTest(nodeKind);
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<nodeKind;
    }

    public String toString() {
        switch (nodeKind) {
            case Type.ELEMENT:
                return "element(" + qName.getClarkName() + ")";
            case Type.ATTRIBUTE:
                return "attribute(" + qName.getClarkName() + ")";
            case Type.PROCESSING_INSTRUCTION:
                return "processing-instruction(" + qName.getLocalName() + ')';
            case Type.NAMESPACE:
                return "namespace(" + qName.getLocalName() + ')';
            default:
                return qName.getDisplayName();
        }
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return nodeKind<<20 ^ qName.hashCode();
    }

    /**
     * Determines whether two NameTests are equal
     */

    public boolean equals(Object other) {
        return other instanceof NameTest &&
                ((NameTest)other).nodeKind == nodeKind &&
                ((NameTest)other).qName.equals(qName);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
