package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.XPathContextMajor;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
 * A SortedGroupIterator is a modified SortedIterator. It sorts a sequence of groups,
 * and is itself a GroupIterator. The modifications retain extra information about
 * the items being sorted. The items are each the leading item of a group, and as well
 * as the item itself, the iterator preserves information about the group: specifically,
 * an iterator over the items in the group, and the value of the grouping key (if any).
 */

public class SortedGroupIterator extends SortedIterator implements GroupIterator {

    public SortedGroupIterator(XPathContext context, GroupIterator base,
                               SortKeyEvaluator sortKeyEvaluator,
                               AtomicComparer[] comparators) {
        super(context, base, sortKeyEvaluator, comparators);
        // add two items to each tuple, for the iterator over the items in the group,
        // and the grouping key, respectively.
        recordSize += 2;
    }

    /**
     * Override the method that builds the array of values and sort keys.
     * @throws XPathException
     */

    protected void buildArray() throws XPathException {
        int allocated;
        if ((base.getProperties() & SequenceIterator.LAST_POSITION_FINDER) != 0) {
            allocated = ((LastPositionFinder)base).getLastPosition();
        } else {
            allocated = 100;
        }

        nodeKeys = new Object[allocated * recordSize];
        count = 0;

        XPathContextMajor c2 = context.newContext();
        c2.setCurrentIterator(base);
        c2.setCurrentGroupIterator((GroupIterator)base);
                // this provides the context for evaluating the sort key

        // initialise the array with data

        while (true) {
            Item item = base.next();
            if (item == null) {
                break;
            }
            if (count==allocated) {
                allocated *= 2;
                Object[] nk2 = new Object[allocated * recordSize];
                System.arraycopy(nodeKeys, 0, nk2, 0, count * recordSize);
                nodeKeys = nk2;
            }
            int k = count*recordSize;
            nodeKeys[k] = item;
            for (int n=0; n<comparators.length; n++) {
                nodeKeys[k+n+1] = sortKeyEvaluator.evaluateSortKey(n, c2);
            }
            nodeKeys[k+comparators.length+1] = Integer.valueOf(count);
            // extra code added to superclass
            nodeKeys[k+comparators.length+2] = ((GroupIterator)base).getCurrentGroupingKey();
            nodeKeys[k+comparators.length+3] = ((GroupIterator)base).iterateCurrentGroup();
            count++;
        }
    }

    public AtomicValue getCurrentGroupingKey() {
        return (AtomicValue)nodeKeys[(position-1)*recordSize+comparators.length+2];
    }

    public SequenceIterator iterateCurrentGroup() throws XPathException {
        SequenceIterator iter =
                (SequenceIterator)nodeKeys[(position-1)*recordSize+comparators.length+3];
        return iter.getAnother();
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.