package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.z.IntIterator;

/**
 * An iterator over a zero-length sequence of integers
 */
public class EmptyIntIterator implements IntIterator {

    private static EmptyIntIterator THE_INSTANCE = new EmptyIntIterator();

    /**
     * Get the singular instance of this class
     * @return the singular instance
     */

    public static EmptyIntIterator getInstance() {
        return THE_INSTANCE;
    }

    private EmptyIntIterator() {}


    /**
     * Test whether there are any more integers in the sequence
     *
     * @return true if there are more integers to come
     */

    public boolean hasNext() {
        return false;
    }

    /**
     * Return the next integer in the sequence. The result is undefined unless hasNext() has been called
     * and has returned true.
     *
     * @return the next integer in the sequence
     */

    public int next() {
        return 0;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
