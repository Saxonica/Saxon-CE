package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.functions.NumberFn;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.*;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod, in backwards
 * compatibility mode: see {@link ArithmeticExpression} for the non-backwards
 * compatible case.
 */

public class ArithmeticExpression10 extends BinaryExpression {

    /**
     * Create an arithmetic expression to be evaluated in XPath 1.0 mode
     * @param p0 the first operand
     * @param operator the operator, for example {@link Token#PLUS}
     * @param p1 the second operand
     */

    public ArithmeticExpression10(Expression p0, int operator, Expression p1) {
        super(p0, operator, p1);
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        Expression e2 = super.typeCheck(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }

        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, true, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, true, role1, visitor);

        ItemType itemType0 = operand0.getItemType();
        if (itemType0 instanceof EmptySequenceTest) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }

        ItemType itemType1 = operand1.getItemType();
        if (itemType1 instanceof EmptySequenceTest) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }

        operand0 = createConversionCode(operand0);
        operand1 = createConversionCode(operand1);

        adoptChildExpression(operand0);
        adoptChildExpression(operand1);

        if (operator == Token.NEGATE) {
            if (operand1 instanceof Literal) {
                Value v = ((Literal)operand1).getValue();
                if (v instanceof NumericValue) {
                    return Literal.makeLiteral(((NumericValue)v).negate());
                }
            }
            NegateExpression ne = new NegateExpression(operand1);
            ne.setBackwardsCompatible(true);
            return visitor.typeCheck(ne, contextItemType);
        }

        return this;
    }


    private Expression createConversionCode(Expression operand) {
        if (Cardinality.allowsMany(operand.getCardinality())) {
            FirstItemExpression fie = new FirstItemExpression(operand);
            ExpressionTool.copyLocationInfo(this, fie);
            operand = fie;
        }
        return operand;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     */

    public ItemType getItemType() {
        return BuiltInAtomicType.ANY_ATOMIC;  // type is not known statically
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue v1 = (AtomicValue) operand0.evaluateItem(context);
        v1 = convertOperand(v1);

        AtomicValue v2 = (AtomicValue) operand1.evaluateItem(context);
        v2 = convertOperand(v2);

        return ArithmeticExpression.compute(v1, operator, v2, context);
    }

    private DoubleValue convertOperand(AtomicValue value) throws XPathException {
        if (value == null) {
            return DoubleValue.NaN;
        }
        if (value instanceof DoubleValue) {
            return (DoubleValue)value;
        }
        BuiltInAtomicType type = value.getItemType();
        if (type == BuiltInAtomicType.INTEGER || type == BuiltInAtomicType.UNTYPED_ATOMIC ||
                type == BuiltInAtomicType.DECIMAL || type == BuiltInAtomicType.FLOAT ||
                type == BuiltInAtomicType.BOOLEAN || type == BuiltInAtomicType.STRING) {
            return NumberFn.convert(value);
        } else {
            throw new XPathException("Invalid operand type for arithmetic: " + type, "XPTY0004");
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.