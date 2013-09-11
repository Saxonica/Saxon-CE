package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.Block;
import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.expr.instruct.ValueOf;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.Orphan;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Cardinality;

/**
 * This class performs the first phase of processing in "constructing simple content":
 * it takes an input sequence, eliminates empty text nodes, and combines adjacent text nodes
 * into one.
 * @since 9.3
 */
public class AdjacentTextNodeMerger extends UnaryExpression {

    public AdjacentTextNodeMerger(Expression p0) {
        super(p0);
    }

    @Override
    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        // This wrapper expression is unnecessary if the base expression cannot return text nodes,
        // or if it can return at most one item
        TypeHierarchy th = TypeHierarchy.getInstance();
        if (th.relationship(getBaseExpression().getItemType(), NodeKindTest.TEXT) == TypeHierarchy.DISJOINT) {
            return getBaseExpression();
        }
        if (!Cardinality.allowsMany(getBaseExpression().getCardinality())) {
            return getBaseExpression();
        }
        // In a choose expression, we can push the wrapper down to the action branches (whence it may disappear)
        if (getBaseExpression() instanceof Choose) {
            Choose choose = (Choose) getBaseExpression();
            Expression[] actions = choose.getActions();
            for (int i=0; i<actions.length; i++) {
                AdjacentTextNodeMerger atm2 = new AdjacentTextNodeMerger(actions[i]);
                actions[i] = atm2.typeCheck(visitor, contextItemType);
            }
            return choose;
        }
        // In a Block expression, check whether adjacent text nodes can occur (used in test strmode089)
        // Code deleted:
        if (getBaseExpression() instanceof Block) {
            Block block = (Block) getBaseExpression();
            Expression[] actions = block.getChildren();
            boolean prevtext = false;
            boolean needed = false;
            boolean maybeEmpty = false;
            for (int i=0; i<actions.length; i++) {
                boolean maybetext;
                if (actions[i] instanceof ValueOf) {
                    maybetext = true;
                    Expression content = ((ValueOf)actions[i]).getContentExpression();
                    if (content instanceof StringLiteral) {
                        // if it's empty, we could remove it now, but that's awkward and probably doesn't happen
                        maybeEmpty |= ((StringLiteral)content).getStringValue().length() == 0;
                    } else {
                        maybeEmpty = true;
                    }
                } else {
                    maybetext = th.relationship(actions[i].getItemType(), NodeKindTest.TEXT) != TypeHierarchy.DISJOINT;
                    maybeEmpty |= maybetext;
                }
                if (prevtext && maybetext) {
                    needed = true;
                    break; // may contain adjacent text nodes
                }
                if (maybetext && Cardinality.allowsMany(actions[i].getCardinality())) {
                    needed = true;
                    break; // may contain adjacent text nodes
                }
                prevtext = maybetext;
            }
            if (!needed) {
                // We don't need to merge adjacent text nodes, we only need to remove empty ones.
                if (maybeEmpty) {
                    return new EmptyTextNodeRemover(block);
                } else {
                    return block;
                }
            }
        }
        return this;
    }

    /**
     * Determine the data type of the expression, if possible. The default
     * implementation for unary expressions returns the item type of the operand
     * @return the item type of the items in the result sequence, insofar as this
     *         is known statically.
     */

    @Override
    public ItemType getItemType() {
        return getBaseExpression().getItemType();
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
        return Expression.PROCESS_METHOD | Expression.ITERATE_METHOD;
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
        return new AdjacentTextNodeMergingIterator(getBaseExpression().iterate(context));
    }

    /**
     * AdjacentTextNodeMergingIterator is an iterator that eliminates zero-length text nodes
     * and merges adjacent text nodes from the underlying iterator
     */

    public static class AdjacentTextNodeMergingIterator implements SequenceIterator {
        private SequenceIterator base;
        private Item current;
        private Item next;
        private int position = 0;

        public AdjacentTextNodeMergingIterator(SequenceIterator base) throws XPathException {
            this.base = base;
            next = base.next();
        }

        public Item next() throws XPathException {
            current = next;
            if (current == null) {
                position = -1;
                return null;
            }
            next = base.next();

            if (isTextNode(current)) {
                FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.MEDIUM);
                fsb.append(current.getStringValue());
                while (next != null && isTextNode(next)) {
                    fsb.append(next.getStringValue());
                    next = base.next();
                }
                if (fsb.length() == 0) {
                    return next();
                } else {
                    Orphan o = new Orphan();
                    o.setNodeKind(Type.TEXT);
                    o.setStringValue(fsb);
                    current = o;
                    position++;
                    return current;
                }
            } else {
                position++;
                return current;
            }
        }

        public Item current() {
            return current;
        }

        public int position() {
            return position;
        }

        public SequenceIterator getAnother() throws XPathException {
            return new AdjacentTextNodeMergingIterator(base.getAnother());
        }

        public int getProperties() {
            return 0;
        }
    }

    /**
     * Ask whether an item is a text node
     * @param item the item in question
     * @return true if the item is a node of kind text
     */

    public static boolean isTextNode(Item item) {
        return item instanceof NodeInfo && ((NodeInfo)item).getNodeKind() == Type.TEXT;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.



