package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;

/**
 * A DocumentSorter is an expression that sorts a sequence of nodes into
 * document order.
 */
public class DocumentSorter extends UnaryExpression {

    private NodeOrderComparer comparer;

    public DocumentSorter(Expression base) {
        super(base);
        int props = base.getSpecialProperties();
        if (((props & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) ||
                (props & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) {
            comparer = LocalOrderComparer.getInstance();
        } else {
            comparer = GlobalOrderComparer.getInstance();
        }
    }

    public NodeOrderComparer getComparer() {
         return comparer;
    }  

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if ((operand.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
            // this can happen as a result of further simplification
            return operand;
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        if ((operand.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
            // this can happen as a result of further simplification
            return operand;
        }
        if (operand instanceof PathExpression) {
            return visitor.getConfiguration().getOptimizer().makeConditionalDocumentSorter(
                    this, (PathExpression)operand);
        }
        return this;
    }


    public int computeSpecialProperties() {
        return operand.getSpecialProperties() | StaticProperty.ORDERED_NODESET;
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            operand = doPromotion(operand, offer);
            return this;
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        //System.err.println("** SORTING **");
        return new DocumentOrderIterator(operand.iterate(context), comparer);
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return operand.effectiveBooleanValue(context);
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.