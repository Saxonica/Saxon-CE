package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.value.SequenceExtent;

/**
 * ArrayIterator is used to enumerate items held in an array.
 * The items are always held in the correct sorted order for the sequence.
 *
 * @author Michael H. Kay
 */


public class ArrayIterator implements UnfailingIterator,
        LastPositionFinder, GroundedIterator {

    protected Item[] items;
    private int index;          // position in array of current item, zero-based
                                // set equal to end+1 when all the items required have been read.
    private Item current = null;

    /**
     * Create an iterator over all the items in an array
     *
     * @param nodes the array (of any items, not necessarily nodes) to be
     *     processed by the iterator
     */

    public ArrayIterator(Item[] nodes) {
        items = nodes;
        index = 0;
    }

    /**
     * Get the next item in the array
     * @return the next item in the array
     */

    public Item next() {
        if (index >= items.length) {
            index = items.length+1;
            current = null;
            return null;
        }
        current = items[index++];
        return current;
    }

    /**
     * Get the current item in the array
     *
     * @return the item returned by the most recent call of next()
     */
    public Item current() {
        return current;
    }

    /**
     * Get the position of the current item in the array
     *
     * @return the current position (starting at 1 for the first item)
     */
    private int position() {
        if (index > items.length) {
            return -1;
        }
        return index;
    }

    /**
     * Get the number of items in the part of the array being processed
     *
     * @return the number of items; equivalently, the position of the last
     *     item
     */
    public int getLastPosition() {
        return items.length;
    }

    /**
     * Get another iterator over the same items
     *
     * @return a new ArrayIterator
     */
    public SequenceIterator getAnother() {
        return new ArrayIterator(items);
    }

    /**
     * Return a SequenceValue containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding SequenceValue
     */

    public Sequence materialize() {
        return new SequenceExtent(items);
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
