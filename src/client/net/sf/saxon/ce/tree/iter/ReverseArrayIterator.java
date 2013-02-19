package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;


/**
  * ReverseArrayIterator is used to enumerate items held in an array in reverse order.
  * @author Michael H. Kay
  */


public class ReverseArrayIterator implements UnfailingIterator,
                                                   LastPositionFinder {

    Item[] items;
    int index = 0;
    int start;
    int end;         // item after the last to be output
    Item current = null;

    /**
     * Create an iterator a slice of an array
     * @param items The array of items
     * @param start The first item in the array to be be used (this will be the last
     * one in the resulting iteration). Zero-based.
     * @param end The item after the last one in the array to be used (this will be the
     * first one to be returned by the iterator). Zero-based.
    */

    public ReverseArrayIterator(Item[] items, int start, int end) {
        this.items = items;
        this.end = end;
        this.start = start;
        index = end - 1;
    }

    public Item next() {
        if (index >= start) {
            current = items[index--];
            return current;
        } else {
            current = null;
            return null;
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        if (index < start-1) {
            return -1;  // position() returns -1 after next() returns null
        }
        return end - 1 - index;
    }

    public int getLastPosition() {
        return end - start;
    }

    public SequenceIterator getAnother() {
        return new ReverseArrayIterator(items, start, end);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return LAST_POSITION_FINDER;
    }

    /**
     * Get an iterator that processes the same items in reverse order.
     * Since this iterator is processing the items backwards, this method
     * returns an ArrayIterator that processes them forwards.
     * @return a new ArrayIterator
     */

    public SequenceIterator getReverseIterator() {
        return new ArrayIterator(items, start, end);
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
