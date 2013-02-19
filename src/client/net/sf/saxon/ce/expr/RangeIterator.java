package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.GroundedValue;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.GroundedIterator;
import client.net.sf.saxon.ce.value.IntegerRange;
import client.net.sf.saxon.ce.value.IntegerValue;

/**
 * An Iterator that produces numeric values in a monotonic sequence,
 * ascending or descending. Although a range expression (N to M) is always
 * in ascending order, applying the reverse() function will produce
 * a RangeIterator that works in descending order.
*/

public class RangeIterator implements SequenceIterator,
        LastPositionFinder,
        GroundedIterator {

    int start;
    int currentValue;
    int limit;

    /**
     * Create an iterator over a range of monotonically increasing integers
     * @param start the first integer in the sequence
     * @param end the last integer in the sequence. Must be >= start.
     */

    public RangeIterator(int start, int end) {
        this.start = start;
        currentValue = start - 1;
        limit = end;
    }

    public Item next() {
        if (++currentValue > limit) {
            return null;
        }
        return new IntegerValue(currentValue);
    }

    public Item current() {
        if (currentValue > limit) {
            return null;
        } else {
            return new IntegerValue(currentValue);
        }
    }

    public int position() {
        if (currentValue > limit) {
            return -1;
        } else {
            return currentValue - start + 1;
        }
    }

    public int getLastPosition() {
        return (limit - start) + 1;
    }

    public SequenceIterator getAnother() throws XPathException {
        return new RangeIterator(start, limit);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link client.net.sf.saxon.ce.om.SequenceIterator#GROUNDED}, {@link client.net.sf.saxon.ce.om.SequenceIterator#LAST_POSITION_FINDER},
     *         It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return LAST_POSITION_FINDER | GROUNDED;
    }

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator. This should be an "in-memory" value, not a Closure.
     *
     * @return the corresponding Value
     */

    public GroundedValue materialize() throws XPathException {
        return new IntegerRange(start, limit);
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
