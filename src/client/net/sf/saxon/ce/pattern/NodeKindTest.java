package client.net.sf.saxon.ce.pattern;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.type.*;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and kind. A NodeKindTest matches the node kind only.
  *
  * @author Michael H. Kay
  */

public class NodeKindTest extends NodeTest {

    public static final NodeKindTest DOCUMENT = new NodeKindTest(Type.DOCUMENT);
    public static final NodeKindTest ELEMENT = new NodeKindTest(Type.ELEMENT);
    public static final NodeKindTest ATTRIBUTE = new NodeKindTest(Type.ATTRIBUTE);
    public static final NodeKindTest TEXT = new NodeKindTest(Type.TEXT);
    public static final NodeKindTest COMMENT = new NodeKindTest(Type.COMMENT);
    public static final NodeKindTest PROCESSING_INSTRUCTION = new NodeKindTest(Type.PROCESSING_INSTRUCTION);
    public static final NodeKindTest NAMESPACE = new NodeKindTest(Type.NAMESPACE);


	private int kind;

	private NodeKindTest(int nodeKind) {
		kind = nodeKind;
	}

    /**
     * Make a test for a given kind of node
     */

    public static NodeTest makeNodeKindTest(int kind) {
		switch (kind) {
		    case Type.DOCUMENT:
		        return DOCUMENT;
		    case Type.ELEMENT:
                return ELEMENT;
		    case Type.ATTRIBUTE:
		        return ATTRIBUTE;
		    case Type.COMMENT:
		        return COMMENT;
		    case Type.TEXT:
		        return TEXT;
		    case Type.PROCESSING_INSTRUCTION:
		        return PROCESSING_INSTRUCTION;
		    case Type.NAMESPACE:
		        return NAMESPACE;
            case Type.NODE:
                return AnyNodeTest.getInstance();
            default:
                throw new IllegalArgumentException("Unknown node kind in NodeKindTest");
		}
    }

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     */

    public boolean matches(int nodeKind, int fingerprint, int annotation) {
        return (kind == nodeKind);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return node.getNodeKind() == kind;
    }


    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.5;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return kind;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<kind;
    }

    /**
     * Get the content type allowed by this NodeTest (that is, the type of content allowed).
     * Return AnyType if there are no restrictions.
     */

    public SchemaType getContentType() {
        switch (kind) {
            case Type.DOCUMENT:
                return AnyType.getInstance();
            case Type.ELEMENT:
                return AnyType.getInstance();
            case Type.ATTRIBUTE:
                return BuiltInAtomicType.UNTYPED_ATOMIC;
            case Type.COMMENT:
                return BuiltInAtomicType.STRING;
            case Type.TEXT:
                return BuiltInAtomicType.UNTYPED_ATOMIC;
            case Type.PROCESSING_INSTRUCTION:
                return BuiltInAtomicType.STRING;
            case Type.NAMESPACE:
                return BuiltInAtomicType.STRING;
            default:
                throw new AssertionError("Unknown node kind");
        }
    }

    /**
     * Get the content type allowed by this NodeTest (that is, the type annotation).
     * Return AnyType if there are no restrictions. The default implementation returns AnyType.
     */

    public AtomicType getAtomizedItemType() {
        switch (kind) {
            case Type.DOCUMENT:
                return BuiltInAtomicType.UNTYPED_ATOMIC;
            case Type.ELEMENT:
                return BuiltInAtomicType.ANY_ATOMIC;
            case Type.ATTRIBUTE:
                return BuiltInAtomicType.ANY_ATOMIC;
            case Type.COMMENT:
                return BuiltInAtomicType.STRING;
            case Type.TEXT:
                return BuiltInAtomicType.UNTYPED_ATOMIC;
            case Type.PROCESSING_INSTRUCTION:
                return BuiltInAtomicType.STRING;
            case Type.NAMESPACE:
                return BuiltInAtomicType.STRING;
            default:
                throw new AssertionError("Unknown node kind");
        }
    }

    public String toString() {
        return toString(kind);
    }

    public static String toString(int kind) {
        switch (kind) {
            case Type.DOCUMENT:
                return( "document-node()" );
            case Type.ELEMENT:
                return( "element()" );
            case Type.ATTRIBUTE:
                return( "attribute()" );
            case Type.COMMENT:
                return( "comment()" );
            case Type.TEXT:
                return( "text()" );
            case Type.PROCESSING_INSTRUCTION:
                return( "processing-instruction()" );
            case Type.NAMESPACE:
                return( "namespace()" );
            default:
                return( "** error **");
        }
    }

    /**
     * Get the name of a node kind
     * @param kind the node kind, for example Type.ELEMENT or Type.ATTRIBUTE
     * @return the name of the node kind, for example "element" or "attribute"
     */

    public static String nodeKindName(int kind) {
        switch (kind) {
            case Type.DOCUMENT:
                return( "document" );
            case Type.ELEMENT:
                return( "element" );
            case Type.ATTRIBUTE:
                return( "attribute" );
            case Type.COMMENT:
                return( "comment" );
            case Type.TEXT:
                return( "text" );
            case Type.PROCESSING_INSTRUCTION:
                return( "processing-instruction" );
            case Type.NAMESPACE:
                return( "namespace" );
            default:
                return( "** error **");
        }
    }


    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return kind;
     }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof NodeKindTest &&
                ((NodeKindTest)other).kind == kind;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
