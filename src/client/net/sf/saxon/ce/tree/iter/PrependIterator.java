package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;

/**
 * An iterator over nodes, that prepends a given node to the nodes
 * returned by another iterator. Used to modify an iterator over axis A
 * to one that iterates over A-OR-SELF.
 */

public class PrependIterator implements UnfailingIterator {

    NodeInfo start;
    UnfailingIterator base;
    int position = 0;

    public PrependIterator(NodeInfo start, UnfailingIterator base) {
        this.start = start;
        this.base = base;
    }


    /**
     * Get the next item in the sequence. <BR>
     *
     * @return the next Item. If there are no more nodes, return null.
     */

    public Item next() {
        if (position == 0) {
            position = 1;
            return start;
        }
        Item n = base.next();
        if (n == null) {
            position = -1;
        } else {
            position++;
        }
        return n;
    }

    /**
     * Get the current item in the sequence.
     *
     * @return the current item, that is, the item most recently returned by
     *         next()
     */

    public Item current() {
        if (position() == 1) {
            return start;
        } else if (position < 1) {
            return null;
        } else {
            return base.current();
        }
    }

    /**
     * Get the current position
     *
     * @return the position of the current item (the item most recently
     *         returned by next()), starting at 1 for the first node
     */

    public int position() {
       return position;
    }

    /**
     * Get another iterator over the same sequence of items, positioned at the
     * start of the sequence
     *
     * @return a new iterator over the same sequence
     */

    public SequenceIterator getAnother() {
        return new PrependIterator(start, base);
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
        return 0;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.