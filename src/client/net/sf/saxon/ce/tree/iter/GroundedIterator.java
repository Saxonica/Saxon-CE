package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.value.Value;

/**
 * This interface is an extension to the SequenceIterator interface; it represents
 * a SequenceIterator that is based on an in-memory representation of a sequence,
 * and that is therefore capable of returned a SequenceValue containing all the items
 * in the sequence.
 */

public interface GroundedIterator extends SequenceIterator {

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator. This should involve no computation, and throws no errors.
     * @return the corresponding Value, or null if the value is not known
     */

    public Value materialize();
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

