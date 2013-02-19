package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod. Note that this code does not handle backwards
 * compatibility mode: see {@link ArithmeticExpression10}
 */

public class ArithmeticExpression extends BinaryExpression {

    private Calculator calculator;
    protected boolean simplified = false;

    /**
     * Create an arithmetic expression
     * @param p0 the first operand
     * @param operator the operator, for example {@link Token#PLUS}
     * @param p1 the second operand
     */

    public ArithmeticExpression(Expression p0, int operator, Expression p1) {
        super(p0, operator, p1);
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        if (simplified) {
            // Don't simplify more than once; because in XSLT the static context on subsequent calls
            // might not know whether backwards compatibility is in force or not
            return this;
        }
        simplified = true;
        Expression e = super.simplify(visitor);
        if (e == this && visitor.getStaticContext().isInBackwardsCompatibleMode()) {
            return new ArithmeticExpression10(operand0, operator, operand1);
        } else {
            if (operator == Token.NEGATE && Literal.isAtomic(operand1)) {
                // very early evaluation of expressions like "-1", so they are treated as numeric literals
                AtomicValue val = (AtomicValue)((Literal)operand1).getValue();
                if (val instanceof NumericValue) {
                    return new Literal(((NumericValue)val).negate());
                }
            }
            return e;
        }
    }

    /**
     * Get the calculator allocated to evaluate this expression
     * @return the calculator, a helper object that does the actual calculation
     */

