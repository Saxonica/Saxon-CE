package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A variant of the GroupByIterator used for XQuery 1.1 grouping, where the grouping key
 * is either a single atomic value or an empty sequence, and an empty sequence compares
 * equal to an empty sequence.
 */
public class GroupBySingletonIterator extends GroupByIterator {

   /**
     * Create a GroupByIterator
     * @param population iterator over the population to be grouped
     * @param keyExpression the expression used to calculate the grouping key
     * @param keyContext dynamic context for calculating the grouping key
     * @param collator Collation to be used for comparing grouping keys
     * @throws client.net.sf.saxon.ce.trans.XPathException
     */

    public GroupBySingletonIterator(SequenceIterator population, Expression keyExpression,
                           XPathContext keyContext, StringCollator collator) throws XPathException {
       super(population, keyExpression, keyContext, collator);
    }

    /**
     * Process one item in the population
     * @param index the index of items
     * @param item  the item from the population to be processed
     * @param c2    the XPath evaluation context
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *
     */

    protected void processItem(HashMap<ComparisonKey, List<Item>> index,
                               Item item, XPathContext c2) throws XPathException {
        AtomicValue key = (AtomicValue)keyExpression.evaluateItem(c2);
        ComparisonKey comparisonKey;
        if (key == null) {
            comparisonKey = new ComparisonKey(Type.EMPTY, "()");
        } else {
            comparisonKey = comparer.getComparisonKey(key);
        }
        List<Item> g = index.get(comparisonKey);
        if (g == null) {
            List<Item> newGroup = new ArrayList<Item>(20);
            newGroup.add(item);
            groups.add(newGroup);
            groupKeys.add(key);
            index.put(comparisonKey, newGroup);
        } else {
            g.add(item);
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


