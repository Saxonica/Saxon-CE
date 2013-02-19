package client.net.sf.saxon.ce.expr;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over a pair of objects (typically sub-expressions of an expression)
 */
public class PairIterator implements Iterator {

    private Object one;
    private Object two;
    private int pos = 0;

    /**
     * Create an iterator over two objects
     * @param one the first object to be returned
     * @param two the second object to be returned
     */

    public PairIterator(Object one, Object two) {
        this.one = one;
        this.two = two;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */

    public boolean hasNext() {
        return pos<2;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @exception NoSuchElementException iteration has no more elements.
     */
    public Object next() {
        switch (pos++) {
            case 0: return one;
            case 1: return two;
            default: throw new NoSuchElementException();
        }
    }

    /**
     *
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation).  This method can be called only once per
     * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     *
     * @exception UnsupportedOperationException if the <tt>remove</tt>
     *		  operation is not supported by this Iterator.

     * @exception IllegalStateException if the <tt>next</tt> method has not
     *		  yet been called, or the <tt>remove</tt> method has already
     *		  been called after the last call to the <tt>next</tt>
     *		  method.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
