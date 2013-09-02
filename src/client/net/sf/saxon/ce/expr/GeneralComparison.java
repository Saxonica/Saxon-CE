package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.sort.AtomicComparer;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.functions.Minimax;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;

import java.math.BigDecimal;


/**
 * GeneralComparison: a boolean expression that compares two expressions
 * for equals, not-equals, greater-than or less-than. This implements the operators
 * =, !=, <, >, etc. This implementation is not used when in backwards-compatible mode
 */

public class GeneralComparison extends BinaryExpression implements ComparisonExpression {

    protected int singletonOperator;
    protected AtomicComparer comparer;


    /**
     * Create a relational expression identifying the two operands and the operator
     *
     * @param p0 the left-hand operand
     * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
     * @param p1 the right-hand operand
     */

    public GeneralComparison(Expression p0, int op, Expression p1) {
        super(p0, op, p1);
        singletonOperator = getSingletonOperator(op);
    }

    /**
     * Simplify a GeneralComparison expression
     * @param gc the GeneralComparison to be simplified
     * @param backwardsCompatible true if in 1.0 compatibility mode
     * @return the simplified expression
     */

    public static BinaryExpression simplifyGeneralComparison(GeneralComparison gc, boolean backwardsCompatible) {
        if (backwardsCompatible) {
            Expression[] operands = gc.getOperands();
            GeneralComparison10 gc10 = new GeneralComparison10(operands[0], gc.getOperator(), operands[1]);
            gc10.setAtomicComparer(gc.getAtomicComparer());
            return gc10;
        } else {
            Expression[] operands = gc.getOperands();
            GeneralComparison20 gc20 = new GeneralComparison20(operands[0], gc.getOperator(), operands[1]);
            gc20.setAtomicComparer(gc.getAtomicComparer());
            return gc20;
        }
    }

    /**
     * Set the comparer to be used
     * @param comparer the comparer to be used
     */

    public void setAtomicComparer(AtomicComparer comparer) {
        this.comparer = comparer;
    }

