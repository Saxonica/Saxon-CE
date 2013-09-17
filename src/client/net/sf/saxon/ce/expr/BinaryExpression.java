package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.Cardinality;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Binary Expression: a numeric or boolean expression consisting of the
* two operands and an operator
*/

public abstract class BinaryExpression extends Expression {

    protected Expression operand0;
    protected Expression operand1;
    protected int operator;       // represented by the token number from class Tokenizer

    /**
    * Create a binary expression identifying the two operands and the operator
    * @param p0 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.AND)
    * @param p1 the right-hand operand
    */

    public BinaryExpression(Expression p0, int op, Expression p1) {
        operator = op;
        operand0 = p0;
        operand1 = p1;
        adoptChildExpression(p0);
        adoptChildExpression(p1);
    }

    /**
     * Simplify an expression
     * @return the simplified expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand0 = visitor.simplify(operand0);
        operand1 = visitor.simplify(operand1);
        return this;
    }

    /**
    * Type-check the expression. Default implementation for binary operators that accept
    * any kind of operand
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);
        // if both operands are known, pre-evaluate the expression
        try {
            if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
                Sequence v = evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext());
                return Literal.makeLiteral(v);
            }
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
        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);
        // if both operands are known, pre-evaluate the expression
        try {
            if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
                Sequence v = evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext());
                return Literal.makeLiteral(v);
            }
        } catch (XPathException err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
    }


    /**
     * Mark an expression as being "flattened". This is a collective term that includes extracting the
     * string value or typed value, or operations such as simple value construction that concatenate text
     * nodes before atomizing. The implication of all of these is that although the expression might
     * return nodes, the identity of the nodes has no significance. This is called during type checking
     * of the parent expression.
     *
     * @param flattened set to true if the result of the expression is atomized or otherwise turned into
     *                  an atomic value
     */

    public void setFlattened(boolean flattened) {
        operand0.setFlattened(flattened);
        operand1.setFlattened(flattened);
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
                operand0 = doPromotion(operand0, offer);
                operand1 = doPromotion(operand1, offer);
            }
            return this;
        }
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator<Expression> iterateSubExpressions() {
        return nonNullChildren(operand0, operand1);
    }


    /**
     * Get the operator
     * @return the operator, for example {@link Token#PLUS}
     */

    public int getOperator() {
        return operator;
    }

    /**
     * Get the operands
     * @return the two operands of the binary expression, as an array of length 2
     */

    public Expression[] getOperands() {
        return new Expression[] {operand0, operand1};
    }

    /**
    * Determine the static cardinality. Default implementation returns [0..1] if either operand
     * can be empty, or [1..1] otherwise.
    */

    public int computeCardinality() {
        if (Cardinality.allowsZero(operand0.getCardinality()) ||
                Cardinality.allowsZero(operand1.getCardinality())) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}. This is overridden
     * for some subclasses.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
     * Determine whether a binary operator is commutative, that is, A op B = B op A.
     * @param operator the operator, for example {@link Token#PLUS}
     * @return true if the operator is commutative
     */

    protected static boolean isCommutative(int operator) {
        return (operator == Token.AND ||
                operator == Token.OR ||
                operator == Token.UNION ||
                operator == Token.INTERSECT ||
                operator == Token.PLUS ||
                operator == Token.MULT ||
                operator == Token.EQUALS ||
                operator == Token.FEQ ||
                operator == Token.NE ||
                operator == Token.FNE
                );
    }

    /**
     * Determine whether an operator is associative, that is, ((a^b)^c) = (a^(b^c))
     * @param operator the operator, for example {@link Token#PLUS}
     * @return true if the operator is associative
     */

    protected static boolean isAssociative(int operator) {
        return (operator == Token.AND ||
                operator == Token.OR ||
                operator == Token.UNION ||
                operator == Token.INTERSECT ||
                operator == Token.PLUS ||
                operator == Token.MULT
                );
    }
    /**
     * Test if one operator is the inverse of another, so that (A op1 B) is
     * equivalent to (B op2 A). Commutative operators are the inverse of themselves
     * and are therefore not listed here.
     * @param op1 the first operator
     * @param op2 the second operator
     * @return true if the operators are the inverse of each other
     */
    protected static boolean isInverse(int op1, int op2) {
        return op1 != op2 && op1 == Token.inverse(op2);
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        if (other instanceof BinaryExpression) {
            BinaryExpression b = (BinaryExpression)other;
            if (operator == b.operator) {
                if (operand0.equals(b.operand0) &&
                        operand1.equals(b.operand1)) {
                    return true;
                }
                if (isCommutative(operator) &&
                        operand0.equals(b.operand1) &&
                        operand1.equals(b.operand0)) {
                    return true;
                }
                if (isAssociative(operator) &&
                        pairwiseEqual(flattenExpression(new ArrayList(4)),
                                b.flattenExpression(new ArrayList(4)))) {
                    return true;
                }
            }
            if (isInverse(operator, b.operator) &&
                    operand0.equals(b.operand1) &&
                    operand1.equals(b.operand0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flatten an expression with respect to an associative operator: for example
     * the expression (a+b) + (c+d) becomes list(a,b,c,d), with the list in canonical
     * order (sorted by hashCode)
     * @param list a list provided by the caller to contain the result
     * @return the list of expressions
     */

    private List flattenExpression(List list) {
        if (operand0 instanceof BinaryExpression &&
                ((BinaryExpression)operand0).operator == operator) {
            ((BinaryExpression)operand0).flattenExpression(list);
        } else {
            int h = operand0.hashCode();
            list.add(operand0);
            int i = list.size()-1;
            while (i > 0 && h > list.get(i-1).hashCode()) {
                list.set(i, list.get(i-1));
                list.set(i-1, operand0);
                i--;
            }
        }
        if (operand1 instanceof BinaryExpression &&
                ((BinaryExpression)operand1).operator == operator) {
            ((BinaryExpression)operand1).flattenExpression(list);
        } else {
            int h = operand1.hashCode();
            list.add(operand1);
            int i = list.size()-1;
            while (i > 0 && h > list.get(i-1).hashCode()) {
                list.set(i, list.get(i-1));
                list.set(i-1, operand1);
                i--;
            }
        }
        return list;
    }

    /**
     * Compare whether two lists of expressions are pairwise equal
     * @param a the first list of expressions
     * @param b the second list of expressions
     * @return true if the two lists are equal
     */

    private boolean pairwiseEqual(List a, List b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i=0; i<a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a hashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
    */

    public int hashCode() {
        // Ensure that an operator and its inverse get the same hash code,
        // so that (A lt B) has the same hash code as (B gt A)
        int op = Math.min(operator, Token.inverse(operator));
        return ("BinaryExpression " + op).hashCode()
                ^ operand0.hashCode()
                ^ operand1.hashCode();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return "(" + operand0.toString() + " " + displayOperator() + " " + operand1.toString() + ")";
    }

    /**
     * Display the operator used by this binary expression
     * @return String representation of the operator (for diagnostic display only)
     */

    protected String displayOperator() {
        return Token.tokens[operator];
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
