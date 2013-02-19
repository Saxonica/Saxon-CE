package client.net.sf.saxon.ce.pattern;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A NamespaceTest matches the node type and the namespace URI.
  *
  * @author Michael H. Kay
  */

public final class NamespaceTest extends NodeTest {

	private NamePool namePool;
	private int nodeKind;
	private short uriCode;
    private String uri;

	public NamespaceTest(NamePool pool, int nodeKind, String uri) {
	    namePool = pool;
		this.nodeKind = nodeKind;
        this.uri = uri;
		this.uriCode = pool.allocateCodeForURI(uri);
	}

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeType The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeType, int fingerprint, int annotation) {
        return fingerprint != -1 &&
                nodeType == nodeKind &&
                uriCode == namePool.getURICode(fingerprint);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return node.getNodeKind()==nodeKind && node.getURI().equals(uri);
    }

    /**
     * Test whether this QNameTest matches a given QName
     * @param qname the QName to be matched
     * @return true if the name matches, false if not
     */

    public boolean matches(StructuredQName qname) {
        return qname.getNamespaceURI().equals(uri);
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.25;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Type.NODE
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
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
     * @param th the type hierarchy cache
     */

    public ItemType getSuperType(TypeHierarchy th) {
        return NodeKindTest.makeNodeKindTest(nodeKind);
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<nodeKind;
    }

    /**
     * Get the namespace URI matched by this NamespaceTest
     * @return  the namespace URI matched by this NamespaceTest
     */

    public String getNamespaceURI() {
        return namePool.getURIFromURICode(uriCode);
    }

    public String toString() {
        return '{' + namePool.getURIFromURICode(uriCode) + "}:*";
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return uriCode << 5 + nodeKind;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof NamespaceTest &&
                ((NamespaceTest)other).namePool == namePool &&
                ((NamespaceTest)other).nodeKind == nodeKind &&
                ((NamespaceTest)other).uriCode == uriCode;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
