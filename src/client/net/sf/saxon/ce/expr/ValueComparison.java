package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.sort.AtomicComparer;
import client.net.sf.saxon.ce.expr.sort.CodepointCollatingComparer;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.functions.Count;
import client.net.sf.saxon.ce.functions.StringLength;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.NoDynamicContextException;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

import java.math.BigDecimal;


/**
 * ValueComparison: a boolean expression that compares two atomic values
 * for equals, not-equals, greater-than or less-than. Implements the operators
 * eq, ne, lt, le, gt, ge
 */

public final class ValueComparison extends BinaryExpression implements ComparisonExpression {

    private AtomicComparer comparer;
    private BooleanValue resultWhenEmpty = null;
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
     * Set the result to be returned if one of the operands is an empty sequence
     * @param value the result to be returned if an operand is empty. Supply null to mean the empty sequence.
     */

    public void setResultWhenEmpty(BooleanValue value) {
        resultWhenEmpty = value;
    }

    /**
     * Get the result to be returned if one of the operands is an empty sequence
     * @return BooleanValue.TRUE, BooleanValue.FALSE, or null (meaning the empty sequence)
     */

    public BooleanValue getResultWhenEmpty() {
        return resultWhenEmpty;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        StaticContext env = visitor.getStaticContext();

        operand0 = visitor.typeCheck(operand0, contextItemType);
        if (Literal.isEmptySequence(operand0)) {
            return (resultWhenEmpty == null ? operand0 : Literal.makeLiteral(resultWhenEmpty));
        }

        operand1 = visitor.typeCheck(operand1, contextItemType);
        if (Literal.isEmptySequence(operand1)) {
            return (resultWhenEmpty == null ? operand1 : Literal.makeLiteral(resultWhenEmpty));
        }

        final SequenceType optionalAtomic = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        operand0 = TypeChecker.staticTypeCheck(operand0, optionalAtomic, false, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        operand1 = TypeChecker.staticTypeCheck(operand1, optionalAtomic, false, role1, visitor);

        BuiltInAtomicType t0 = operand0.getItemType(th).getAtomizedItemType();
        BuiltInAtomicType t1 = operand1.getItemType(th).getAtomizedItemType();

        BuiltInAtomicType p0 = (BuiltInAtomicType)t0.getPrimitiveItemType();
        if (p0.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            p0 = BuiltInAtomicType.STRING;
        }
        BuiltInAtomicType p1 = (BuiltInAtomicType)t1.getPrimitiveItemType();
        if (p1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            p1 = BuiltInAtomicType.STRING;
        }

        needsRuntimeCheck =
                p0.equals(BuiltInAtomicType.ANY_ATOMIC) || p1.equals(BuiltInAtomicType.ANY_ATOMIC);

        if (!needsRuntimeCheck && !Type.isComparable(p0, p1, Token.isOrderedOperator(operator))) {
            boolean opt0 = Cardinality.allowsZero(operand0.getCardinality());
            boolean opt1 = Cardinality.allowsZero(operand1.getCardinality());
            if (opt0 || opt1) {
                // This is a comparison such as (xs:integer? eq xs:date?). This is almost
                // certainly an error, but we need to let it through because it will work if
                // one of the operands is an empty sequence.

                String which = null;
                if (opt0) which = "the first operand is";
                if (opt1) which = "the second operand is";
                if (opt0 && opt1) which = "one or both operands are";

                visitor.getStaticContext().issueWarning("Comparison of " + t0.toString() +
                        (opt0 ? "?" : "") + " to " + t1.toString() +
                        (opt1 ? "?" : "") + " will fail unless " + which + " empty", getSourceLocator());
                needsRuntimeCheck = true;
            } else {
                typeError("Cannot compare " + t0.toString() +
                        " to " + t1.toString(), "XPTY0004", null);
            }
        }
        if (!(operator == Token.FEQ || operator == Token.FNE)) {
            if (!p0.isOrdered()) {
                typeError("Type " + t0.toString() + " is not an ordered type", "XPTY0004", null);
            }
            if (!p1.isOrdered()) {
                typeError("Type " + t1.toString() + " is not an ordered type", "XPTY0004", null);
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
                    p0, p1, comp, env.getConfiguration().getConversionContext());
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

        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);

        Value value0 = null;
        Value value1 = null;

        if (operand0 instanceof Literal) {
            value0 = ((Literal)operand0).getValue();
        }

        if (operand1 instanceof Literal) {
            value1 = ((Literal)operand1).getValue();
        }

        // evaluate the expression now if both arguments are constant

        if ((value0 != null) && (value1 != null)) {
            try {
                AtomicValue r = (AtomicValue)evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext());
                //noinspection RedundantCast
                return Literal.makeLiteral(r == null ? (Value)EmptySequence.getInstance() : (Value)r);
            } catch (NoDynamicContextException e) {
                // early evaluation failed, typically because the implicit context isn't available.
                // Try again at run-time
                return this;
            }
        }        

        // optimise count(x) eq 0 (or gt 0, ne 0, eq 0, etc)

        if (operand0 instanceof Count && Literal.isAtomic(operand1)) {
            if (isZero(value1)) {
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

        if (operand1 instanceof Count && isZero(value0)) {
            ValueComparison vc =
                    new ValueComparison(operand1, Token.inverse(operator), operand0);
            ExpressionTool.copyLocationInfo(this, vc);
            return visitor.optimize(visitor.typeCheck(vc, contextItemType), contextItemType);
        }

        // optimise string-length(x) = 0, >0, !=0 etc

        if ((operand0 instanceof StringLength) &&
                (((StringLength) operand0).getNumberOfArguments() == 1) &&
                isZero(value1)) {
            Expression arg = (((StringLength)operand0).getArguments()[0]);
            switch (operator) {
                case Token.FEQ:
                case Token.FLE:
                    return SystemFunction.makeSystemFunction("not", new Expression[]{arg});
                case Token.FNE:
                case Token.FGT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{arg});
                case Token.FGE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
                case Token.FLT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
            }
        }

        // optimise (0 = string-length(x)), etc

        if ((operand1 instanceof StringLength) &&
                        (((StringLength) operand1).getNumberOfArguments() == 1) &&
                        isZero(value0)) {
            Expression arg = (((StringLength)operand1).getArguments()[0]);
            switch (operator) {
                case Token.FEQ:
                case Token.FGE:
                    return SystemFunction.makeSystemFunction("not", new Expression[]{arg});
                case Token.FNE:
                case Token.FLT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{arg});
                case Token.FLE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
                case Token.FGT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
            }
        }

        // optimise string="" etc
        // Note we can change S!="" to boolean(S) for cardinality zero-or-one, but we can only
        // change S="" to not(S) for cardinality exactly-one.

        ItemType p0 = operand0.getItemType(th);
        if ((p0 == BuiltInAtomicType.STRING ||
                p0 == BuiltInAtomicType.ANY_URI ||
                p0 == BuiltInAtomicType.UNTYPED_ATOMIC) &&
                operand1 instanceof Literal &&
                ((Literal)operand1).getValue() instanceof StringValue &&
                ((StringValue)((Literal)operand1).getValue()).isZeroLength() &&
                comparer instanceof CodepointCollatingComparer) {

            switch (operator) {
                case Token.FNE:
                case Token.FGT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{operand0});
                case Token.FEQ:
                case Token.FLE:
                    if (operand0.getCardinality() == StaticProperty.EXACTLY_ONE) {
                        return SystemFunction.makeSystemFunction("not", new Expression[]{operand0});
                    }
            }
        }

