package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
* A NumericPromoter performs numeric promotion on each item in a supplied sequence.
 * There are two subclasses, to handle promotion to double and promotion to float
*/

public abstract class NumericPromoter extends UnaryExpression {

    public NumericPromoter(Expression exp) {
        super(exp);
    }

    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        return this;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemMappingFunction promoter = new ItemMappingFunction() {
            public Item mapItem(Item item) throws XPathException {
                return promote(((AtomicValue)item));
            }
        };
        return new ItemMappingIterator(base, promoter, true);
    }

    /**
    * Evaluate as an Item. This should only be called if the expression has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        return promote(((AtomicValue)item));
    }

    /**
     * Perform the promotion
     * @param value the numeric or untyped atomic value to be promoted
     * @return the value that results from the promotion
     */

    protected abstract AtomicValue promote(AtomicValue value) throws XPathException;


}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.