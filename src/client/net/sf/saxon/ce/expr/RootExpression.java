package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;


/**
 * An expression whose value is always a set of nodes containing a single node,
 * the document root. This corresponds to the XPath Expression "/", including the implicit
 * "/" at the start of a path expression with a leading "/".
*/

public class RootExpression extends SingleNodeExpression {

    /**
     * Customize the error message on type checking
     */

    protected String noContextMessage() {
        return "Leading '/' cannot select the root node of the tree containing the context item";
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return (other instanceof RootExpression);
    }

    /**
    * Specify that the expression returns a singleton
    */

    public final int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the data type of the items returned by this expression
     *
     * @return Type.NODE
     */

    public ItemType getItemType() {
        return NodeKindTest.DOCUMENT;
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        return "RootExpression".hashCode();
    }

    /**
    * Return the first element selected by this Expression
    * @param context The evaluation context
    * @return the NodeInfo of the first selected element, or null if no element
    * is selected
    */

    public NodeInfo getNode(XPathContext context) throws XPathException {
        Item current = context.getContextItem();
        if (current==null) {
            dynamicError("Finding root of tree: the context item is undefined", "XPDY0002");
        }
        if (current instanceof NodeInfo) {
            DocumentInfo doc = ((NodeInfo)current).getDocumentRoot();
            if (doc==null) {
                dynamicError("The root of the tree containing the context item is not a document node", "XPDY0050");
            }
            return doc;
        }
        typeError("Finding root of tree: the context item is not a node", "XPTY0020");
        // dummy return; we never get here
        return null;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
    * StaticProperty.CURRENT_NODE
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_DOCUMENT;
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    private Expression copy() {
        return new RootExpression();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return "(/)";
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