        // optimize "" = string etc

        ItemType p1 = operand1.getItemType(th);
        if ((p1 == BuiltInAtomicType.STRING ||
                p1 == BuiltInAtomicType.ANY_URI ||
                p1 == BuiltInAtomicType.UNTYPED_ATOMIC) &&
                operand0 instanceof Literal &&
                ((Literal)operand0).getValue() instanceof StringValue &&
                ((StringValue)((Literal)operand0).getValue()).isZeroLength() &&
                comparer instanceof CodepointCollatingComparer) {

            switch (operator) {
                case Token.FNE:
                case Token.FLT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{operand1});
                case Token.FEQ:
                case Token.FGE:
                    if (operand1.getCardinality() == StaticProperty.EXACTLY_ONE) {
                        return SystemFunction.makeSystemFunction("not", new Expression[]{operand1});
                    }
            }
        }

        return this;
    }

    /**
     * Test whether an expression is constant zero
     * @param v the value to be tested
     * @return true if the operand is the constant zero (of any numeric data type)
     */

    private static boolean isZero(Value v) {
        return v instanceof NumericValue && ((NumericValue)v).compareTo(0) == 0;
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
                return (resultWhenEmpty == BooleanValue.TRUE);  // normally false
            }
            AtomicValue v1 = ((AtomicValue) operand1.evaluateItem(context));
            if (v1 == null) {
                return (resultWhenEmpty == BooleanValue.TRUE);  // normally false
            }
            return compare(v0, operator, v1, comparer.provideContext(context), needsRuntimeCheck);
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(getSourceLocator());
            e.maybeSetContext(context);
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
     * @param collator the Collator to be used when comparing strings
     * @param checkTypes
     * @return the result of the comparison: -1 for LT, 0 for EQ, +1 for GT
     * @throws XPathException if the values are not comparable
     */

    static boolean compare(AtomicValue v0, int op, AtomicValue v1, AtomicComparer collator, boolean checkTypes)
            throws XPathException {
        if (checkTypes &&
                    !Type.isComparable(v0.getPrimitiveType(), v1.getPrimitiveType(), Token.isOrderedOperator(op))) {
            XPathException e2 = new XPathException("Cannot compare " + Type.displayTypeName(v0) +
                " to " + Type.displayTypeName(v1));
            e2.setErrorCode("XPTY0004");
            e2.setIsTypeError(true);
            throw e2;
        }
        if (v0.isNaN() || v1.isNaN()) {
            return (op == Token.FNE);
        }
        try {
            switch (op) {
                case Token.FEQ:
                    return collator.comparesEqual(v0, v1);
                case Token.FNE:
                    return !collator.comparesEqual(v0, v1);
                case Token.FGT:
                    return collator.compareAtomicValues(v0, v1) > 0;
                case Token.FLT:
                    return collator.compareAtomicValues(v0, v1) < 0;
                case Token.FGE:
                    return collator.compareAtomicValues(v0, v1) >= 0;
                case Token.FLE:
                    return collator.compareAtomicValues(v0, v1) <= 0;
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
                return resultWhenEmpty;
            }
            AtomicValue v1 = (AtomicValue) operand1.evaluateItem(context);
            if (v1 == null) {
                return resultWhenEmpty;
            }
            return BooleanValue.get(compare(v0, operator, v1, comparer.provideContext(context), needsRuntimeCheck));
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(getSourceLocator());
            e.maybeSetContext(context);
            throw e;
        }
    }


    /**
     * Determine the data type of the expression
     *
     * @param th the type hierarchy cache
     * @return Type.BOOLEAN
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Determine the static cardinality.
     */

    public int computeCardinality() {
        if (resultWhenEmpty != null) {
            return StaticProperty.EXACTLY_ONE;
        } else {
            return super.computeCardinality();
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
