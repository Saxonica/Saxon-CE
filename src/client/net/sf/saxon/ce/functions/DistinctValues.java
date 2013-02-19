package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.expr.sort.AtomicComparer;
import client.net.sf.saxon.ce.expr.sort.AtomicSortComparer;
import client.net.sf.saxon.ce.expr.sort.ComparisonKey;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;

import java.util.HashSet;

/**
* The XPath 2.0 distinct-values() function
*/

public class DistinctValues extends CollatingFunction {

    public DistinctValues newInstance() {
        return new DistinctValues();
    }

    private transient AtomicComparer atomicComparer;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        if (stringCollator != null) {
            int type = argument[0].getItemType(visitor.getConfiguration().getTypeHierarchy()).getPrimitiveType();
            atomicComparer = AtomicSortComparer.makeSortComparer(
                    stringCollator, type, visitor.getStaticContext().makeEarlyEvaluationContext());
        }
    }

    /**
     * Get the AtomicComparer allocated at compile time.
     * @return the AtomicComparer if one has been allocated at compile time; return null
     * if the collation is not known until run-time
     */

    public AtomicComparer getAtomicComparer() {
        return atomicComparer;
    }

    /**
     * Get the AtomicComparer, creating it if it was not allocated at compile time.
     * @return the AtomicComparer already allocated if one has been allocated at compile time; otherwise
     * allocate one from knowledge of the collation at run-time
     */

    public AtomicComparer makeAtomicComparer(XPathContext context) throws XPathException {
        AtomicComparer comp = atomicComparer;
        if (comp == null) {
            int type = argument[0].getItemType(context.getConfiguration().getTypeHierarchy()).getPrimitiveType();
            comp = makeAtomicSortComparer(type, context);
        } else {
            comp = comp.provideContext(context);
        }
        return comp;
    }

    /**
    * Evaluate the function to return an iteration of selected values or nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        AtomicComparer comp = makeAtomicComparer(context);
        SequenceIterator iter = argument[0].iterate(context);
        return new DistinctIterator(iter, comp);
    }

    /**
     * Get a SortComparer that can be used to compare values
     * @param type the fingerprint of the static item type of the first argument after atomization
     * @param context The dynamic evaluation context.
     * @return the comparer
    */

    private AtomicComparer makeAtomicSortComparer(int type, XPathContext context) throws XPathException {
        final StringCollator collator = getCollator(1, context);
        return AtomicSortComparer.makeSortComparer(collator, type, context);
    }

    /**
     * Iterator class to return the distinct values in a sequence
     */

    public static class DistinctIterator implements SequenceIterator {

        private SequenceIterator base;
        private AtomicComparer comparer;
        private int position;
        private AtomicValue current;
        private HashSet<ComparisonKey> lookup = new HashSet<ComparisonKey>(40);

        /**
         * Create an iterator over the distinct values in a sequence
         * @param base the input sequence. This must return atomic values only.
         * @param comparer The comparer used to obtain comparison keys from each value;
         * these comparison keys are themselves compared using equals().
         */

        public DistinctIterator(SequenceIterator base, AtomicComparer comparer) {
            this.base = base;
            this.comparer = comparer;
            position = 0;
        }

        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next item, or null if there are no more items.
         * @throws client.net.sf.saxon.ce.trans.XPathException
         *          if an error occurs retrieving the next item
         */

        public Item next() throws XPathException {
            while (true) {
                AtomicValue nextBase = (AtomicValue)base.next();
                if (nextBase==null) {
                    current = null;
                    position = -1;
                    return null;
                }
                ComparisonKey key = comparer.getComparisonKey(nextBase);
                if (lookup.add(key)) {
                    // returns true if newly added (if not, keep looking)
                    current = nextBase;
                    position++;
                    return nextBase;
                }
            }
        }

        /**
         * Get the current value in the sequence (the one returned by the
         * most recent call on next()). This will be null before the first
         * call of next().
         *
         * @return the current item, the one most recently returned by a call on
         *         next(); or null, if next() has not been called, or if the end
         *         of the sequence has been reached.
         */

        public Item current() {
            return current;
        }

        /**
         * Get the current position. This will be zero before the first call
         * on next(), otherwise it will be the number of times that next() has
         * been called.
         *
         * @return the current position, the position of the item returned by the
         *         most recent call of next()
         */

        public int position() {
            return position;
        }

        /**
         * Get another SequenceIterator that iterates over the same items as the original,
         * but which is repositioned at the start of the sequence.
         *
         * @return a SequenceIterator that iterates over the same items,
         *         positioned before the first item
         * @throws client.net.sf.saxon.ce.trans.XPathException
         *          if any error occurs
         */

        public SequenceIterator getAnother() throws XPathException {
            return new DistinctIterator(base.getAnother(), comparer);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
         *         and {@link #LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return 0;
        }
    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
