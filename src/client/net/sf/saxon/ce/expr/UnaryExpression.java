package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceExtent;
import client.net.sf.saxon.ce.value.Value;

import java.util.Iterator;

/**
* Unary Expression: an expression taking a single operand expression
*/

public abstract class UnaryExpression extends Expression {

    protected Expression operand;

    protected UnaryExpression() {}

    public UnaryExpression(Expression p0) {
        operand = p0;
        adoptChildExpression(p0);
    }

    public Expression getBaseExpression() {
        return operand;
    }

    /**
    * Simplify an expression
    * @return the simplified expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        return this;
    }

    /**
    * Type-check the expression. Default implementation for unary operators that accept
    * any kind of operand
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        // if the operand value is known, pre-evaluate the expression
        try {
            if (operand instanceof Literal) {
                return Literal.makeLiteral(Value.asValue(
                        SequenceExtent.makeSequenceExtent(
                                iterate(visitor.getStaticContext().makeEarlyEvaluationContext()))));
            }
                //return (Value)ExpressionTool.eagerEvaluate(this, env.makeEarlyEvaluationContext());
        } catch (XPathException err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
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
        operand = visitor.optimize(operand, contextItemType);
        // if the operand value is known, pre-evaluate the expression
        try {
            if (operand instanceof Literal) {
                return Literal.makeLiteral(Value.asValue(
                        SequenceExtent.makeSequenceExtent(
                                iterate(visitor.getStaticContext().makeEarlyEvaluationContext()))));
            }
        } catch (XPathException err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
    }


    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            operand = doPromotion(operand, offer);
            return this;
        }
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator<Expression> iterateSubExpressions() {
        return nonNullChildren(operand);
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        return operand.getSpecialProperties();
    }

    /**
    * Determine the static cardinality. Default implementation returns the cardinality of the operand
    */

    public int computeCardinality() {
        return operand.getCardinality();
    }

    /**
     * Determine the data type of the expression, if possible. The default
     * implementation for unary expressions returns the item type of the operand
     * @return the item type of the items in the result sequence, insofar as this
     * is known statically.
     */

    public ItemType getItemType() {
        return operand.getItemType();
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return other != null && this.getClass().equals(other.getClass()) &&
                this.operand.equals(((UnaryExpression)other).operand);
    }

    /**
    * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
    */

    public int hashCode() {
        return ("UnaryExpression " + getClass()).hashCode()  ^ operand.hashCode();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return getClass().getName() + "(" + operand.toString() + ")";
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
