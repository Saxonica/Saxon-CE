package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.NoDynamicContextException;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
* Class to do a sorted iteration
*/

public class SortedIterator implements SequenceIterator, LastPositionFinder, Sortable {

    // the items to be sorted
    protected SequenceIterator base;

    // the call-back function used to evaluate sort keys
    protected SortKeyEvaluator sortKeyEvaluator;

    // the comparators corresponding to these sort keys
    protected AtomicComparer[] comparators;

    // The items and keys are read into an array (nodeKeys) for sorting. This
    // array contains one "record" representing each node: the "record" contains
    // first, the Item itself, then an entry for each of its sort keys, in turn;
    // the last sort key is the position of the Item in the original sequence.
    protected int recordSize;
    protected Object[] nodeKeys;

    // The number of items to be sorted. -1 means not yet known.
    protected int count = -1;

    // The next item to be delivered from the sorted iteration
    protected int position = 0;

    // The context for the evaluation of sort keys
    protected XPathContext context;

    private SortedIterator(){}

    /**
     * Create a sorted iterator
     * @param context the dynamic XPath evaluation context
     * @param base an iterator over the sequence to be sorted
     * @param sortKeyEvaluator an object that allows the n'th sort key for a given item to be evaluated
     * @param comparators an array of AtomicComparers, one for each sort key, for comparing sort key values
     */

    public SortedIterator(XPathContext context, SequenceIterator base,
                                SortKeyEvaluator sortKeyEvaluator, AtomicComparer[] comparators) {
        this.context = context.newMinorContext();
        this.context.setCurrentIterator(base);
        this.base = base;
        this.sortKeyEvaluator = sortKeyEvaluator;
        this.comparators = comparators;
        recordSize = comparators.length + 2;

        // Avoid doing the sort until the user wants the first item. This is because
        // sometimes the user only wants to know whether the collection is empty.
    }

    /**
    * Get the next item, in sorted order
    */

    public Item next() throws XPathException {
        if (position < 0) {
            return null;
        }
        if (count<0) {
            doSort();
        }
        if (position < count) {
            return (Item)nodeKeys[(position++)*recordSize];
        } else {
            position = -1;
            return null;
        }
    }

    public Item current() {
        if (position < 1) {
            return null;
        }
        return (Item)nodeKeys[(position-1)*recordSize];
    }

    public int position() {
        return position;
    }

    public int getLastPosition() throws XPathException {
        if (count<0) {
            doSort();
        }
        return count;
    }

    public SequenceIterator getAnother() throws XPathException {
        // make sure the sort has been done, so that multiple iterators over the
        // same sorted data only do the sorting once.
        if (count<0) {
            doSort();
        }
        SortedIterator s = new SortedIterator();
        // the new iterator is the same as the old ...
        s.base = base.getAnother();
        s.sortKeyEvaluator = sortKeyEvaluator;
        s.comparators = comparators;
        s.recordSize = recordSize;
        s.nodeKeys = nodeKeys;
        s.count = count;
        s.context = context;
        //s.keyComparers = keyComparers;
        // ... except for its start position.
        s.position = 0;
        return s;
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
        return LAST_POSITION_FINDER;
    }

    /**
     * Create an array holding the items to be sorted and the values of their sort keys
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
            // TODO: delay evaluating the sort keys until we know they are needed. Often the 2nd and subsequent
            // sort key values will never be used. The only problem is with sort keys that depend on position().
            for (int n=0; n<comparators.length; n++) {
                nodeKeys[k+n+1] = sortKeyEvaluator.evaluateSortKey(n, context);
            }
            // make the sort stable by adding the record number
            nodeKeys[k+comparators.length+1] = Integer.valueOf(count);
            count++;
        }

        // If there's lots of unused space, reclaim it

        if (allocated * 2 < count || (allocated - count) > 2000) {
            Object[] nk2 = new Object[count * recordSize];
            System.arraycopy(nodeKeys, 0, nk2, 0, count * recordSize);
            nodeKeys = nk2;
        }
    }

    private void doSort() throws XPathException {
        buildArray();
        if (count<2) return;

        // sort the array

        //QuickSort.sort(this, 0, count-1);
        try {
            GenericSorter.quickSort(0, count, this);
        } catch (ClassCastException e) {
            //e.printStackTrace();
            XPathException err = new XPathException("Non-comparable types found while sorting: " + e.getMessage());
            err.setErrorCode("XTDE1030");
            throw err;
        }
        //GenericSorter.mergeSort(0, count, this);
    }

    /**
    * Compare two items in sorted sequence
    * (needed to implement the Sortable interface)
    * @return <0 if obj[a]<obj[b], 0 if obj[a]=obj[b], >0 if obj[a]>obj[b]
    */

    public int compare(int a, int b) {
        int a1 = a*recordSize + 1;
        int b1 = b*recordSize + 1;
        try {
            for (int i=0; i<comparators.length; i++) {
                int comp = comparators[i].compareAtomicValues(
                        (AtomicValue)nodeKeys[a1+i], (AtomicValue)nodeKeys[b1+i]);
                if (comp != 0) {
                    // we have found a difference, so we can return
                    return comp;
                }
            }
        } catch (NoDynamicContextException e) {
            throw new AssertionError("Sorting without dynamic context: " + e.getMessage());
        }

        // all sort keys equal: return the items in their original order

        return ((Integer)nodeKeys[a1+comparators.length]).intValue() -
                ((Integer)nodeKeys[b1+comparators.length]).intValue();
    }

    /**
    * Swap two items (needed to implement the Sortable interface)
    */

    public void swap(int a, int b) {
        int a1 = a*recordSize;
        int b1 = b*recordSize;
        for (int i=0; i<recordSize; i++) {
            Object temp = nodeKeys[a1+i];
            nodeKeys[a1+i] = nodeKeys[b1+i];
            nodeKeys[b1+i] = temp;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
