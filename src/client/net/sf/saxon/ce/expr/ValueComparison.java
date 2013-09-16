package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.sort.AtomicComparer;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.functions.Count;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.*;

import java.math.BigDecimal;


/**
 * ValueComparison: a boolean expression that compares two atomic values
 * for equals, not-equals, greater-than or less-than. Implements the operators
 * eq, ne, lt, le, gt, ge
 */

public final class ValueComparison extends BinaryExpression implements ComparisonExpression {

    private AtomicComparer comparer;
    private boolean needsRuntimeCheck;

    /**
     * Create a comparison expression identifying the two operands and the operator
     *
     * @param p1 the left-hand operand
     * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
     * @param p2 the right-hand operand
     */

    public ValueComparison(Expression p1, int op, Expression p2) {
        super(p1, op, p2);
    }

    /**
     * Set the AtomicComparer used to compare atomic values
     * @param comparer the AtomicComparer
     */

    public void setAtomicComparer(AtomicComparer comparer) {
        this.comparer = comparer;
    }

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used.
     * Note that the comparer is always known at compile time.
     */

    public AtomicComparer getAtomicComparer() {
        return comparer;
    }

    /**
     * Get the primitive (singleton) operator used: one of Token.FEQ, Token.FNE, Token.FLT, Token.FGT,
     * Token.FLE, Token.FGE
     */

    public int getSingletonOperator() {
        return operator;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        StaticContext env = visitor.getStaticContext();

        operand0 = visitor.typeCheck(operand0, contextItemType);
        if (Literal.isEmptySequence(operand0)) {
            return operand0;
        }

        operand1 = visitor.typeCheck(operand1, contextItemType);
        if (Literal.isEmptySequence(operand1)) {
            return operand1;
        }

        final SequenceType optionalAtomic = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        operand0 = TypeChecker.staticTypeCheck(operand0, optionalAtomic, false, role0);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        operand1 = TypeChecker.staticTypeCheck(operand1, optionalAtomic, false, role1);

        AtomicType t0 = operand0.getItemType().getAtomizedItemType();
        AtomicType t1 = operand1.getItemType().getAtomizedItemType();

        AtomicType p0 = (AtomicType)t0.getPrimitiveItemType();
        if (p0.equals(AtomicType.UNTYPED_ATOMIC)) {
            p0 = AtomicType.STRING;
        }
        AtomicType p1 = (AtomicType)t1.getPrimitiveItemType();
        if (p1.equals(AtomicType.UNTYPED_ATOMIC)) {
            p1 = AtomicType.STRING;
        }

        needsRuntimeCheck =
                p0.equals(AtomicType.ANY_ATOMIC) || p1.equals(AtomicType.ANY_ATOMIC);

        if (!needsRuntimeCheck && !Type.isComparable(p0, p1, Token.isOrderedOperator(operator))) {
            boolean opt0 = Cardinality.allowsZero(operand0.getCardinality());
            boolean opt1 = Cardinality.allowsZero(operand1.getCardinality());
            if (opt0 || opt1) {
                // This is a comparison such as (xs:integer? eq xs:date?). This is almost
                // certainly an error, but we need to let it through because it will work if
                // one of the operands is an empty sequence.
                needsRuntimeCheck = true;
            } else {
                typeError("Cannot compare " + t0.toString() +
                        " to " + t1.toString(), "XPTY0004");
            }
        }
        if (!(operator == Token.FEQ || operator == Token.FNE)) {
            if (!p0.isOrdered()) {
                typeError("Type " + t0.toString() + " is not an ordered type", "XPTY0004");
            }
            if (!p1.isOrdered()) {
                typeError("Type " + t1.toString() + " is not an ordered type", "XPTY0004");
            }
        }

        if (comparer == null) {
            // In XSLT, only do this the first time through, otherwise the default-collation attribute may be missed
            final String defaultCollationName = env.getDefaultCollationName();
            StringCollator comp = env.getConfiguration().getNamedCollation(defaultCollationName);
            if (comp == null) {
                comp = CodepointCollator.getInstance();
            }
            comparer = GenericAtomicComparer.makeAtomicComparer(
                    p0, p1, comp, env.getConfiguration().getImplicitTimezone());
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

        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }

        // optimise count(x) eq 0 (or gt 0, ne 0, eq 0, etc)

        if (operand0 instanceof Count && Literal.isAtomic(operand1)) {
            AtomicValue value1 = (AtomicValue)((Literal)operand1).getValue();
            if (value1 instanceof NumericValue && ((NumericValue) value1).compareTo(0) == 0) {
                if (operator == Token.FEQ || operator == Token.FLE) {
                    // rewrite count(x)=0 as empty(x)
                    return SystemFunction.makeSystemFunction(
                            "empty", new Expression[]{((FunctionCall) operand0).argument[0]});
                } else if (operator == Token.FNE || operator == Token.FGT) {
                    // rewrite count(x)!=0, count(x)>0 as exists(x)
                    return SystemFunction.makeSystemFunction(
                            "exists", new Expression[]{((FunctionCall) operand0).argument[0]});
                } else if (operator == Token.FGE) {
                    // rewrite count(x)>=0 as true()
                    return Literal.makeLiteral(BooleanValue.TRUE);
                } else {  // singletonOperator == Token.FLT
                    // rewrite count(x)<0 as false()
                    return Literal.makeLiteral(BooleanValue.FALSE);
                }
            } else if (value1 instanceof IntegerValue &&
                    (operator == Token.FGT || operator == Token.FGE)) {
                // rewrite count(x) gt n as exists(x[n+1])
                //     and count(x) ge n as exists(x[n])
                long val = ((IntegerValue) value1).intValue();
                if (operator == Token.FGT) {
                    val++;
                }
                FilterExpression filter = new FilterExpression(((FunctionCall) operand0).argument[0],
                                Literal.makeLiteral(new IntegerValue(new BigDecimal(val))));
                ExpressionTool.copyLocationInfo(this, filter);
                return SystemFunction.makeSystemFunction("exists", new Expression[]{filter});
            }
        }

        // optimise (0 eq count(x)), etc

        if (operand1 instanceof Count && Literal.isAtomic(operand0)) {
            ValueComparison vc =
                    new ValueComparison(operand1, Token.inverse(operator), operand0);
            ExpressionTool.copyLocationInfo(this, vc);
            return visitor.optimize(vc, contextItemType);
        }

        return this;
    }


