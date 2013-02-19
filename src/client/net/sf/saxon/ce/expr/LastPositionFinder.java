package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* A LastPositionFinder is an interface implemented by any SequenceIterator that is
 * able to return the position of the last item in the sequence.
*/

public interface LastPositionFinder  {

    /**
    * Get the last position (that is, the number of items in the sequence). This method is
    * non-destructive: it does not change the state of the iterator.
    * The result is undefined if the next() method of the iterator has already returned null.
    * This method must not be called unless the result of getProperties() on the iterator
     * includes the bit setting {@link client.net.sf.saxon.ce.om.SequenceIterator#LAST_POSITION_FINDER}
    */

    public int getLastPosition() throws XPathException;

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
