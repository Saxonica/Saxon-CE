package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Value;

/**
 * This is an iterator over a sequence whose first item has already been read. On entry, the baseIterator
 * must be positioned so the second item in the sequence is the next item to be returned; the first item
 * in the sequence is available by calling current() on the baseIterator.
 *
 * <p>This avoids the cost of calling getAnother() to re-read the first item (which itself can be an
 * expensive operation, for example if it involves calling a user function).</p>
 */
public class OneItemGoneIterator
        implements SequenceIterator, LastPositionFinder, GroundedIterator {

    private SequenceIterator baseIterator;
    private boolean catchingUp;

    /**
     * Create an iterator that delivers all the items that the base iterator delivers, even
     * though the first item of the base iterator has already been read
     * @param baseIterator the base iterator, whose current position must be 1
     */

    public OneItemGoneIterator(SequenceIterator baseIterator) {
        this.baseIterator = baseIterator;
        if (baseIterator.position() != 1) {
            throw new IllegalStateException();
        }
        this.catchingUp = true;
    }

    /**
     * Get the next item in the sequence. This method changes the state of the
     * iterator, in particular it affects the result of subsequent calls of
     * position() and current().
     * @return the next item, or null if there are no more items. Once a call
     *         on next() has returned null, no further calls should be made. The preferred
     *         action for an iterator if subsequent calls on next() are made is to return
     *         null again, and all implementations within Saxon follow this rule.
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error occurs retrieving the next item
     * @since 8.4
     */

    public Item next() throws XPathException {
        if (catchingUp) {
            catchingUp = false;
            return baseIterator.current();
        } else {
            return baseIterator.next();
        }
    }

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next(). This method does not change the state of the iterator.
     * @return the current item, the one most recently returned by a call on
     *         next(). Returns null if next() has not been called, or if the end
     *         of the sequence has been reached.
     * @since 8.4
     */

    public Item current() {
        return (catchingUp ? null : baseIterator.current());
    }

    /**
     * Get the current position. This will usually be zero before the first call
     * on next(), otherwise it will be the number of times that next() has
     * been called. Once next() has returned null, the preferred action is
     * for subsequent calls on position() to return -1, but not all existing
     * implementations follow this practice. (In particular, the EmptyIterator
     * is stateless, and always returns 0 as the value of position(), whether
     * or not next() has been called.)
     * <p/>
     * This method does not change the state of the iterator.
     * @return the current position, the position of the item returned by the
     *         most recent call of next(). This is 1 after next() has been successfully
     *         called once, 2 after it has been called twice, and so on. If next() has
     *         never been called, the method returns zero. If the end of the sequence
     *         has been reached, the value returned will always be <= 0; the preferred
     *         value is -1.
     * @since 8.4
     */

    public int position() {
        return (catchingUp ? 0 : baseIterator.position());
    }

    /**
     * Get another SequenceIterator that iterates over the same items as the original,
     * but which is repositioned at the start of the sequence.
     * <p/>
     * This method allows access to all the items in the sequence without disturbing the
     * current position of the iterator. Internally, its main use is in evaluating the last()
     * function.
     * <p/>
     * This method does not change the state of the iterator.
     * @return a SequenceIterator that iterates over the same items,
     *         positioned before the first item
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error occurs
     * @since 8.4
     */

    public SequenceIterator getAnother() throws XPathException {
        return baseIterator.getAnother();
    }

    /**
     * Get the last position (that is, the number of items in the sequence). This method is
     * non-destructive: it does not change the state of the iterator.
     * The result is undefined if the next() method of the iterator has already returned null.
     * This method returns -1 if the last position cannot be determined.
     */

    public int getLastPosition() throws XPathException {
        if (baseIterator instanceof LastPositionFinder) {
            return ((LastPositionFinder)baseIterator).getLastPosition();
        } else {
            return -1;
        }
    }

    /**
     * Return a GroundedValue containing all the items in the sequence returned by this
     * SequenceIterator. This should be an "in-memory" value, not a Closure.
     * @return the corresponding Value if the base iterator is grounded, or null otherwise.
     */

    public Value materialize() {
        if (baseIterator instanceof GroundedIterator) {
            return ((GroundedIterator)baseIterator).materialize();
        } else {
            return null;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


