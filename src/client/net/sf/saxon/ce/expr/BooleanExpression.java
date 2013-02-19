package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.functions.BooleanFn;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.BooleanValue;


/**
* Boolean expression: two truth values combined using AND or OR.
*/

public class BooleanExpression extends BinaryExpression {

    /**
     * Construct a boolean expression
     * @param p1 the first operand
     * @param operator one of {@link Token#AND} or {@link Token#OR}
     * @param p2 the second operand
     */

    public BooleanExpression(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(visitor, contextItemType);
        if (e == this) {
            XPathException err0 = TypeChecker.ebvError(operand0, visitor.getConfiguration().getTypeHierarchy());
            if (err0 != null) {
                err0.setLocator(getSourceLocator());
                throw err0;
            }
            XPathException err1 = TypeChecker.ebvError(operand1, visitor.getConfiguration().getTypeHierarchy());
            if (err1 != null) {
                err1.setLocator(getSourceLocator());
                throw err1;
            }
            // Precompute the EBV of any constant operand
            if (operand0 instanceof Literal && !(((Literal)operand0).getValue() instanceof BooleanValue)) {
                operand0 = Literal.makeLiteral(BooleanValue.get(operand0.effectiveBooleanValue(null)));
            }
            if (operand1 instanceof Literal && !(((Literal)operand1).getValue() instanceof BooleanValue)) {
                operand1 = Literal.makeLiteral(BooleanValue.get(operand1.effectiveBooleanValue(null)));
            }
        }
        return e;
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
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

        final Expression e = super.optimize(visitor, contextItemType);
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        if (e != this) {
            return e;
        }

        Expression op0 = BooleanFn.rewriteEffectiveBooleanValue(operand0, visitor, contextItemType);
        if (op0 != null) {
            operand0 = op0;
        }
        Expression op1 = BooleanFn.rewriteEffectiveBooleanValue(operand1, visitor, contextItemType);
        if (op1 != null) {
            operand1 = op1;
        }

        // If the value can be determined from knowledge of one operand, precompute the result

        if (operator == Token.AND) {
            if (Literal.isConstantBoolean(operand0, false) || Literal.isConstantBoolean(operand1, false)) {
                // A and false() => false()
                // false() and B => false()
                return new Literal(BooleanValue.FALSE);
            } else if (Literal.isConstantBoolean(operand0, true)) {
                // true() and B => B
                return forceToBoolean(operand1, th);
            } else if (Literal.isConstantBoolean(operand1, true)) {
                // A and true() => A
                return forceToBoolean(operand0, th);
            }
        }

        if (operator == Token.OR) {
            if (Literal.isConstantBoolean(operand0, true) || Literal.isConstantBoolean(operand1, true)) {
                // A or true() => true()
                // true() or B => true()                
                return new Literal(BooleanValue.TRUE);
            } else if (Literal.isConstantBoolean(operand0, false)) {
                // false() or B => B
                return forceToBoolean(operand1, th);
            } else if (Literal.isConstantBoolean(operand1, false)) {
                // A or false() => A
                return forceToBoolean(operand0, th);
            }
        }

        // Rewrite (A and B) as (if (A) then B else false()). The benefit of this is that when B is a recursive
        // function call, it is treated as a tail call (test qxmp290). To avoid disrupting other optimizations
        // of "and" expressions (specifically, where clauses in FLWOR expressions), do this ONLY if B is a user
        // function call (we can't tell if it's recursive), and it's not in a loop.


        if (e == this && operator == Token.AND &&
                operand1 instanceof UserFunctionCall &&
                th.isSubType(operand1.getItemType(th), BuiltInAtomicType.BOOLEAN) &&
                !visitor.isLoopingSubexpression(null)) {
            Expression cond = Choose.makeConditional(operand0, operand1, Literal.makeLiteral(BooleanValue.FALSE));
            ExpressionTool.copyLocationInfo(this, cond);
            return cond;
        }
        return this;
    }

    private Expression forceToBoolean(Expression in, TypeHierarchy th) {
        if (in.getItemType(th) == BuiltInAtomicType.BOOLEAN && in.getCardinality() == StaticProperty.ALLOWS_ONE) {
            return in;
        } else {
            return SystemFunction.makeSystemFunction("boolean", new Expression[]{in});
        }
    }

   /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate as a boolean.
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        switch(operator) {
            case Token.AND:
                return operand0.effectiveBooleanValue(c) && operand1.effectiveBooleanValue(c);

            case Token.OR:
                return operand0.effectiveBooleanValue(c) || operand1.effectiveBooleanValue(c);

            default:
                throw new UnsupportedOperationException("Unknown operator in boolean expression");
        }
    }

    /**
     * Determine the data type of the expression
     * @return BuiltInAtomicType.BOOLEAN
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
