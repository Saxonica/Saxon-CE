package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * ItemMappingIterator applies a mapping function to each item in a sequence.
 * The mapping function either returns a single item, or null (representing an
 * empty sequence).
 * <p/>
 * This is a specialization of the more general MappingIterator class, for use
 * in cases where a single input item never maps to a sequence of more than one
 * output item.
 */

public class ItemMappingIterator implements SequenceIterator, LastPositionFinder {

    private SequenceIterator base;
    private ItemMappingFunction action;
    private Item current = null;
    private int position = 0;
    private boolean oneToOne = false;

    /**
     * Construct an ItemMappingIterator that will apply a specified ItemMappingFunction to
     * each Item returned by the base iterator.
     *
     * @param base   the base iterator
     * @param action the mapping function to be applied. 
     */

    public ItemMappingIterator(SequenceIterator base, ItemMappingFunction action) {
        this.base = base;
        this.action = action;
    }

    /**
     * Construct an ItemMappingIterator that will apply a specified ItemMappingFunction to
     * each Item returned by the base iterator.
     *
     * @param base   the base iterator
     * @param action the mapping function to be applied
     * @param oneToOne true if this iterator is one-to-one
     */

    public ItemMappingIterator(SequenceIterator base, ItemMappingFunction action, boolean oneToOne) {
        this.base = base;
        this.action = action;
        this.oneToOne = oneToOne;
    }

    public Item next() throws XPathException {
        while (true) {
            Item nextSource = base.next();
            if (nextSource == null) {
                current = null;
                position = -1;
                return null;
            }
            // Call the supplied mapping function
            current = action.mapItem(nextSource);
            if (current != null) {
                position++;
                return current;
            }
            // otherwise go round the loop to get the next item from the base sequence
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        SequenceIterator newBase = base.getAnother();
        ItemMappingFunction newAction = action instanceof StatefulMappingFunction ?
                (ItemMappingFunction)((StatefulMappingFunction)action).getAnother() :
                action;
        return new ItemMappingIterator(newBase, newAction, oneToOne);
    }

    public int getLastPosition() throws XPathException {
        // Must only be called if this is a last-position-finder iterator, which will only be true if the base iterator
        // is a last-position-finder iterator and one-to-one is true
        return ((LastPositionFinder)base).getLastPosition();
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link client.net.sf.saxon.ce.om.SequenceIterator#GROUNDED},
     *         {@link client.net.sf.saxon.ce.om.SequenceIterator#LAST_POSITION_FINDER},. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        if (oneToOne) {
            return base.getProperties() & (LAST_POSITION_FINDER);
        } else {
            return 0;
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.