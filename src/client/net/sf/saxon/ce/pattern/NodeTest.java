package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;

/**
  * A NodeTest is a simple kind of pattern that enables a context-free test of whether
  * a node has a particular
  * name. There are several kinds of node test: a full name test, a prefix test, and an
  * "any node of a given type" test, an "any node of any type" test, a "no nodes"
  * test (used, e.g. for "@comment()").
  *
  * <p>As well as being used to support XSLT pattern matching, NodeTests act as predicates in
  * axis steps, and also act as item types for type matching.</p>
  *
  * <p>For use in user-written application calling {@link NodeInfo#iterateAxis(byte, NodeTest)},
 * it is possible to write a user-defined subclass of <code>NodeTest</code> that implements
 * a single method, {@link #matches(int, client.net.sf.saxon.ce.om.StructuredQName)}</p>
  *
  * @author Michael H. Kay
  */

public abstract class NodeTest implements ItemType {

    /**
     * Test whether a given item conforms to this type. This implements a method of the ItemType interface.
     *
     *
     * @param item The item to be tested
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item) {
        return item instanceof NodeInfo && matches((NodeInfo)item);
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
        return AnyNodeTest.getInstance();
    }

    /**
     * Determine the default priority of this node test when used on its own as a Pattern
     * @return the default priority
    */

    public abstract double getDefaultPriority();

    /**
     * Get the basic kind of node that this ItemType matches: this is the specific kind of node,
     * or Type.Node if it matches different kinds of nodes.
     * @return the node kind matched by this node test
     */

    public int getRequiredNodeKind() {
        return Type.NODE;
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized (assuming that atomization succeeds)
     */

    public AtomicType getAtomizedItemType() {
        return AtomicType.ANY_ATOMIC;
    }

    /**
     * Test whether this node test is satisfied by a given node. This method is only
     * fully supported for a subset of NodeTests, because it doesn't provide all the information
     * needed to evaluate all node tests. In particular (a) it can't be used to evaluate a node
     * test of the form element(N,T) or schema-element(E) where it is necessary to know whether the
     * node is nilled, and (b) it can't be used to evaluate a node test of the form
     * document-node(element(X)). This in practice means that it is used (a) to evaluate the
     * simple node tests found in the XPath 1.0 subset used in XML Schema, and (b) to evaluate
     * node tests where the node kind is known to be an attribute.
     * @param nodeKind The kind of node to be matched
     * @return true if the node matches
     * @param qName identifies the expanded name of the node to be matched.
     *  The value should be null for a node with no name.
     *
    */

    public abstract boolean matches(int nodeKind, StructuredQName qName);

    /**
     * Test whether this node test is satisfied by a given node.
     * @return true if the node matches
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return matches(node.getNodeKind(), node.getNodeName());
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: Type.ELEMENT for element nodes, Type.TEXT for text nodes, and so on. The default
     * implementation indicates that nodes of all kinds are matched.
     * @return a bit mask indicating the kinds of nodes that can be matched
     */

    public int getNodeKindMask() {
        return 1<<Type.ELEMENT | 1<<Type.TEXT | 1<<Type.COMMENT | 1<<Type.PROCESSING_INSTRUCTION |
                1<<Type.ATTRIBUTE | 1<<Type.NAMESPACE | 1<<Type.DOCUMENT;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
