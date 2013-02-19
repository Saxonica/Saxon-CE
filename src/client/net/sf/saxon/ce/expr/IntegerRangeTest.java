package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.NumericValue;

import java.util.Arrays;
import java.util.Iterator;

/**
* An IntegerRangeTest is an expression of the form
 * E = N to M
 * where E is numeric, and N and M are both expressions of type integer.
*/

public class IntegerRangeTest extends Expression {

    Expression value;
    Expression min;
    Expression max;

    /**
     * Construct a IntegerRangeTest
     * @param value the integer value to be tested to see if it is in the range min to max inclusive
     * @param min the lowest permitted value
     * @param max the highest permitted value
     */

    public IntegerRangeTest(Expression value, Expression min, Expression max) {
        this.value = value;
        this.min = min;
        this.max = max;
    }

    /**
     * Get the value to be tested
     * @return the expression that evaluates to the value being tested
     */

    public Expression getValueExpression() {
        return value;
    }

    /**
     * Get the expression denoting the start of the range
     * @return the expression denoting the minumum value
     */

    public Expression getMinValueExpression() {
        return min;
    }

    /**
     * Get the expression denoting the end of the range
     * @return the expression denoting the maximum value
     */

    public Expression getMaxValueExpression() {
        return max;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        // Already done, we only get one of these expressions after the operands have been analyzed
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
        return this;
    }

    /**
    * Get the data type of the items returned
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator<Expression> iterateSubExpressions() {
        Expression[] e = {value, min, max};
        return Arrays.asList(e).iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (value == original) {
            value = replacement;
            found = true;
        }
        if (min == original) {
            min = replacement;
            found = true;
        }
        if (max == original) {
            max = replacement;
            found = true;
        }
                return found;
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
                value = doPromotion(value, offer);
                min = doPromotion(min, offer);
                max = doPromotion(max, offer);
            }
            return this;
        }
    }



    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue av = (AtomicValue)value.evaluateItem(c);
        if (av==null) {
            return BooleanValue.FALSE;
        }
        NumericValue v = (NumericValue)av;

        if (!v.isWholeNumber()) {
            return BooleanValue.FALSE;
        }

        AtomicValue av2 = (AtomicValue)min.evaluateItem(c);
        NumericValue v2 = (NumericValue)av2;

        if (v.compareTo(v2) < 0) {
            return BooleanValue.FALSE;
        }

        AtomicValue av3 = (AtomicValue)max.evaluateItem(c);
        NumericValue v3 = (NumericValue)av3;

        return BooleanValue.get(v.compareTo(v3) <= 0);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.