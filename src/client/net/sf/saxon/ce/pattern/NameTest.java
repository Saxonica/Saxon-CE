package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.om.FingerprintedNode;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;

import java.util.HashSet;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A NameTest matches the node kind and the namespace URI and the local
  * name.
  *
  * @author Michael H. Kay
  */

public class NameTest extends NodeTest {

	private int nodeKind;
	private int fingerprint;
    private NamePool namePool;
    private String uri = null;
    private String localName = null;

    /**
     * Create a NameTest to match nodes by name
     * @param nodeKind the kind of node, for example {@link Type#ELEMENT}
     * @param uri the namespace URI of the required nodes. Supply "" to match nodes that are in
     * no namespace
     * @param localName the local name of the required nodes. Supply "" to match unnamed nodes
     * @param namePool the namePool holding the name codes
     * @since 9.0
     */

	public NameTest(int nodeKind, String uri, String localName, NamePool namePool) {
		this.nodeKind = nodeKind;
		this.fingerprint = namePool.allocate("", uri, localName) & NamePool.FP_MASK;
        this.namePool = namePool;
	}

    /**
     * Create a NameTest to match nodes by their nameCode allocated from the NamePool
     * @param nodeKind the kind of node, for example {@link Type#ELEMENT}
     * @param nameCode the nameCode representing the name of the node
     * @param namePool the namePool holding the name codes
     * @since 8.4
     */

	public NameTest(int nodeKind, int nameCode, NamePool namePool) {
		this.nodeKind = nodeKind;
		this.fingerprint = nameCode & 0xfffff;
        this.namePool = namePool;
	}

	/**
	 * Create a NameTest for nodes of the same type and name as a given node
     * @param node the node whose node kind and node name will form the basis of the NameTest
	*/

	public NameTest(NodeInfo node) {
		this.nodeKind = node.getNodeKind();
		this.fingerprint = node.getFingerprint();
        this.namePool = node.getNamePool();
	}

    /**
     * Test whether this node test is satisfied by a given node
     * @param nodeKind The type of node to be matched
     * @param nameCode identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeKind, int nameCode, int annotation) {
        // System.err.println("Matching node " + nameCode + " against " + this.fingerprint);
        // System.err.println("  " + ((nameCode&0xfffff) == this.fingerprint && nodeType == this.nodeType));
        return ((nameCode&0xfffff) == this.fingerprint && nodeKind == this.nodeKind);
        // deliberately in this order for speed (first test is more likely to fail)
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

        // Two different algorithms are used for name matching. If the fingerprint of the node is readily
        // available, we use it to do an integer comparison. Otherwise, we do string comparisons on the URI
        // and local name. In practice, Saxon's native node implementations use fingerprint matching, while
        // DOM and JDOM nodes use string comparison of names

        if (node instanceof FingerprintedNode) {
            return node.getFingerprint() == fingerprint;
        } else {
            if (uri == null) {
                uri = namePool.getURI(fingerprint);
            }
            if (localName == null) {
                localName = namePool.getLocalName(fingerprint);
            }
            return localName.equals(node.getLocalPart()) && uri.equals(node.getURI());
        }
    }

    /**
     * Test whether the NameTest matches a given QName
     * @param qname the QName to be matched
     * @return true if the name matches
     */

    public boolean matches(StructuredQName qname) {
        if (uri == null) {
            uri = namePool.getURI(fingerprint);
        }
        if (localName == null) {
            localName = namePool.getLocalName(fingerprint);
        }
        return qname.getLocalName().equals(localName) && qname.getNamespaceURI().equals(uri);
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

	public int getFingerprint() {
		return fingerprint;
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
     * Get the set of node names allowed by this NodeTest. This is returned as a set of Integer fingerprints.
     * A null value indicates that all names are permitted (i.e. that there are no constraints on the node name.
     * The default implementation returns null.
     */

    public HashSet<Integer> getRequiredNodeNames() {
        HashSet<Integer> s = new HashSet<Integer>(1);
        s.add(fingerprint);
        return s;
    }

    public String toString() {
        return toString(namePool);
    }

    public String toString(NamePool pool) {
        switch (nodeKind) {
            case Type.ELEMENT:
                return pool.getClarkName(fingerprint);
            case Type.ATTRIBUTE:
                return pool.getClarkName(fingerprint);
            case Type.PROCESSING_INSTRUCTION:
                return "processing-instruction(" + pool.getDisplayName(fingerprint) + ')';
            case Type.NAMESPACE:
                return "namespace(" + pool.getDisplayName(fingerprint) + ')';
        }
        return pool.getDisplayName(fingerprint);
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return nodeKind<<20 ^ fingerprint;
    }

    /**
     * Determines whether two NameTests are equal
     */

    public boolean equals(Object other) {
        return other instanceof NameTest &&
                ((NameTest)other).namePool == namePool &&
                ((NameTest)other).nodeKind == nodeKind &&
                ((NameTest)other).fingerprint == fingerprint;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
