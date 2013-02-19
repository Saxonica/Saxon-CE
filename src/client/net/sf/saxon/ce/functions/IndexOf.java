package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.IntegerValue;


/**
* The XPath 2.0 index-of() function
*/


public class IndexOf extends CollatingFunction {

    public IndexOf newInstance() {
        return new IndexOf();
    }
    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        GenericAtomicComparer comparer = getAtomicComparer(2, context);
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue val = (AtomicValue)argument[1].evaluateItem(context);
        return new IndexIterator(seq, val, comparer);
    }

    /**
     * Iterator to return the index positions of selected items in a sequence
     */

    public static class IndexIterator implements SequenceIterator {

        private SequenceIterator base;
        private AtomicValue value;
        private GenericAtomicComparer comparer;
        private int index = 0;
        private int position = 0;
        private Item current = null;
        private BuiltInAtomicType primitiveTypeRequired;

        /**
         * Get an iterator returning the index positions of selected items in a sequence
         * @param base The sequence to be searched
         * @param value The value being sought
         * @param comparer Comparer used to determine whether values match
         */

        public IndexIterator(SequenceIterator base, AtomicValue value, GenericAtomicComparer comparer) {
            this.base = base;
            this.value = value;
            this.comparer = comparer;
            primitiveTypeRequired = value.getPrimitiveType();
        }

        public Item next() throws XPathException {
            while (true) {
                AtomicValue i = (AtomicValue)base.next();
                if (i==null) break;
                index++;
                if (Type.isComparable(primitiveTypeRequired,
                            i.getPrimitiveType(), false)) {
                    try {
                        if (comparer.comparesEqual(i, value)) {
                            current = IntegerValue.makeIntegerValue(index);
                            position++;
                            return current;
                        }
                    } catch (ClassCastException err) {
                        // non-comparable values are treated as not equal
                        // Exception shouldn't happen but we catch it anyway
                    }
                }
            }
            current = null;
            position = -1;
            return null;
        }

        public Item current() {
            return current;
        }

        public int position() {
            return position;
        }

        public SequenceIterator getAnother() throws XPathException {
            return new IndexIterator(base.getAnother(), value, comparer);
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
            return 0;
        }
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
