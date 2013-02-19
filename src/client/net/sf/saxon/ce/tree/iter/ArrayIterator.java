package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.value.SequenceExtent;
import client.net.sf.saxon.ce.om.*;

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
    protected int start;          // position of first item to be returned, zero-based
    protected int end;            // position of first item that is NOT returned, zero-based
    private Item current = null;

    /**
     * Create an iterator over all the items in an array
     *
     * @param nodes the array (of any items, not necessarily nodes) to be
     *     processed by the iterator
     */

    public ArrayIterator(Item[] nodes) {
        items = nodes;
        start = 0;
        end = nodes.length;
        index = 0;
    }

    /**
     * Create an iterator over a range of an array. Note that the start position is zero-based
     *
     * @param items the array (of nodes or simple values) to be processed by
     *     the iterator
     * @param start the position of the first item to be processed
     *     (numbering from zero). Must be between zero and nodes.length-1; if not,
     *     undefined exceptions are likely to occur.
     * @param end position of first item that is NOT returned, zero-based. Must be
     *     beween 1 and nodes.length; if not, undefined exceptions are likely to occur.
     */

    public ArrayIterator(Item[] items, int start, int end) {
        this.items = items;
        this.end = end;
        this.start = start;
        index = start;
    }

    /**
     * Get the next item in the array
     * @return the next item in the array
     */

    public Item next() {
        if (index >= end) {
            index = end+1;
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
    public int position() {
        if (index > end) {
            return -1;
        }
        return index - start;
    }

    /**
     * Get the number of items in the part of the array being processed
     *
     * @return the number of items; equivalently, the position of the last
     *     item
     */
    public int getLastPosition() {
        return end - start;
    }

    /**
     * Get another iterator over the same items
     *
     * @return a new ArrayIterator
     */
    public SequenceIterator getAnother() {
        return new ArrayIterator(items, start, end);
    }

    /**
     * Indicate that any nodes returned in the sequence will be atomized. This
     * means that if it wishes to do so, the implementation can return the typed
     * values of the nodes rather than the nodes themselves. The implementation
     * is free to ignore this hint.
     * @param atomizing true if the caller of this iterator will atomize any
     * nodes that are returned, and is therefore willing to accept the typed
     * value of the nodes instead of the nodes themselves.
     */

    //public void setIsAtomizing(boolean atomizing) {}

    /**
     * Get the underlying array
     *
     * @return the underlying array being processed by the iterator
     */

    public Item[] getArray() {
        return items;
    }

    /**
     * Get the initial start position
     *
     * @return the start position of the iterator in the array (zero-based)
     */

    public int getStartPosition() {
        return start;
    }

    /**
     * Get the end position in the array
     *
     * @return the position in the array (zero-based) of the first item not included
     * in the iteration
     */

    public int getEndPosition() {
        return end;
    }

    /**
     * Return a SequenceValue containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding SequenceValue
     */

    public GroundedValue materialize() {
        if (start==0 && end == items.length) {
            return new SequenceExtent(items);
        } else {
            SequenceExtent e = new SequenceExtent(items);
            return new SequenceExtent(e, start, end-start);
        }
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
        return GROUNDED | LAST_POSITION_FINDER;
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
