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

    protected SequenceIterator getBaseIterator() {
        return base;
    }

    protected ItemMappingFunction getMappingFunction() {
        return action;
    }

    public Item next() throws XPathException {
        while (true) {
            Item nextSource = base.next();
            if (nextSource == null) {
                return null;
            }
            // Call the supplied mapping function
            try {
                Item curr = action.mapItem(nextSource);
                if (curr != null) {
                    return curr;
                }
            } catch (EarlyExitException e) {
                return null;
            }
            // otherwise go round the loop to get the next item from the base sequence
        }
    }

    public SequenceIterator getAnother() throws XPathException {
        SequenceIterator newBase = base.getAnother();
        ItemMappingFunction newAction = action instanceof StatefulMappingFunction ?
                (ItemMappingFunction)((StatefulMappingFunction)action).getAnother(newBase) :
                action;
        return new ItemMappingIterator(newBase, newAction, oneToOne);
    }

    public int getLastPosition() throws XPathException {
        if (base instanceof LastPositionFinder && oneToOne) {
            return ((LastPositionFinder)base).getLastPosition();
        } else {
            return -1;
        }
    }

    /**
     * The mapping function can throw an EarlyExitException to indicate that no more items will be
     * returned; processing of the input sequence can cease at this point.
     */

    public static class EarlyExitException extends XPathException{
        public EarlyExitException() {
            super("");
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.