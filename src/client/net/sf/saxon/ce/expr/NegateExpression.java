package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.NumericValue;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.DoubleValue;

/**
 * Negate Expression: implements the unary minus operator.
 * This expression is initially created as an ArithmeticExpression (or in backwards
 * compatibility mode, an ArithmeticExpression10) to take advantage of the type checking code.
 * So we don't need to worry about type checking or argument conversion.
 */

public class NegateExpression extends UnaryExpression {

    private boolean backwardsCompatible;

    /**
     * Create a NegateExpression
     * @param base the expression that computes the value whose sign is to be reversed
     */

    public NegateExpression(Expression base) {
        super(base);
    }

    /**
     * Set whether the expression is to be evaluated in XPath 1.0 compatibility mode
     * @param compatible true if XPath 1.0 compatibility mode is enabled
     */

    public void setBackwardsCompatible(boolean compatible) {
        backwardsCompatible = compatible;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression oldop = operand;
        RoleLocator role = new RoleLocator(RoleLocator.UNARY_EXPR, "-", 0);
        operand = TypeChecker.staticTypeCheck(operand, SequenceType.OPTIONAL_NUMERIC, backwardsCompatible,
                role, visitor);
        operand = visitor.typeCheck(operand, contextItemType);
        if (operand != oldop) {
            adoptChildExpression(operand);
        }
        return this;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return operand.getItemType(th);
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        NumericValue v1 = (NumericValue) operand.evaluateItem(context);
        if (v1 == null) {
            return backwardsCompatible ? DoubleValue.NaN : null;
        }
        return v1.negate();
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.