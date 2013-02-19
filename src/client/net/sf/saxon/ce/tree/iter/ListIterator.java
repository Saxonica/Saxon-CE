package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.om.GroundedValue;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceExtent;
import client.net.sf.saxon.ce.value.SingletonItem;

import java.util.List;

/**
* Class ListIterator, iterates over a sequence of items held in a Java List
*/

public class ListIterator
        implements UnfailingIterator, LastPositionFinder, GroundedIterator {

    int index=0;
    int length;
    Item current = null;
    List list = null;

    /**
     * Create a ListIterator over a given List
     * @param list the list: all objects in the list must be instances of {@link Item}
     */

    public ListIterator(List list) {
        index = 0;
        this.list = list;
        this.length = list.size();
    }

   /**
     * Create a ListIterator over the leading part of a given List
     * @param list the list: all objects in the list must be instances of {@link Item}
     * @param length the number of items to be included
     */

    public ListIterator(List list, int length) {
        index = 0;
        this.list = list;
        this.length = length;
    }

    public Item next() {
        if (index >= length) {
            current = null;
            index = -1;
            length = -1;
            return null;
        }
        current = (Item)list.get(index++);
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return index;
    }

    public int getLastPosition() {
        return length;
    }

    public SequenceIterator getAnother() {
        return new ListIterator(list);
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

    /**
     * Return a SequenceValue containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding SequenceValue
     */

    public GroundedValue materialize() {
        if (length == 0) {
            return EmptySequence.getInstance();
        } else if (length == 1) {
            Item item = (Item)list.get(0);
            if (item instanceof NodeInfo) {
                return new SingletonItem((NodeInfo)item);
            } else {
                return (AtomicValue)item;
            }
        } else {
            return new SequenceExtent(list);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

