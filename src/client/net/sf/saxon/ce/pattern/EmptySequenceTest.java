package client.net.sf.saxon.ce.pattern;
import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. An EmptySequenceTest matches no nodes or atomic values: it corresponds to the
  * type empty-sequence().
  *
  * @author Michael H. Kay
  */

public final class EmptySequenceTest extends NodeTest {

    private static EmptySequenceTest THE_INSTANCE = new EmptySequenceTest();

    /**
    * Get a NoNodeTest instance
    */

    public static EmptySequenceTest getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Private constructor
     */ 

    private EmptySequenceTest() {}

	public final int getRequiredNodeKind() {
		return Type.EMPTY;
	}

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public ItemType getPrimitiveItemType() {
        return this;
    }

    /**
     * Test whether a given item conforms to this type
     * @param item The item to be tested
     * @param allowURIPromotion
     * @param config
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        return false;
    }


    /**
    * Test whether this node test is satisfied by a given node
     * @param nodeType The type of node to be matched
      * @param qName identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeType, StructuredQName qName) {
        return false;
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return false;
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
        return 0;
    }

    public String toString() {
        return "empty-sequence()";
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return "NoNodeTest".hashCode();
     }




}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
