package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;

/**
 * A SequenceIterator is used to iterate over a sequence. An AxisIterator
 * is a SequenceIterator that always iterates over a set of nodes, and that
 * throws no exceptions; it also supports the ability
 * to find the last() position, again with no exceptions.
 * This class is an abstract implementation of AxisIterator that is used
 * as a base class for many concrete implementations. The main functionality
 * that it provides is maintaining the current position.
 */

public abstract class AxisIteratorImpl implements UnfailingIterator {

    protected int position = 0;
    protected NodeInfo current;

    /**
     * Get the current node in the sequence.
     * @return the node returned by the most recent call on next()
     */

    public Item current() {
        return current;
    }

    /**
     * Get the current position
     * @return the position of the most recent node returned by next()
     */

    private final int position() {
        return position;
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
