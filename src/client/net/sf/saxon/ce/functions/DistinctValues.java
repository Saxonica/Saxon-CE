package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.ItemMappingFunction;
import client.net.sf.saxon.ce.expr.ItemMappingIterator;
import client.net.sf.saxon.ce.expr.StatefulMappingFunction;
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
        ItemMappingFunction function = new DistinctItemsMappingFunction(collator, context.getImplicitTimezone());
        return new ItemMappingIterator(iter, function);
    }

    public static class DistinctItemsMappingFunction implements ItemMappingFunction, StatefulMappingFunction {
        private StringCollator collator;
        private int implicitTimezone;
        private HashSet<Object> lookup = new HashSet<Object>(40);

        public DistinctItemsMappingFunction(StringCollator collator, int implicitTimezone) {
            this.collator = collator;
            this.implicitTimezone = implicitTimezone;
        }

        public Item mapItem(Item item) throws XPathException {
            AtomicValue value = (AtomicValue)item;
            Object key;
            if (value.isNaN()) {
                key = DistinctValues.class;
            } else {
                key = value.getXPathComparable(false, collator, implicitTimezone);
            }
            if (lookup.add(key)) {
                // returns true if newly added
                return item;
            } else {
                return null;
            }
        }

        public StatefulMappingFunction getAnother() {
            return new DistinctItemsMappingFunction(collator, implicitTimezone);
        }
    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
