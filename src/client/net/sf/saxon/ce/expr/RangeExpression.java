package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.SteppingIterator;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.*;

/**
* A RangeExpression is an expression that represents an integer sequence as
* a pair of end-points (for example "x to y").
* If the end-points are equal, the sequence is of length one.
 * <p>From Saxon 7.8, the sequence must be ascending; if the end-point is less
 * than the start-point, an empty sequence is returned. This is to allow
 * expressions of the form "for $i in 1 to count($seq) return ...." </p>
*/

public class RangeExpression extends BinaryExpression {

    /**
     * Construct a RangeExpression
     * @param start expression that computes the start of the range
     * @param op represents the operator "to", needed only because this class is a subclass of
     * BinaryExpression which needs an operator
     * @param end expression that computes the end of the range
    */

    public RangeExpression(Expression start, int op, Expression end) {
        super(start, op, end);
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);

        boolean backCompat = visitor.getStaticContext().isInBackwardsCompatibleMode();
        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 0);
        operand0 = TypeChecker.staticTypeCheck(
                operand0, SequenceType.OPTIONAL_INTEGER, backCompat, role0);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 1);
        operand1 = TypeChecker.staticTypeCheck(
                operand1, SequenceType.OPTIONAL_INTEGER, backCompat, role1);

        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);
        return this;
    }

    /**
    * Get the data type of the items returned
     */

    public ItemType getItemType() {
        return AtomicType.INTEGER;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }


    /**
    * Return an iteration over the sequence
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
        if (av1 == null) {
            return EmptyIterator.getInstance();
        }
        NumericValue v1 = (NumericValue)av1;

        AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
        if (av2 == null) {
            return EmptyIterator.getInstance();
        }
        NumericValue v2 = (NumericValue)av2;

        if (v1.compareTo(v2) > 0) {
            return EmptyIterator.getInstance();
        }

        return new SteppingIterator(v1, new RangeSteppingFunction(v2.intValue()), true);

    }

    /**
     * Function used by SteppingIterator to compute the next value in the sequence
     */

    private static class RangeSteppingFunction implements SteppingIterator.SteppingFunction {
        private int limit;

        public RangeSteppingFunction(int limit) {
            this.limit = limit;
        }
        public Item step(Item current) {
            try {
                int curr = ((IntegerValue)current).intValue();
                return (curr >= limit ? null : new IntegerValue(curr+1));
            } catch (XPathException e) {
                throw new AssertionError(e);
            }
        }

        public boolean conforms(Item current) {
            return true;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.