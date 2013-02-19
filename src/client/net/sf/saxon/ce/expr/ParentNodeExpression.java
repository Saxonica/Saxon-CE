package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* Class ParentNodeExpression represents the XPath expression ".." or "parent::node()"
*/

public class ParentNodeExpression extends SingleNodeExpression {

    /**
     * Customize the error message on type checking
     */

    protected String noContextMessage() {
        return "Cannot select the parent of the context node";
    }

    /**
    * Return the node selected by this SingleNodeExpression
    * @param context The context for the evaluation
    * @return the parent of the current node defined by the context
    */

    public NodeInfo getNode(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item==null) {
            dynamicError("The context item is not set", "XPDY0002", context);
        }
        if (item instanceof NodeInfo) {
            return ((NodeInfo)item).getParent();
        } else {
            dynamicError("The context item for the parent axis (..) is not a node", "XPTY0020", context);
            return null;
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    private Expression copy() {
        return new ParentNodeExpression();
    }


    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
    * XPathContext.CURRENT_NODE
    */
/*
    public int getDependencies() {
        return StaticProperty.CONTEXT_ITEM;
    }
*/
    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return (other instanceof ParentNodeExpression);
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        return "ParentNodeExpression".hashCode();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return "..";
    }

 
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
