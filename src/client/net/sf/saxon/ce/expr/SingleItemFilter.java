package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.trans.XPathException;

public abstract class SingleItemFilter extends UnaryExpression {

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expresion visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws client.net.sf.saxon.ce.trans.XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        if (!Cardinality.allowsMany(operand.getCardinality())) {
            return operand;
        }
        return super.optimize(visitor, contextItemType);
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                // we can't push the UNORDERED property down to the operand, because order is significant
                operand = doPromotion(operand, offer);
            }
            return this;
        }
    }

    /**
     * Get the static cardinality: this implementation is appropriate for [1] and [last()] which will always
     * return something if the input is non-empty
    */

    public int computeCardinality() {
        return operand.getCardinality() & ~StaticProperty.ALLOWS_MANY;
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
