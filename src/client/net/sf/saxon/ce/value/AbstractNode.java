package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;

/**
* A value that is a sequence containing zero or one items. Used only for items that are not atomic values
 * (that is, nodes, and function items)
*/

public abstract class AbstractNode implements Item, Sequence {

    /**
     * Get the length of the sequence
     */

    public int getLength() {
        return 1;
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     */

    public Item itemAt(int n) {
        return (n==0 ? this : null);
    }

    /**
    * Return an enumeration of this nodeset value.
    */

    public UnfailingIterator iterate() {
        return SingletonIterator.makeIterator(this);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

