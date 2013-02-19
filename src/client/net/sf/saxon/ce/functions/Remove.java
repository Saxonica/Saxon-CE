package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.NumericValue;
import client.net.sf.saxon.ce.value.IntegerValue;

/**
* The XPath 2.0 remove() function
*/


public class Remove extends SystemFunction {

    public Remove newInstance() {
        return new Remove();
    }

    /**
     * Simplify. Recognize remove(seq, 1) as a TailExpression.
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Expression exp = super.simplify(visitor);
        if (exp instanceof Remove) {
            return ((Remove)exp).simplifyAsTailExpression();
        } else {
            return exp;
        }
    }

    /**
     * Simplify. Recognize remove(seq, 1) as a TailExpression. This
     * is worth doing because tail expressions used in a recursive call
     * are handled specially.
     */

    private Expression simplifyAsTailExpression() {
        if (Literal.isAtomic(argument[1])) {
            try {
                long value = ((IntegerValue)((Literal)argument[1]).getValue()).intValue();
                if (value <= 0) {
                    return argument[0];
                } else if (value == 1) {
                    Expression t = SystemFunction.makeSystemFunction("subsequence",
                            new Expression[]{argument[0], new Literal(IntegerValue.TWO)});
                    ExpressionTool.copyLocationInfo(this, t);
                    return t;
                }
            } catch (XPathException err) {
                return this;
            }
        }    
        return this;
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
        if (e == this) {
            return simplifyAsTailExpression();
        }
        return e;
    }

    /**
    * Determine the data type of the items in the sequence
    * @return the type of the input sequence
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return argument[0].getItemType(th);
    }

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue n0 = (AtomicValue)argument[1].evaluateItem(context);
        NumericValue n = (NumericValue)n0;
        int pos = (int)n.intValue();
        if (pos < 1) {
            return seq;
        }
        return new RemoveIterator(seq, pos);
    }

    /**
     * An implementation of SequenceIterator that returns all items except the one
     * at a specified position.
     */

    public static class RemoveIterator implements SequenceIterator, LastPositionFinder {

        SequenceIterator base;
        int removePosition;
        int position = 0;
        Item current = null;

        public RemoveIterator(SequenceIterator base, int removePosition) {
            this.base = base;
            this.removePosition = removePosition;
        }

        public Item next() throws XPathException {
            current = base.next();
            if (current != null && base.position() == removePosition) {
                current = base.next();
            }
            if (current == null) {
                position = -1;
            } else {
                position++;
            }
            return current;
        }

        public Item current() {
            return current;
        }

        public int position() {
            return position;
        }

        /**
         * Get the last position (that is, the number of items in the sequence). This method is
         * non-destructive: it does not change the state of the iterator.
         * The result is undefined if the next() method of the iterator has already returned null.
         */

        public int getLastPosition() throws XPathException {
            if (base instanceof LastPositionFinder) {
                int x = ((LastPositionFinder)base).getLastPosition();
                if (removePosition >= 1 && removePosition <= x) {
                    return x - 1;
                } else {
                    return x;
                }
            } else {
                // This shouldn't happen, because this iterator only has the LAST_POSITION_FINDER property
                // if the base iterator has the LAST_POSITION_FINDER property
                throw new AssertionError("base of removeIterator is not a LastPositionFinder");
            }
        }

        public SequenceIterator getAnother() throws XPathException {
            return new RemoveIterator(  base.getAnother(),
                                        removePosition);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link SequenceIterator#GROUNDED}, {@link SequenceIterator#LAST_POSITION_FINDER},
         *         and {@link SequenceIterator#LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return base.getProperties() & LAST_POSITION_FINDER;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
