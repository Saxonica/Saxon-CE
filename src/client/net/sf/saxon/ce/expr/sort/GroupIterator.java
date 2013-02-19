package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
 * A GroupIterator is an iterator that iterates over a sequence of groups.
 * The normal methods such as next() and current() always deliver the leading item
 * of the group. Additional methods are available to get the grouping key for the
 * current group (only applicable to group-by and group-adjacent), and to get all the
 * members of the current group.
 */

public interface GroupIterator extends SequenceIterator {

    /**
     * Get the grouping key of the current group
     * @return the current grouping key in the case of group-by or group-adjacent,
     * or null in the case of group-starting-with and group-ending-with
     */

    public AtomicValue getCurrentGroupingKey();

    /**
     * Get an iterator over the members of the current group, in population
     * order. This must always be a clean iterator, that is, an iterator that
     * starts at the first item of the group.
     * @return an iterator over all the members of the current group, in population
     * order.
     */

    public SequenceIterator iterateCurrentGroup() throws XPathException;

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.