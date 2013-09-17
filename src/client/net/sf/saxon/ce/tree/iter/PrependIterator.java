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
        } else {
            return base.next();
        }
    }

    /**
     * Get another iterator over the same sequence of items, positioned at the
     * start of the sequence
     *
     * @return a new iterator over the same sequence
     */

    public SequenceIterator getAnother() {
        return new PrependIterator(start, (UnfailingIterator)base.getAnother());
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.