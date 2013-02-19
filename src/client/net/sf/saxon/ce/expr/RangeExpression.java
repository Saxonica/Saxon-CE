package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.ListIterator;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(
                operand0, SequenceType.OPTIONAL_INTEGER, backCompat, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(
                operand1, SequenceType.OPTIONAL_INTEGER, backCompat, role1, visitor);

        return makeConstantRange();
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
        return makeConstantRange();

    }

    private Expression makeConstantRange() throws XPathException {
        if (operand0 instanceof Literal && operand1 instanceof Literal) {
            Value v0 = ((Literal)operand0).getValue();
            Value v1 = ((Literal)operand1).getValue();
            if (v0 instanceof IntegerValue && v1 instanceof IntegerValue &&
                    ((IntegerValue) v0).getDecimalValue().compareTo(DecimalValue.BIG_DECIMAL_MAX_INT) < 0 &&
                    ((IntegerValue) v0).getDecimalValue().compareTo(DecimalValue.BIG_DECIMAL_MIN_INT) > 0 &&
                    ((IntegerValue) v1).getDecimalValue().compareTo(DecimalValue.BIG_DECIMAL_MAX_INT) < 0 &&
                    ((IntegerValue) v1).getDecimalValue().compareTo(DecimalValue.BIG_DECIMAL_MIN_INT) > 0) {
                int i0 = ((IntegerValue)v0).intValue();
                int i1 = ((IntegerValue)v1).intValue();
                Literal result;
                if (i0 > i1) {
                    result = Literal.makeEmptySequence();
                } else if (i0 == i1) {
                    result = Literal.makeLiteral(new IntegerValue(new BigDecimal(i0)));
                } else {
                    result = Literal.makeLiteral(new IntegerRange(i0, i1));
                }
                ExpressionTool.copyLocationInfo(this, result);
                return result;
            }
        }
        return this;
    }


    /**
    * Get the data type of the items returned
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.INTEGER;
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
        try {
            return new RangeIterator(v1.intValue(), v2.intValue());
        } catch (XPathException err) {
            // values out of range for int; not much hope, but we'll try the hard way
            BigDecimal ds = v1.getDecimalValue();
            BigDecimal de = v2.getDecimalValue();
            List<IntegerValue> list = new ArrayList<IntegerValue>();
            do {
                list.add(new IntegerValue(ds));
                ds = ds.add(BigDecimal.ONE);
            } while (ds.compareTo(de) <= 0);
            return new ListIterator(list);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.