package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.*;

/**
 * Implementation of the fn:count function
 */
public class Count extends Aggregate {

    public Count newInstance() {
        return new Count();
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = argument[0].iterate(context);

        return new IntegerValue(count(iter));
    }

    /**
     * Get the number of items in a sequence identified by a SequenceIterator
     * @param iter The SequenceIterator. This method moves the current position
     * of the supplied iterator; if this isn't safe, make a copy of the iterator
     * first by calling getAnother(). The supplied iterator must be positioned
     * before the first item (there must have been no call on next()).
     * @return the number of items in the underlying sequence
     * @throws client.net.sf.saxon.ce.trans.XPathException if a failure occurs reading the input sequence
     */

    public static int count(SequenceIterator iter) throws XPathException {
        if ((iter.getProperties() & SequenceIterator.LAST_POSITION_FINDER) != 0) {
            return ((LastPositionFinder)iter).getLastPosition();
        } else {
            int n = 0;
            while (iter.next() != null) {
                n++;
            }
            return n;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


