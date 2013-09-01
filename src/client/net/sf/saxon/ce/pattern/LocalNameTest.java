package client.net.sf.saxon.ce.pattern;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A LocalNameTest matches the node type and the local name,
  * it represents an XPath 2.0 test of the form *:name.
  *
  * @author Michael H. Kay
  */

public final class LocalNameTest extends NodeTest {

	private int nodeKind;
	private String localName;

	public LocalNameTest(int nodeKind, String localName) {
		this.nodeKind = nodeKind;
		this.localName = localName;
	}

    /**
    * Test whether this node test is satisfied by a given node
     * @param nodeType The type of node to be matched
      * @param fingerprint identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeType, StructuredQName fingerprint, int annotation) {
        if (fingerprint == null) return false;
        if (nodeType != nodeKind) return false;
        return localName.equals(fingerprint.getLocalName());
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return localName.equals(node.getLocalPart()) && nodeKind == node.getNodeKind();
    }

    /**
     * Test whether this QNameTest matches a given QName
     * @param qname the QName to be matched
     * @return true if the name matches, false if not
     */

    public boolean matches(StructuredQName qname) {
        return localName.equals(qname.getLocalName());
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.25;
    }

    /**
     * Get the local name used in this LocalNameTest
     */

    public String getLocalName() {
        return localName;
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
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<nodeKind;
    }

    public String toString() {
        return "*:" + localName;
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return nodeKind<<20 ^ localName.hashCode();
     }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof LocalNameTest &&
                ((LocalNameTest)other).nodeKind == nodeKind &&
                ((LocalNameTest)other).localName.equals(localName);
    }           

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
