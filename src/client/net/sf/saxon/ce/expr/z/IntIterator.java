package client.net.sf.saxon.ce.expr.z;

/**
 * An iterator over a sequence of unboxed int values
 */
public interface IntIterator {

    /**
     * Test whether there are any more integers in the sequence
     * @return true if there are more integers to come
     */

    public boolean hasNext();

    /**
     * Return the next integer in the sequence. The result is undefined unless hasNext() has been called
     * and has returned true.
     * @return the next integer in the sequence
     */

    public int next();
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
