package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;

import java.math.BigDecimal;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod. Note that this code does not handle backwards
 * compatibility mode: see {@link ArithmeticExpression10}
 */

public class ArithmeticExpression extends BinaryExpression {

    protected boolean simplified = false;

    /**
     * Create an arithmetic expression
     *
     * @param p0       the first operand
     * @param operator the operator, for example {@link Token#PLUS}
     * @param p1       the second operand
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
                AtomicValue val = (AtomicValue) ((Literal) operand1).getValue();
                if (val instanceof NumericValue) {
                    return new Literal(((NumericValue) val).negate());
                }
            }
            return e;
        }
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = TypeHierarchy.getInstance();

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);


        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, false, role0);
        final ItemType itemType0 = operand0.getItemType();
        if (itemType0 instanceof EmptySequenceTest) {
            return new Literal(EmptySequence.getInstance());
        }
        AtomicType type0 = (AtomicType) itemType0.getPrimitiveItemType();
        if (type0 == AtomicType.UNTYPED_ATOMIC) {
            operand0 = new UntypedAtomicConverter(operand0, AtomicType.DOUBLE, true, role0);
        } else if (/*!(operand0 instanceof UntypedAtomicConverter)*/
                (operand0.getSpecialProperties() & StaticProperty.NOT_UNTYPED) == 0 &&
                        th.relationship(type0, AtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT) {
            operand0 = new UntypedAtomicConverter(operand0, AtomicType.DOUBLE, false, role0);
        }

