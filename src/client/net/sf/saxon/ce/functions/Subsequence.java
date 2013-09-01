package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;

/**
* Implements the XPath 2.0 subsequence()  function
*/


public class Subsequence extends SystemFunction {

    public Subsequence newInstance() {
        return new Subsequence();
    }

    /**
    * Determine the data type of the items in the sequence
    * @return the type of the argument
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return argument[0].getItemType(th);
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-significant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        return argument[0].getSpecialProperties();
    }


    /**
     * Determine the cardinality of the function.
     */

    public int computeCardinality() {
        if (getNumberOfArguments() == 3 && Literal.isConstantOne(argument[2])) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
        return argument[0].getCardinality() | StaticProperty.ALLOWS_ZERO;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        return this;
    }

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator seq = argument[0].iterate(context);
        DoubleValue startVal = (DoubleValue)argument[1].evaluateItem(context);
        if (startVal.isNaN()) {
            return EmptyIterator.getInstance();
        }
        if (startVal.compareTo(IntegerValue.MAX_LONG) > 0) {
            return EmptyIterator.getInstance();
        }
        startVal = (DoubleValue)startVal.round();
        int lstart;
        if (startVal.compareTo(IntegerValue.PLUS_ONE) <= 0) {
            lstart = 1;
        } else {
            lstart = startVal.intValue();
        }

        int lend;
        if (argument.length == 2) {
            lend = Integer.MAX_VALUE;
        } else {
            DoubleValue lengthVal = (DoubleValue)argument[2].evaluateItem(context);
            if (lengthVal.isNaN()) {
                return EmptyIterator.getInstance();
            }
            lengthVal = (DoubleValue)lengthVal.round();

            if (lengthVal.compareTo(IntegerValue.ZERO) <= 0) {
                return EmptyIterator.getInstance();
            }

            NumericValue rend = (NumericValue)ArithmeticExpression.compute(
                    startVal, Token.PLUS, lengthVal, context);
            if (rend.isNaN()) {
                // Can happen when start = -INF, length = +INF
                return EmptyIterator.getInstance();
            }
            rend = (NumericValue)ArithmeticExpression.compute(
                    rend, Token.MINUS, IntegerValue.PLUS_ONE, context);
            if (rend.compareTo(IntegerValue.ZERO) <= 0) {
                return EmptyIterator.getInstance();
            }
            lend = rend.intValue();
        }
        return SubsequenceIterator.make(seq, lstart, lend);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
