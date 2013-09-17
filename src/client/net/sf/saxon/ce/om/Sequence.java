package client.net.sf.saxon.ce.om;

import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;

/**
 * A ValueRepresentation is a representation of a Value. This is a marker interface
 * used to represent the union of two classes: Value, and NodeInfo.
 * Either of these two classes can be used to represent a value. The class is used primarily
 * to represent the value of a variable.
 * <p>
 * This class is intended primarily for internal use, and should not be considered part
 * of the Saxon public API.
 */

public interface Sequence {

    /**
     * Iterate over the items contained in this value.
     * @return an iterator over the sequence of items
     */

    public UnfailingIterator iterate();

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     * numbered zero. If n is negative or >= the length of the sequence, returns null.
     */

    public Item itemAt(int n);

    /**
     * Get the length of the sequence
     * @return the number of items in the sequence
     */

    public int getLength();



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