        // System.err.println("First operand"); operand0.display(10);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, false, role1);
        final ItemType itemType1 = operand1.getItemType();
        if (itemType1 instanceof EmptySequenceTest) {
            return new Literal(EmptySequence.getInstance());
        }
        AtomicType type1 = (AtomicType) itemType1.getPrimitiveItemType();
        if (type1 == AtomicType.UNTYPED_ATOMIC) {
            operand1 = new UntypedAtomicConverter(operand1, AtomicType.DOUBLE, true, role1);
        } else if (/*!(operand1 instanceof UntypedAtomicConverter) &&*/
                (operand1.getSpecialProperties() & StaticProperty.NOT_UNTYPED) == 0 &&
                        th.relationship(type1, AtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT) {
            operand1 = new UntypedAtomicConverter(operand1, AtomicType.DOUBLE, false, role1);
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
            if (operand1 instanceof Literal && ((Literal) operand1).getValue() instanceof NumericValue) {
                NumericValue nv = (NumericValue) ((Literal) operand1).getValue();
                return new Literal(nv.negate());
            } else {
                NegateExpression ne = new NegateExpression(operand1);
                ne.setBackwardsCompatible(false);
                return visitor.typeCheck(ne, contextItemType);
            }
        }

        try {
            if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
                return new Literal(evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext()));
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
     *
     * @param value0   the first value
     * @param operator the operator as denoted in the Token class, for example {@link Token#PLUS}
     * @param value1   the second value
     * @param context  the XPath dynamic evaluation context
     * @return the result of the arithmetic operation
     */

    public static AtomicValue compute(AtomicValue value0, int operator, AtomicValue value1, XPathContext context)
            throws XPathException {
        AtomicType p0 = value0.getItemType();
        AtomicType p1 = value1.getItemType();
        TypeHierarchy th = TypeHierarchy.getInstance();

        if (p0 == AtomicType.UNTYPED_ATOMIC) {
            p0 = AtomicType.DOUBLE;
            value0 = value0.convert(AtomicType.DOUBLE).asAtomic();
        }
        if (p1 == AtomicType.UNTYPED_ATOMIC) {
            p1 = AtomicType.DOUBLE;
            value1 = value1.convert(AtomicType.DOUBLE).asAtomic();
        }
        if (p0 == AtomicType.DATE || p0 == AtomicType.TIME) {
            p0 = AtomicType.DATE_TIME;
        }
        if (p1 == AtomicType.DATE || p1 == AtomicType.TIME) {
            p1 = AtomicType.DATE_TIME;
        }

        if (value0 instanceof NumericValue && value1 instanceof NumericValue) {
            NumericValue n0 = (NumericValue) value0;
            NumericValue n1 = (NumericValue) value1;
            if (value0 instanceof DoubleValue || value1 instanceof DoubleValue) {
                double d0 = n0.getDoubleValue();
                double d1 = n1.getDoubleValue();
                double result;
                switch (operator) {
                    case Token.PLUS:
                    default:
                        result = d0 + d1;
                        break;
                    case Token.MINUS:
                        result = d0 - d1;
                        break;
                    case Token.MULT:
                        result = d0 * d1;
                        break;
                    case Token.DIV:
                        result = d0 / d1;
                        break;
                    case Token.MOD:
                        result = d0 % d1;
                        break;
                    case Token.IDIV:
                        if (d1 == 0.0) {
                            throw new XPathException("Integer division by zero", "FOAR0001");
                        }
                        if (Double.isNaN(d0) || Double.isInfinite(d0)) {
                            throw new XPathException("First operand of idiv is NaN or infinity", "FOAR0002");
                        }
                        if (Double.isNaN(d1)) {
                            throw new XPathException("Second operand of idiv is NaN", "FOAR0002");
                        }
                        return new DoubleValue(d0 / d1).convert(AtomicType.INTEGER).asAtomic();

                }
                return new DoubleValue(result);
            } else if (value0 instanceof FloatValue || value1 instanceof FloatValue) {
                float f0 = n0.getFloatValue();
                float f1 = n1.getFloatValue();
                float result;
                switch (operator) {
                    case Token.PLUS:
                    default:
                        result = f0 + f1;
                        break;
                    case Token.MINUS:
                        result = f0 - f1;
                        break;
                    case Token.MULT:
                        result = f0 * f1;
                        break;
                    case Token.DIV:
                        result = f0 / f1;
                        break;
                    case Token.MOD:
                        result = f0 % f1;
                        break;
                    case Token.IDIV:
                        if (f1 == 0.0) {
                            throw new XPathException("Integer division by zero", "FOAR0001");
                        }
                        if (Float.isNaN(f0) || Float.isInfinite(f0)) {
                            throw new XPathException("First operand of idiv is NaN or infinity", "FOAR0002");
                        }
                        if (Float.isNaN(f1)) {
                            throw new XPathException("Second operand of idiv is NaN", "FOAR0002");
                        }
                        return new FloatValue(f0 / f1).convert(AtomicType.INTEGER).asAtomic();

                }
                return new FloatValue(result);
            } else {
                BigDecimal d0 = n0.getDecimalValue();
                BigDecimal d1 = n1.getDecimalValue();
                BigDecimal result;
                switch (operator) {
                    case Token.PLUS:
                    default:
                        result = d0.add(d1);
                        break;
                    case Token.MINUS:
                        result = d0.subtract(d1);
                        break;
                    case Token.MULT:
                        result = d0.multiply(d1);
                        break;
                    case Token.DIV:
                        BigDecimal result1;
                        int scale = Math.max(DecimalValue.DIVIDE_PRECISION,
                                        Math.max(d0.scale(), d1.scale()));
                        try {
                            result1 = d0.divide(d1, scale, BigDecimal.ROUND_HALF_DOWN);
                        } catch (ArithmeticException err1) {
                            if (d1.signum() == 0) {
                                throw new XPathException("Decimal divide by zero", "FOAR0001");
                            } else {
                                throw err1;
                            }
                        }
                        return new DecimalValue(result1);
                    case Token.MOD:
                        try {
                            result = d0.remainder(d1);
                        } catch (ArithmeticException err) {
                            if (n1.compareTo(0) == 0) {
                                throw new XPathException("Decimal modulo zero", "FOAR0001");
                            } else {
                                throw err;
                            }
                        }
                        break;
                    case Token.IDIV:
                        if (d1.signum() == 0) {
                            throw new XPathException("Integer division by zero", "FOAR0001");
                        }
                        BigDecimal quot = d0.divideToIntegralValue(d1);
                        return IntegerValue.decimalToInteger(quot).asAtomic();

                }
                if (n0 instanceof IntegerValue && n0 instanceof IntegerValue) {
                    return new IntegerValue(result);
                } else {
                    return new DecimalValue(result);
                }
            }

        } else {
            // computations involving dates, times, and durations

            if (p0 == AtomicType.DATE_TIME) {
                if (p1 == AtomicType.DATE_TIME && operator == Token.MINUS) {
                    return ((CalendarValue)value0).subtract((CalendarValue)value1, context);
                } else if (th.isSubType(p1, AtomicType.DURATION) && (operator == Token.PLUS || operator == Token.MINUS)) {
                    DurationValue b = (DurationValue) value1;
                    if (operator == Token.MINUS) {
                        b = b.multiply(-1.0);
                    }
                    return ((CalendarValue) value0).add(b);
                }
            } else if (th.isSubType(p0, AtomicType.DURATION)) {
                if (th.isSubType(p1, AtomicType.DURATION)) {
                    DurationValue d0 = (DurationValue) value1;
                    DurationValue d1 = (DurationValue) value1;
                    switch (operator) {
                        case Token.PLUS:
                            return d0.add(d1);
                        case Token.MINUS:
                            return d0.add(d1.negate());
                        case Token.DIV:
                            return d0.divide(d1);
                    }
                } else if (p1 == AtomicType.DATE_TIME && operator == Token.PLUS) {
                    return ((CalendarValue) value1).add((DurationValue) value0);
                } else if (th.isSubType(p1, AtomicType.NUMERIC) && (operator == Token.MULT || operator == Token.DIV)) {
                    double d1 = ((NumericValue) value1).getDoubleValue();
                    if (operator == Token.DIV) {
                        d1 = 1.0 / d1;
                    }
                    return ((DurationValue) value0).multiply(d1);
                }
            } else if (th.isSubType(p0, AtomicType.NUMERIC) &&
                    th.isSubType(p1, AtomicType.DURATION) &&
                    operator == Token.MULT) {
                return ((DurationValue) value1).multiply(((NumericValue) value0).getDoubleValue());
            }
        }
        throw new XPathException("Undefined arithmetic operation: " + p0 + " " + Token.tokens[operator] + " " + p1, "XPTY0004");
    }

    /**
     * Determine the data type of the expression, insofar as this is known statically
     *
     * @return the atomic type of the result of this arithmetic expression
     */

    public ItemType getItemType() {
        ItemType t1 = operand0.getItemType();
        if (!(t1 instanceof AtomicType)) {
            t1 = t1.getAtomizedItemType();
        }
        ItemType t2 = operand1.getItemType();
        if (!(t2 instanceof AtomicType)) {
            t2 = t2.getAtomizedItemType();
        }
        TypeHierarchy th = TypeHierarchy.getInstance();
        boolean numeric1 = th.isSubType(t1, AtomicType.NUMERIC);
        boolean numeric2 = th.isSubType(t2, AtomicType.NUMERIC);
        if (numeric1 && numeric2) {
            return AtomicType.NUMERIC;
        } else if ((numeric1 || numeric2) && (operator == Token.PLUS || operator == Token.MINUS)) {
            // this case is important for filter expressions such as $a[$i+1]
            return AtomicType.NUMERIC;
        } else {
            return AtomicType.ANY_ATOMIC;
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
            return compute(v0, operator, v1, context);
        } catch (XPathException e) {
            e.maybeSetLocation(getSourceLocator());
            throw e;
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.