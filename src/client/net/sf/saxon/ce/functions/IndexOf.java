package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.ItemMappingFunction;
import client.net.sf.saxon.ce.expr.ItemMappingIterator;
import client.net.sf.saxon.ce.expr.StatefulMappingFunction;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.AtomicComparer;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
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
    * Evaluate the function to return an iteration of selected items.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        final GenericAtomicComparer comparer = getAtomicComparer(2, context);
        SequenceIterator seq = argument[0].iterate(context);
        final AtomicValue val = (AtomicValue)argument[1].evaluateItem(context);
        final BuiltInAtomicType searchType = val.getItemType();
        return new ItemMappingIterator(seq,
                new IndexOfMappingFunction(searchType, comparer, val));
    }

    public static class IndexOfMappingFunction implements ItemMappingFunction, StatefulMappingFunction {
        int index = 0;
        private BuiltInAtomicType searchType;
        private AtomicComparer comparer;
        private AtomicValue val;

        public IndexOfMappingFunction(BuiltInAtomicType searchType, AtomicComparer comparer, AtomicValue val) {
            this.searchType = searchType;
            this.comparer = comparer;
            this.val = val;
        }

        public IntegerValue mapItem(Item item) throws XPathException {
            index++;
            if (Type.isComparable(searchType, ((AtomicValue) item).getItemType(), false) &&
                    comparer.comparesEqual(((AtomicValue) item), val)) {
                return new IntegerValue(index);
            } else {
                return null;
            }
        }

        /**
         * Return a clone of this MappingFunction, with the state reset to its state at the beginning
         * of the underlying iteration
         *
         * @return a clone of this MappingFunction
         */
        public StatefulMappingFunction getAnother() {
            return new IndexOfMappingFunction(searchType, comparer, val);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
