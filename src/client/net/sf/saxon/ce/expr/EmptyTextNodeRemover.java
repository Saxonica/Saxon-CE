package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;

/**
 * This class performs the first phase of processing in "constructing simple content":
 * it takes an input sequence, eliminates empty text nodes, and combines adjacent text nodes
 * into one.
 * @since 9.3
 */
public class EmptyTextNodeRemover extends UnaryExpression {

    public EmptyTextNodeRemover(Expression p0) {
        super(p0);
    }

    /**
     * Determine the data type of the expression, if possible. The default
     * implementation for unary expressions returns the item type of the operand
     * @param th the type hierarchy cache
     * @return the item type of the items in the result sequence, insofar as this
     *         is known statically.
     */

    @Override
    public ItemType getItemType(TypeHierarchy th) {
        return getBaseExpression().getItemType(th);
    }

    @Override
    public int computeCardinality() {
        return getBaseExpression().getCardinality() | StaticProperty.ALLOWS_ZERO;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return Expression.ITERATE_METHOD;
    }    

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    @Override
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        ItemMappingFunction map = new ItemMappingFunction() {
             public Item mapItem(Item item) throws XPathException {
                if (item instanceof NodeInfo &&
                        ((NodeInfo)item).getNodeKind() == Type.TEXT &&
                        item.getStringValue().length() == 0) {
                     return null;
                } else {
                    return item;
                }
             }
        };
        return new ItemMappingIterator(getBaseExpression().iterate(context), map);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.



