package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
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

    /**
    * Evaluate the function to return an iteration of selected values or nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        StringCollator collator = getCollator(1, context);
        SequenceIterator iter = argument[0].iterate(context);
        return new DistinctIterator(iter, collator, context);
    }

    /**
     * Iterator class to return the distinct values in a sequence
     */

    public static class DistinctIterator implements SequenceIterator {

        private SequenceIterator base;
        private StringCollator collator;
        private XPathContext context;
        private int position;
        private AtomicValue current;
        private HashSet<Object> lookup = new HashSet<Object>(40);

        /**
         * Create an iterator over the distinct values in a sequence
         * @param base the input sequence. This must return atomic values only.
         * @param collator The collator used to obtain comparison keys from each value;
         * @param context dynamic context (used e.g. for comparing dateTimes with no timezone)
         * these comparison keys are themselves compared using equals().
         */

        public DistinctIterator(SequenceIterator base, StringCollator collator, XPathContext context) {
            this.base = base;
            this.collator = collator;
            this.context = context;
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
                Object key;
                if (nextBase.isNaN()) {
                    key = DistinctValues.class;
                } else {
                    key = nextBase.getXPathComparable(false, collator, context);
                }
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
            return new DistinctIterator(base.getAnother(), collator, context);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER}. It is always
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
