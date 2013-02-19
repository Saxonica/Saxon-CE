package client.net.sf.saxon.ce.expr;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over a single object (typically a sub-expression of an expression)
 */
public class MonoIterator implements Iterator {

    private Object thing;  // the single object in the collection
    private boolean gone;  // true if the single object has already been returned

    /**
     * Create an iterator of the single object supplied
     * @param thing the object to be iterated over
     */

    public MonoIterator(Object thing) {
        gone = false;
        this.thing = thing;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */

    public boolean hasNext() {
        return !gone;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @exception NoSuchElementException iteration has no more elements.
     */

    public Object next() {
        if (gone) {
            throw new NoSuchElementException();
        } else {
            gone = true;
            return thing;
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
     *		  operation is not supported by this Iterator (which is the
     *        case for this iterator).
     */

    public void remove() {
        throw new UnsupportedOperationException();
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