    public Calculator getCalculator() {
        return calculator;
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);


        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, false, role0, visitor);
        final ItemType itemType0 = operand0.getItemType(th);
        if (itemType0 instanceof EmptySequenceTest) {
            return new Literal(EmptySequence.getInstance());
        }
        AtomicType type0 = (AtomicType) itemType0.getPrimitiveItemType();
        if (type0.getFingerprint() == StandardNames.XS_UNTYPED_ATOMIC) {
            operand0 = new UntypedAtomicConverter(operand0, BuiltInAtomicType.DOUBLE, true, role0);
            type0 = BuiltInAtomicType.DOUBLE;
        } else if (/*!(operand0 instanceof UntypedAtomicConverter)*/
                (operand0.getSpecialProperties()&StaticProperty.NOT_UNTYPED) == 0 &&
                th.relationship(type0, BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT) {
            operand0 = new UntypedAtomicConverter(operand0, BuiltInAtomicType.DOUBLE, false, role0);
            type0 = (AtomicType)operand0.getItemType(th);
        }

        // System.err.println("First operand"); operand0.display(10);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, false, role1, visitor);
        final ItemType itemType1 = operand1.getItemType(th);
        if (itemType1 instanceof EmptySequenceTest) {
            return new Literal(EmptySequence.getInstance());
        }
        AtomicType type1 = (AtomicType)itemType1.getPrimitiveItemType();
        if (type1.getFingerprint() == StandardNames.XS_UNTYPED_ATOMIC) {
            operand1 = new UntypedAtomicConverter(operand1, BuiltInAtomicType.DOUBLE, true, role1);
            type1 = BuiltInAtomicType.DOUBLE;
        } else if (/*!(operand1 instanceof UntypedAtomicConverter) &&*/
                (operand1.getSpecialProperties()&StaticProperty.NOT_UNTYPED) == 0 &&
                th.relationship(type1, BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT) {
            operand1 = new UntypedAtomicConverter(operand1, BuiltInAtomicType.DOUBLE, false, role1);
            type1 = (AtomicType)operand1.getItemType(th);
        }

        if (operand0 != oldOp0) {
            adoptChildExpression(operand0);
        }

        if (operand1 != oldOp1) {
            adoptChildExpression(operand1);
        }

        if (Literal.isEmptySequence(operand0) ||
                Literal.isEmptySequence(operand1)) {
            return new Literal(EmptySequence.getInstance());
        }

        if (operator == Token.NEGATE) {
            if (operand1 instanceof Literal && ((Literal)operand1).getValue() instanceof NumericValue) {
                NumericValue nv = (NumericValue)((Literal)operand1).getValue();
                return new Literal(nv.negate());
            } else {
                NegateExpression ne = new NegateExpression(operand1);
                ne.setBackwardsCompatible(false);
                return visitor.typeCheck(ne, contextItemType);
            }
        }

        // Get a calculator to implement the arithmetic operation. If the types are not yet specifically known,
        // we allow this to return an "ANY" calculator which defers the decision. However, we only allow this if
        // at least one of the operand types is AnyAtomicType or (otherwise unspecified) numeric.

        boolean mustResolve = !(type0.equals(BuiltInAtomicType.ANY_ATOMIC) || type1.equals(BuiltInAtomicType.ANY_ATOMIC)
                || type0.equals(BuiltInAtomicType.NUMERIC) || type1.equals(BuiltInAtomicType.NUMERIC));

        calculator = Calculator.getCalculator(
                type0.getFingerprint(), type1.getFingerprint(), mapOpCode(operator), mustResolve);

        if (calculator == null) {
            typeError("Arithmetic operator is not defined for arguments of types (" +
                    type0.getDisplayName() + ", " + type1.getDisplayName() + ")", "XPTY0004", null);
        }

        try {
            if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
                return new Literal(Value.asValue(evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext())));
            }
        } catch (XPathException err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time, or it might be due to context such as the implicit timezone
            // not being available yet
        }
        return this;
    }

    /**
     * Static method to apply arithmetic to two values
     * @param value0 the first value
     * @param operator the operator as denoted in the Calculator class, for example {@link Calculator#PLUS}
     * @param value1 the second value
     * @param context the XPath dynamic evaluation context
     * @return the result of the arithmetic operation
     */

    public static AtomicValue compute(AtomicValue value0, int operator, AtomicValue value1, XPathContext context)
            throws XPathException {
        int p0 = value0.getPrimitiveType().getFingerprint();
        int p1 = value1.getPrimitiveType().getFingerprint();
        Calculator calculator = Calculator.getCalculator(p0, p1, operator, false);
        return calculator.compute(value0, value1, context);
    }

    /**
     * Map operator codes from those in the Token class to those in the Calculator class
     * @param op an operator denoted by a constant in the {@link Token} class, for example {@link Token#PLUS}
     * @return an operator denoted by a constant defined in the {@link Calculator} class, for example
     * {@link Calculator#PLUS}
     */

    public static int mapOpCode(int op) {
        switch (op) {
            case Token.PLUS:
                return Calculator.PLUS;
            case Token.MINUS:
            case Token.NEGATE:
                return Calculator.MINUS;
            case Token.MULT:
                return Calculator.TIMES;
            case Token.DIV:
                return Calculator.DIV;
            case Token.IDIV:
                return Calculator.IDIV;
            case Token.MOD:
                return Calculator.MOD;
            default:
                throw new IllegalArgumentException();
        }

    }

    /**
     * Determine the data type of the expression, insofar as this is known statically
     * @param th the type hierarchy cache
     * @return the atomic type of the result of this arithmetic expression
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (calculator == null) {
            return BuiltInAtomicType.ANY_ATOMIC;  // type is not known statically
        } else {
            ItemType t1 = operand0.getItemType(th);
            if (!(t1 instanceof AtomicType)) {
                t1 = t1.getAtomizedItemType();
            }
            ItemType t2 = operand1.getItemType(th);
            if (!(t2 instanceof AtomicType)) {
                t2 = t2.getAtomizedItemType();
            }
            ItemType resultType = calculator.getResultType((AtomicType) t1.getPrimitiveItemType(),
                    (AtomicType) t2.getPrimitiveItemType());

            if (resultType.equals(BuiltInAtomicType.ANY_ATOMIC)) {
                // there are a few special cases where we can do better. For example, given X+1, where the type of X
                // is unknown, we can still infer that the result is numeric. (Not so for X*2, however, where it could
                // be a duration)
                if ((operator == Token.PLUS || operator == Token.MINUS) &&
                        (th.isSubType(t2, BuiltInAtomicType.NUMERIC) || th.isSubType(t1, BuiltInAtomicType.NUMERIC))) {
                    resultType = BuiltInAtomicType.NUMERIC;
                }
            }
            return resultType;
        }
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue v0 = (AtomicValue) operand0.evaluateItem(context);
        if (v0 == null) {
            return null;
        }

        AtomicValue v1 = (AtomicValue) operand1.evaluateItem(context);
        if (v1 == null) {
            return null;
        }

        try {
            return calculator.compute(v0, v1, context);
        } catch (XPathException e) {
            e.maybeSetLocation(getSourceLocator());
            e.maybeSetContext(context);
            throw e;
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.