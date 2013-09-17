package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceExtent;

import java.util.List;

/**
 * Class ListIterator, iterates over a sequence of items held in a Java List
 */

public class ListIterator
        implements UnfailingIterator, LastPositionFinder, GroundedIterator {

    int index = 0;
    int length;
    List<? extends Item> list = null;

    /**
     * Create a ListIterator over a given List
     *
     * @param list the list: all objects in the list must be instances of {@link Item}
     */

    public ListIterator(List<? extends Item> list) {
        index = 0;
        this.list = list;
        this.length = list.size();
    }

    /**
     * Create a ListIterator over the leading part of a given List
     *
     * @param list   the list: all objects in the list must be instances of {@link Item}
     * @param length the number of items to be included
     */

    public ListIterator(List list, int length) {
        index = 0;
        this.list = list;
        this.length = length;
    }

    public Item next() {
        if (index >= length) {
            index = -1;
            length = -1;
            return null;
        }
        return list.get(index++);
    }

    public int getLastPosition() {
        return length;
    }

    public SequenceIterator getAnother() {
        return new ListIterator(list);
    }

    /**
     * Return a SequenceValue containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding SequenceValue
     */

    public Sequence materialize() {
        if (length == 0) {
            return EmptySequence.getInstance();
        } else if (length == 1) {
            return (Item) list.get(0);
        } else {
            return new SequenceExtent(list);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