    /**
     * Evaluate the effective boolean value of the expression
     *
     * @param context the given context for evaluation
     * @return a boolean representing the result of the comparison of the two operands
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        try {
            AtomicValue v0 = ((AtomicValue) operand0.evaluateItem(context));
            if (v0 == null) {
                return false;
            }
            AtomicValue v1 = ((AtomicValue) operand1.evaluateItem(context));
            if (v1 == null) {
                return false;
            }
            return compare(v0, operator, v1, comparer, needsRuntimeCheck);
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(getSourceLocator());
            throw e;
        } 
    }

    /**
     * Compare two atomic values, using a specified operator and collation
     *
     * @param v0       the first operand
     * @param op       the operator, as defined by constants such as {@link client.net.sf.saxon.ce.expr.Token#FEQ} or
     *                 {@link client.net.sf.saxon.ce.expr.Token#FLT}
     * @param v1       the second operand
     * @param comparer identifies the Collator to be used when comparing strings
     * @param checkTypes true if a check is required that the types of the arguments are comparable
     * @return the result of the comparison: -1 for LT, 0 for EQ, +1 for GT
     * @throws XPathException if the values are not comparable
     */

    static boolean compare(AtomicValue v0, int op, AtomicValue v1, AtomicComparer comparer, boolean checkTypes)
            throws XPathException {
        if (checkTypes &&
                    !Type.isComparable(v0.getItemType(), v1.getItemType(), Token.isOrderedOperator(op))) {
            throw new XPathException("Cannot compare " + Type.displayTypeName(v0) +
                " to " + Type.displayTypeName(v1), "XPTY0004");
        }
        if (v0.isNaN() || v1.isNaN()) {
            return (op == Token.FNE);
        }
        try {
            switch (op) {
                case Token.FEQ:
                    return comparer.comparesEqual(v0, v1);
                case Token.FNE:
                    return !comparer.comparesEqual(v0, v1);
                case Token.FGT:
                    return comparer.compareAtomicValues(v0, v1) > 0;
                case Token.FLT:
                    return comparer.compareAtomicValues(v0, v1) < 0;
                case Token.FGE:
                    return comparer.compareAtomicValues(v0, v1) >= 0;
                case Token.FLE:
                    return comparer.compareAtomicValues(v0, v1) <= 0;
                default:
                    throw new UnsupportedOperationException("Unknown operator " + op);
            }
        } catch (ClassCastException err) {
            XPathException e2 = new XPathException("Cannot compare " + Type.displayTypeName(v0) +
                    " to " + Type.displayTypeName(v1));
            e2.setErrorCode("XPTY0004");
            e2.setIsTypeError(true);
            throw e2;
        }
    }

    /**
     * Evaluate the expression in a given context
     *
     * @param context the given context for evaluation
     * @return a BooleanValue representing the result of the numeric comparison of the two operands,
     *         or null representing the empty sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        try {
            AtomicValue v0 = (AtomicValue) operand0.evaluateItem(context);
            if (v0 == null) {
                return null;
            }
            AtomicValue v1 = (AtomicValue) operand1.evaluateItem(context);
            if (v1 == null) {
                return null;
            }
            return BooleanValue.get(compare(v0, operator, v1, comparer, needsRuntimeCheck));
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(getSourceLocator());
            throw e;
        }
    }


    /**
     * Determine the data type of the expression
     *
     * @return Type.BOOLEAN
     */

    public ItemType getItemType() {
        return AtomicType.BOOLEAN;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