    @Override
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Expression e = super.simplify(visitor);
        if (e == this) {
            e = simplifyGeneralComparison(this, visitor.getStaticContext().isInBackwardsCompatibleMode());
        }
        ExpressionTool.copyLocationInfo(this, e);
        return e;
    }

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used
     */

    public AtomicComparer getAtomicComparer() {
        return comparer;
    }

    /**
     * Get the primitive (singleton) operator used: one of Token.FEQ, Token.FNE, Token.FLT, Token.FGT,
     * Token.FLE, Token.FGE
     */

    public int getSingletonOperator() {
        return singletonOperator;
    }

    /**
     * Determine the static cardinality. Returns [1..1]
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Type-check the expression
     *
     * @return the checked expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final Configuration config = visitor.getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);

        // If either operand is statically empty, return false

        if (Literal.isEmptySequence(operand0) || Literal.isEmptySequence(operand1)) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        // Neither operand needs to be sorted

        operand0 = ExpressionTool.unsorted(config, operand0, false);
        operand1 = ExpressionTool.unsorted(config, operand1, false);

        SequenceType atomicType = SequenceType.ATOMIC_SEQUENCE;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, false, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, false, role1, visitor);

        if (operand0 != oldOp0) {
            adoptChildExpression(operand0);
        }

        if (operand1 != oldOp1) {
            adoptChildExpression(operand1);
        }

        ItemType t0 = operand0.getItemType(th);  // this is always an atomic type or empty-sequence()
        ItemType t1 = operand1.getItemType(th);  // this is always an atomic type or empty-sequence()

        if (t0 instanceof EmptySequenceTest || t1 instanceof EmptySequenceTest) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        BuiltInAtomicType pt0 = (BuiltInAtomicType)t0.getPrimitiveItemType();
        BuiltInAtomicType pt1 = (BuiltInAtomicType)t1.getPrimitiveItemType();

        int c0 = operand0.getCardinality();
        int c1 = operand1.getCardinality();

        if (c0 == StaticProperty.EMPTY || c1 == StaticProperty.EMPTY) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        if (t0.equals(BuiltInAtomicType.ANY_ATOMIC) || t0.equals(BuiltInAtomicType.UNTYPED_ATOMIC) ||
                t1.equals(BuiltInAtomicType.ANY_ATOMIC) || t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            // then no static type checking is possible
        } else {

            if (!Type.isComparable(pt0, pt1, Token.isOrderedOperator(singletonOperator))) {
                typeError("Cannot compare " +
                        t0.toString() + " to " + t1.toString(), "XPTY0004", null);
            }
        }

        if (c0 == StaticProperty.EXACTLY_ONE &&
                c1 == StaticProperty.EXACTLY_ONE &&
                !t0.equals(BuiltInAtomicType.ANY_ATOMIC) &&
                !t1.equals(BuiltInAtomicType.ANY_ATOMIC)) {

            // Use a value comparison if both arguments are singletons, and if the comparison operator to
            // be used can be determined.

            Expression e0 = operand0;
            Expression e1 = operand1;

            if (t0.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                    e0 = new CastExpression(operand0, BuiltInAtomicType.STRING, false);
                    adoptChildExpression(e0);
                    e1 = new CastExpression(operand1, BuiltInAtomicType.STRING, false);
                    adoptChildExpression(e1);
                } else if (th.isSubType(t1, BuiltInAtomicType.NUMERIC)) {
                    e0 = new CastExpression(operand0, BuiltInAtomicType.DOUBLE, false);
                    adoptChildExpression(e0);
                } else {
                    e0 = new CastExpression(operand0, pt1, false);
                    adoptChildExpression(e0);
                }
            } else if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                if (th.isSubType(t0, BuiltInAtomicType.NUMERIC)) {
                    e1 = new CastExpression(operand1, BuiltInAtomicType.DOUBLE, false);
                    adoptChildExpression(e1);
                } else {
                    e1 = new CastExpression(operand1, pt0, false);
                    adoptChildExpression(e1);
                }
            }

            ValueComparison vc = new ValueComparison(e0, singletonOperator, e1);
            vc.setAtomicComparer(comparer);
            ExpressionTool.copyLocationInfo(this, vc);
            return visitor.typeCheck(visitor.simplify(vc), contextItemType);
        }

        StaticContext env = visitor.getStaticContext();
        if (comparer == null) {
            // In XSLT, only do this the first time through, otherwise default-collation may be missed
            final String defaultCollationName = env.getDefaultCollationName();
            StringCollator collation = env.getConfiguration().getNamedCollation(defaultCollationName);
            if (collation == null) {
                collation = CodepointCollator.getInstance();
            }
            comparer = GenericAtomicComparer.makeAtomicComparer(
                    pt0, pt1, collation, config.getConversionContext());
        }


        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
            return Literal.makeLiteral((AtomicValue)evaluateItem(env.makeEarlyEvaluationContext()));
        }

        return this;
    }

    private static Expression makeMinOrMax(Expression exp, String function) {
        FunctionCall fn = SystemFunction.makeSystemFunction(function, new Expression[]{exp});
        ((Minimax)fn).setIgnoreNaN(true);
        return fn;
    }

    /**
     * Optimize the expression
     *
     * @return the checked expression
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        Configuration config = visitor.getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();
        final StaticContext env = visitor.getStaticContext();

        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);

        // If either operand is statically empty, return false

        if (Literal.isEmptySequence(operand0) || Literal.isEmptySequence(operand1)) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        // Neither operand needs to be sorted

        operand0 = ExpressionTool.unsorted(config, operand0, false);
        operand1 = ExpressionTool.unsorted(config, operand1, false);

        if (operand0 instanceof Literal && operand1 instanceof Literal) {
            return new Literal(
                    Value.asValue(evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext())));
        }

        ItemType t0 = operand0.getItemType(th);
        ItemType t1 = operand1.getItemType(th);

        int c0 = operand0.getCardinality();
        int c1 = operand1.getCardinality();

        // if first argument is a singleton, reverse the arguments
        if (Cardinality.allowsMany(operand1.getCardinality()) && !Cardinality.allowsMany(operand0.getCardinality())) {
            GeneralComparison mc = getInverseComparison();
            ExpressionTool.copyLocationInfo(this, mc);
            mc.comparer = comparer;
            return visitor.optimize(mc, contextItemType);
        }

        // see if both arguments are singletons...
        if (c0 == StaticProperty.EXACTLY_ONE &&
                c1 == StaticProperty.EXACTLY_ONE &&
                !t0.equals(BuiltInAtomicType.ANY_ATOMIC) &&
                !t1.equals(BuiltInAtomicType.ANY_ATOMIC)) {

            // Use a value comparison if both arguments are singletons, and if the comparison operator to
            // be used can be determined.

            Expression e0 = operand0;
            Expression e1 = operand1;

            if (t0.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                    e0 = new CastExpression(operand0, BuiltInAtomicType.STRING, false);
                    adoptChildExpression(e0);
                    e1 = new CastExpression(operand1, BuiltInAtomicType.STRING, false);
                    adoptChildExpression(e1);
                } else if (th.isSubType(t1, BuiltInAtomicType.NUMERIC)) {
                    e0 = new CastExpression(operand0, BuiltInAtomicType.DOUBLE, false);
                    adoptChildExpression(e0);
                } else {
                    BuiltInAtomicType pt1 = (BuiltInAtomicType)t1.getPrimitiveItemType();
                    e0 = new CastExpression(operand0, pt1, false);
                    adoptChildExpression(e0);
                }
            } else if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                if (th.isSubType(t0, BuiltInAtomicType.NUMERIC)) {
                    e1 = new CastExpression(operand1, BuiltInAtomicType.DOUBLE, false);
                    adoptChildExpression(e1);
                } else {
                    BuiltInAtomicType pt0 = (BuiltInAtomicType)t0.getPrimitiveItemType();
                    e1 = new CastExpression(operand1, pt0, false);
                    adoptChildExpression(e1);
                }
            }

            ValueComparison vc = new ValueComparison(e0, singletonOperator, e1);
            vc.setAtomicComparer(comparer);
            ExpressionTool.copyLocationInfo(this, vc);
            return visitor.optimize(visitor.typeCheck(visitor.simplify(vc), contextItemType), contextItemType);
        }

        if (comparer == null) {
            final String defaultCollationName = env.getDefaultCollationName();
            StringCollator comp = config.getNamedCollation(defaultCollationName);
            if (comp == null) {
                comp = CodepointCollator.getInstance();
            }
            BuiltInAtomicType pt0 = (BuiltInAtomicType)t0.getPrimitiveItemType();
            BuiltInAtomicType pt1 = (BuiltInAtomicType)t1.getPrimitiveItemType();
            comparer = GenericAtomicComparer.makeAtomicComparer(pt0, pt1, comp,
                    config.getConversionContext());
        }

        // If one operand is numeric, then construct code
        // to force the other operand to numeric

        // TODO: shouldn't this happen during type checking?


        boolean numeric0 = th.isSubType(t0, BuiltInAtomicType.NUMERIC);
        boolean numeric1 = th.isSubType(t1, BuiltInAtomicType.NUMERIC);
        if (numeric1 && !numeric0) {
            RoleLocator role = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
            //role.setSourceLocator(this);
            operand0 = TypeChecker.staticTypeCheck(operand0, SequenceType.NUMERIC_SEQUENCE, false, role, visitor);
        }

        if (numeric0 && !numeric1) {
            RoleLocator role = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
            //role.setSourceLocator(this);
            operand1 = TypeChecker.staticTypeCheck(operand1, SequenceType.NUMERIC_SEQUENCE, false, role, visitor);
        }


        // look for (N to M = I)
        // First a variable range...

        if (operand0 instanceof RangeExpression &&
                th.isSubType(operand1.getItemType(th), BuiltInAtomicType.NUMERIC) &&
                operator == Token.EQUALS &&
                !Cardinality.allowsMany(operand1.getCardinality())) {
            Expression min = ((RangeExpression)operand0).operand0;
            Expression max = ((RangeExpression)operand0).operand1;
            IntegerRangeTest ir = new IntegerRangeTest(operand1, min, max);
            ExpressionTool.copyLocationInfo(this, ir);
            return ir;
        }

        // Now a fixed range...

        if (operand0 instanceof Literal) {
            Value value0 = ((Literal)operand0).getValue();
            if (value0 instanceof IntegerRange &&
                    th.isSubType(operand1.getItemType(th), BuiltInAtomicType.NUMERIC) &&
                    operator == Token.EQUALS &&
                    !Cardinality.allowsMany(operand1.getCardinality())) {
                long min = ((IntegerRange)value0).getStart();
                long max = ((IntegerRange)value0).getEnd();
                IntegerRangeTest ir = new IntegerRangeTest(operand1,
                        Literal.makeLiteral(new IntegerValue(new BigDecimal(min))),
                        Literal.makeLiteral(new IntegerValue(new BigDecimal(max))));
                ExpressionTool.copyLocationInfo(this, ir);
                return ir;
            }
        }

        // If the operator is gt, ge, lt, le then replace X < Y by min(X) < max(Y)

        // This optimization is done only in the case where at least one of the
        // sequences is known to be purely numeric. It isn't safe if both sequences
        // contain untyped atomic values, because in that case, the type of the
        // comparison isn't known in advance. For example [(1, U1) < ("fred", U2)]
        // involves both string and numeric comparisons.

        if (operator != Token.EQUALS && operator != Token.NE &&
                (th.isSubType(t0, BuiltInAtomicType.NUMERIC) || th.isSubType(t1, BuiltInAtomicType.NUMERIC))) {

            // System.err.println("** using minimax optimization **");
            ValueComparison vc;
            switch(operator) {
                case Token.LT:
                case Token.LE:
                    vc = new ValueComparison(makeMinOrMax(operand0, "min"),
                            singletonOperator,
                            makeMinOrMax(operand1, "max"));
                    vc.setResultWhenEmpty(BooleanValue.FALSE);
                    vc.setAtomicComparer(comparer);
                    break;
                case Token.GT:
                case Token.GE:
                    vc = new ValueComparison(makeMinOrMax(operand0, "max"),
                            singletonOperator,
                            makeMinOrMax(operand1, "min"));
                    vc.setResultWhenEmpty(BooleanValue.FALSE);
                    vc.setAtomicComparer(comparer);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown operator " + operator);
            }

            ExpressionTool.copyLocationInfo(this, vc);
            return visitor.typeCheck(vc, contextItemType);
        }



        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
            return Literal.makeLiteral((AtomicValue)evaluateItem(env.makeEarlyEvaluationContext()));
        }

        return this;
    }


    /**
     * Evaluate the expression in a given context
     *
     * @param context the given context for evaluation
     * @return a BooleanValue representing the result of the numeric comparison of the two operands
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
     * Evaluate the expression in a boolean context
     *
     * @param context the given context for evaluation
     * @return a boolean representing the result of the numeric comparison of the two operands
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {

        try {
            SequenceIterator iter1 = operand0.iterate(context);
            SequenceIterator iter2 = operand1.iterate(context);

            Value seq2 = (Value)SequenceExtent.makeSequenceExtent(iter2);
            // we choose seq2 because it's more likely to be a singleton
            int count2 = seq2.getLength();

            if (count2 == 0) {
                return false;
            }

            if (count2 == 1) {
                AtomicValue s2 = (AtomicValue)seq2.itemAt(0);
                while (true) {
                    AtomicValue s1 = (AtomicValue)iter1.next();
                    if (s1 == null) {
                        break;
                    }
                    if (compare(s1, singletonOperator, s2, comparer, true, context)) {
                        return true;
                    }
                }
                return false;
            }

            while (true) {
                AtomicValue s1 = (AtomicValue)iter1.next();
                if (s1 == null) {
                    break;
                }
                SequenceIterator e2 = seq2.iterate();
                while (true) {
                    AtomicValue s2 = (AtomicValue)e2.next();
                    if (s2 == null) {
                        break;
                    }
                    if (compare(s1, singletonOperator, s2, comparer, true, context)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(getSourceLocator());
            e.maybeSetContext(context);
            throw e;
        }

    }

    /**
     * Compare two atomic values
     * @param a1 the first value
     * @param operator the operator, for example {@link client.net.sf.saxon.ce.expr.Token#EQUALS}
     * @param a2 the second value
     * @param comparer the comparer to be used to perform the comparison
     * @param checkTypes set to true if the operand types need to be checked for comparability at runtime
     * @param context the XPath evaluation context @return true if the comparison succeeds
     */

    protected static boolean compare(AtomicValue a1,
                                     int operator,
                                     AtomicValue a2,
                                     AtomicComparer comparer,
                                     boolean checkTypes,
                                     XPathContext context) throws XPathException {

        boolean u1 = (a1 instanceof UntypedAtomicValue);
        boolean u2 = (a2 instanceof UntypedAtomicValue);
        if (u1 != u2) {
            // one value untyped, the other not
            if (u1) {
                // a1 is untyped atomic
                if (a2 instanceof NumericValue) {
                    a1 = a1.convert(BuiltInAtomicType.DOUBLE, true).asAtomic();
                } else {
                    a1 = a1.convert(a2.getPrimitiveType(), true).asAtomic();
                }
            } else {
                // a2 is untyped atomic
                if (a1 instanceof NumericValue) {
                    a2 = a2.convert(BuiltInAtomicType.DOUBLE, true).asAtomic();
                } else {
                    a2 = a2.convert(a1.getPrimitiveType(), true).asAtomic();
                }
            }
            checkTypes = false; // No further checking needed if conversion succeeded
        }
        return ValueComparison.compare(a1, operator, a2, comparer.provideContext(context), checkTypes);

    }

    /**
     * Determine the data type of the expression
     * @param th the type hierarchy cache
     * @return the value BuiltInAtomicType.BOOLEAN
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Return the singleton form of the comparison operator, e.g. FEQ for EQUALS, FGT for GT
     * @param op the many-to-many form of the operator, for example {@link Token#LE}
     * @return the corresponding singleton operator, for example {@link Token#FLE}
     */

    private static int getSingletonOperator(int op) {
        switch (op) {
            case Token.EQUALS:
                return Token.FEQ;
            case Token.GE:
                return Token.FGE;
            case Token.NE:
                return Token.FNE;
            case Token.LT:
                return Token.FLT;
            case Token.GT:
                return Token.FGT;
            case Token.LE:
                return Token.FLE;
            default:
                return op;
        }
    }

    protected GeneralComparison getInverseComparison() {
        return new GeneralComparison(operand1, Token.inverse(operator), operand0);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
