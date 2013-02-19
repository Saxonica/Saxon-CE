package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. An AnyNodeTest matches any node.
  *
  * @author Michael H. Kay
  */

public final class AnyNodeTest extends NodeTest {

    private static AnyNodeTest THE_INSTANCE = new AnyNodeTest();

    /**
    * Get an instance of AnyNodeTest
    */

    public static AnyNodeTest getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Private constructor
     */

    private AnyNodeTest() {}

    /**
     * Test whether a given item conforms to this type
     * @param item The item to be tested
     * @param allowURIPromotion
     * @param config
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        return (item instanceof NodeInfo);
    }

    public ItemType getSuperType(TypeHierarchy th) {
        return AnyItemType.getInstance();
    }

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeType The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     */

    public final boolean matches(int nodeType, int fingerprint, int annotation) {
        return nodeType != Type.PARENT_POINTER;
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return true;
    }

    /**
     * Test whether this QNameTest matches a given QName
     * @param qname the QName to be matched
     * @return true if the name matches, false if not
     */

    public boolean matches(StructuredQName qname) {
        return true;
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.5;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<Type.ELEMENT | 1<<Type.TEXT | 1<<Type.COMMENT | 1<<Type.PROCESSING_INSTRUCTION |
                1<<Type.ATTRIBUTE | 1<<Type.NAMESPACE | 1<<Type.DOCUMENT;
    }

    public String toString() {
        return "node()";
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return "AnyNodeTest".hashCode();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
