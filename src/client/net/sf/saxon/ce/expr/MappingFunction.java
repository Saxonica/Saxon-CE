package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* MappingFunction is an interface that must be satisfied by an object passed to a
* MappingIterator. It represents an object which, given an Item, can return a
* SequenceIterator that delivers a sequence of zero or more Items.
*/

public interface MappingFunction {

    /**
    * Map one item to a sequence.
    * @param item The item to be mapped.
    * @return one of the following: (a) a SequenceIterator over the sequence of items that the supplied input
    * item maps to, or (b) null if it maps to an empty sequence.
    */

    public SequenceIterator map(Item item) throws XPathException;

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.