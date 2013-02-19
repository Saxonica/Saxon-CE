package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.BooleanValue;

/**
 * Implementation of the fn:exists function
 */
public class Empty extends Aggregate {

    public Empty newInstance() {
        return new Empty();
    }

    /**
     * Return the negation of the expression
     * @return the negation of the expression
     */

    public Expression negate() {
        FunctionCall fc = SystemFunction.makeSystemFunction("exists", getArguments());
        fc.setSourceLocator(getSourceLocator());
        return fc;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // See if we can deduce the answer from the cardinality
        int c = argument[0].getCardinality();
        if (c == StaticProperty.ALLOWS_ONE_OR_MORE) {
            return new Literal(BooleanValue.FALSE);
        } else if (c == StaticProperty.ALLOWS_ZERO) {
            return new Literal(BooleanValue.TRUE);
        }
        // Rewrite
        //    empty(A|B) => empty(A) and empty(B)
        if (argument[0] instanceof VennExpression) {
            VennExpression v = (VennExpression)argument[0];
            if (v.getOperator() == Token.UNION) {
                FunctionCall e0 = SystemFunction.makeSystemFunction("empty", new Expression[]{v.getOperands()[0]});
                FunctionCall e1 = SystemFunction.makeSystemFunction("empty", new Expression[]{v.getOperands()[1]});
                return new BooleanExpression(e0, Token.AND, e1).optimize(visitor, contextItemType);
            }
        }
        return this;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the function in a boolean context
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        SequenceIterator iter = argument[0].iterate(c);
        return iter.next() == null;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.