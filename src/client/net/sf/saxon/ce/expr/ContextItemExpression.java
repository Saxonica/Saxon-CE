package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;


/**
* This class represents the expression "(dot)", which always returns the context item.
* This may be a AtomicValue or a Node.
*/

public class ContextItemExpression extends Expression {

    ItemType itemType = Type.ITEM_TYPE;

    /**
     * Create the expression
     */

    public ContextItemExpression() {}

    /**
     * Create a clone copy of this expression
     * @return a copy of this expression
     */

    private Expression copy() {
        ContextItemExpression cie2 = new ContextItemExpression();
        cie2.itemType = itemType;
        return cie2;
    }

    protected String getErrorCodeForUndefinedContext() {
        return "XPDY0002";
    }

    /**
    * Type-check the expression.
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (contextItemType == null) {
            typeError("The context item is undefined at this point", getErrorCodeForUndefinedContext(), null) ;
        }
        itemType = contextItemType;
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        // In XSLT, we don't catch this error at the typeCheck() phase because it's done one XPath expression
        // at a time. So we repeat the check here.
        return typeCheck(visitor, contextItemType);
    }

    /**
     * Determine the item type
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return itemType;
    }

    /**
    * Get the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the special properties of this expression
     * @return the value {@link StaticProperty#NON_CREATIVE}
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return (other instanceof ContextItemExpression);
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        return "ContextItemExpression".hashCode();
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }


    /**
     * Determine whether the expression can be evaluated without reference to the part of the context
     * document outside the subtree rooted at the context node.
     * @return true if the expression has no dependencies on the context node, or if the only dependencies
     *         on the context node are downward selections using the self, child, descendant, attribute, and namespace
     *         axes.
     */

     public boolean isSubtreeExpression() {
        return true;
    }    

    /**
    * Iterate over the value of the expression
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item==null) {
            dynamicError("The context item is not set", getErrorCodeForUndefinedContext(), context);
        }
        return SingletonIterator.makeIterator(item);
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item==null) {
            dynamicError("The context item is not set", getErrorCodeForUndefinedContext(), context);
        }
        return item;
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return ".";
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
