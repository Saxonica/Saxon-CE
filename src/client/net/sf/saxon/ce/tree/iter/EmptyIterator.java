package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.value.EmptySequence;

/**
 * EmptyIterator: an iterator over an empty sequence. Since such an iterator has no state,
 * only one instance is required; therefore a singleton instance is available via the static
 * getInstance() method.
 */

public class EmptyIterator implements UnfailingIterator,
        LastPositionFinder, GroundedIterator {

    private static EmptyIterator theInstance = new EmptyIterator();

    /**
     * Get an EmptyIterator, an iterator over an empty sequence.
     * @return an EmptyIterator (in practice, this always returns the same
     *     one)
     */
    public static EmptyIterator getInstance() {
        return theInstance;
    }

    /**
     * private constructor
     */

    private EmptyIterator() {}


    /**
     * Get the next item.
     * @return the next item. For the EmptyIterator this is always null.
     */
    public Item next() {
        return null;
    }

    /**
     * Get the position of the last item in the sequence.
     * @return the position of the last item in the sequence, always zero in
     *     this implementation
     */
    public int getLastPosition() {
        return 0;
    }

    /**
     * Get another iterator over the same items, positioned at the start.
     * @return another iterator over an empty sequence (in practice, it
     *     returns the same iterator each time)
     */
    public UnfailingIterator getAnother() {
        return theInstance;
    }

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator. This should be an "in-memory" value, not a Closure.
     *
     * @return the corresponding Value
     */

    public Sequence materialize() {
        return EmptySequence.getInstance();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
