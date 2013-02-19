package client.net.sf.saxon.ce.expr;

import java.util.Iterator;

/**
 * An iterator that combines the results of a sequence of iterators
 */
public class MultiIterator implements Iterator {

    private Iterator[] array;
    private int current;

    /**
     * Create an iterator that concatenates a number of supplied iterators
     * @param array the iterators to be concatenated
     */

    public MultiIterator(Iterator[] array) {
        this.array = array;
        current = 0;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */

    public boolean hasNext() {
        while (true) {
            if (current >= array.length) {
                return false;
            }
            if (array[current].hasNext()) {
                return true;
            }
            current++;
        }
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @exception java.util.NoSuchElementException iteration has no more elements.
     */
    public Object next() {
        return array[current].next();
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
