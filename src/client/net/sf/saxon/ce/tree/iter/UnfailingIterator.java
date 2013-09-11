package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.Item;


/**
 * A SequenceIterator is used to iterate over a sequence. An UnfailingIterator
 * is a SequenceIterator that throws no checked exceptions.
 */

public interface UnfailingIterator extends SequenceIterator {

    /**
     * Get the next item in the sequence. <BR>
     * @return the next Item. If there are no more nodes, return null.
     */

    public Item next();

    /**
     * Get another iterator over the same sequence of items, positioned at the
     * start of the sequence. It must be possible to call this method at any time, whether
     * none, some, or all of the items in the original iterator have been read. The method
     * is non-destructive: it does not change the state of the original iterator.
     * @return a new iterator over the same sequence
     */

    public SequenceIterator getAnother();

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
