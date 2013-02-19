package client.net.sf.saxon.ce.pattern;
import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.type.*;

/**
 * NodeTest is an interface that enables a test of whether a node matches particular
 * conditions. ContentTypeTest tests for an element or attribute node with a particular
 * type annotation.
  *
  * @author Michael H. Kay
  */

public class ContentTypeTest extends NodeTest {

	private int kind;          // element or attribute
    private SchemaType schemaType;
    private int requiredType;
    private Configuration config;
    private boolean nillable = false;

    /**
     * Create a ContentTypeTest
     * @param nodeKind the kind of nodes to be matched: always elements or attributes
     * @param schemaType the required type annotation, as a simple or complex schema type
     * @param config the Configuration, supplied because this KindTest needs access to schema information
     */

	public ContentTypeTest(int nodeKind, SchemaType schemaType, Configuration config) {
		this.kind = nodeKind;
        this.schemaType = schemaType;
        this.requiredType = schemaType.getFingerprint();
        if (requiredType == -1) {
            requiredType = StandardNames.XS_UNTYPED;   // probably doesn't happen
        }
        this.config = config;
	}

    /**
     * Indicate whether nilled elements should be matched (the default is false)
     * @param nillable true if nilled elements should be matched
     */
    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    /**
     * The test is nillable if a question mark was specified as the occurrence indicator
     * @return true if the test is nillable
     */

    public boolean isNillable() {
        return nillable;
    }

     public SchemaType getSchemaType() {
        return schemaType;
    }

    public int getNodeKind() {
        return kind;
    }

    public ItemType getSuperType(TypeHierarchy th) {
        return NodeKindTest.makeNodeKindTest(kind);
    }

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     * @param annotation The actual content type of the node
     */

    public boolean matches(int nodeKind, int fingerprint, int annotation) {
        return kind == nodeKind && matchesAnnotation(annotation);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return node.getNodeKind() == kind &&
                matchesAnnotation(node.getTypeAnnotation());
    }

    private boolean matchesAnnotation(int annotation) {
        if (requiredType == StandardNames.XS_ANY_TYPE) {
            return true;
        }

        if (annotation == -1) {
            annotation = (kind==Type.ATTRIBUTE ? StandardNames.XS_UNTYPED_ATOMIC : StandardNames.XS_UNTYPED);
        }

        if (((annotation & NodeInfo.IS_DTD_TYPE) != 0)) {
            return (requiredType == StandardNames.XS_UNTYPED_ATOMIC);
        }

        if (annotation == requiredType) {
            return true;
        }

        // see if the type annotation is a subtype of the required type


        SchemaType type = config.getSchemaType(annotation & NamePool.FP_MASK).getBaseType();
        if (type == null) {
            // only true if annotation = XS_ANY_TYPE
            return false;
        }
        ItemType actual = new ContentTypeTest(kind, type, config);
        return config.getTypeHierarchy().isSubType(actual, this);
        //return false;
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return 0;
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
     * Get the content type allowed by this NodeTest (that is, the type annotation of the matched nodes).
     * Return AnyType if there are no restrictions. The default implementation returns AnyType.
     */

    public SchemaType getContentType() {
        return schemaType;
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized (assuming that atomization succeeds)
     */

    public AtomicType getAtomizedItemType() {
        SchemaType type = config.getSchemaType(requiredType);
        if (type.isAtomicType()) {
            return (AtomicType)type;
        }
        return BuiltInAtomicType.ANY_ATOMIC;
    }

    public String toString() {
        return (kind == Type.ELEMENT ? "element(*, " : "attribute(*, ") +
                        schemaType.getDisplayName() + ')';
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return kind<<20 ^ requiredType;
     }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof ContentTypeTest &&
                ((ContentTypeTest)other).kind == kind &&
                ((ContentTypeTest)other).schemaType == schemaType &&
                ((ContentTypeTest)other).requiredType == requiredType &&
                ((ContentTypeTest)other).nillable == nillable;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
