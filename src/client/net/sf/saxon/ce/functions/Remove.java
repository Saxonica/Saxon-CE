package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
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
     */

    public ItemType getItemType() {
        return argument[0].getItemType();
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
        ItemMappingFunction function = new RemoveMappingFunction(pos);
        return new ItemMappingIterator(seq, function);
    }

    /**
     * Mapping function to return the item unchanged except for the item at the specified position
     */

    public static class RemoveMappingFunction implements ItemMappingFunction, StatefulMappingFunction {

        private int position = 1;
        private int removeIndex;

        public RemoveMappingFunction(int removeIndex) {
            this.removeIndex = removeIndex;
        }

        /**
         * Map one item to another item.
         *
         * @param item The input item to be mapped.
         * @return either the output item, or null.
         */
        public Item mapItem(Item item) throws XPathException {
            return (position++ == removeIndex ? null : item);
        }

        /**
         * Return a clone of this MappingFunction, with the state reset to its state at the beginning
         * of the underlying iteration
         *
         * @return a clone of this MappingFunction
         * @param newBaseIterator
         */
        public StatefulMappingFunction getAnother(SequenceIterator newBaseIterator) {
            return new RemoveMappingFunction(removeIndex);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
