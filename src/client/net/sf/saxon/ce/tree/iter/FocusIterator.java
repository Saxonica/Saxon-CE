package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.functions.Count;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * An iterator that maintains the values of position() and current(), as a wrapper
 * over an iterator which does not maintain these values itself.
 */
public class FocusIterator implements SequenceIterator {

    private SequenceIterator base;
    private Item curr;
    private int pos = 0;
    private int last = -1;

    public FocusIterator(SequenceIterator base) {
        this.base = base;
    }

    /**
     * Get the underlying iterator
     * @return the iterator underlying this FocusIterator
     */

    public SequenceIterator getUnderlyingIterator() {
        return base;
    }

    /**
     * Get the next item in the sequence. This method changes the state of the
     * iterator, in particular it affects the result of subsequent calls of
     * position() and current().
     *
     * @return the next item, or null if there are no more items. Once a call
     *         on next() has returned null, no further calls should be made. The preferred
     *         action for an iterator if subsequent calls on next() are made is to return
     *         null again, and all implementations within Saxon follow this rule.
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error occurs retrieving the next item
     * @since 8.4
     */
    public Item next() throws XPathException {
        curr = base.next();
        if (curr == null) {
            pos = -1;
        } else {
            pos++;
        }
        return curr;
    }

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next(). This method does not change the state of the iterator.
     *
     * @return the current item, the one most recently returned by a call on
     *         next(). Returns null if next() has not been called, or if the end
     *         of the sequence has been reached.
     * @since 8.4
     */
    public Item current() {
        return curr;
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
     *
     * @return the current position, the position of the item returned by the
     *         most recent call of next(). This is 1 after next() has been successfully
     *         called once, 2 after it has been called twice, and so on. If next() has
     *         never been called, the method returns zero. If the end of the sequence
     *         has been reached, the value returned will always be <= 0; the preferred
     *         value is -1.
     * @since 8.4
     */
    public int position() {
        return pos;
    }

    /**
     * Get the position of the last item in the sequence
     * @return the position of the last item
     * @throws XPathException if a failure occurs reading the sequence
     */

    public int last() throws XPathException {
        if (last == -1) {
            if (base instanceof LastPositionFinder) {
                last = ((LastPositionFinder)base).getLastPosition();
            }
            if (last == -1) {
                last = Count.count(base.getAnother());
            }
        }
        return last;
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
     *
     * @return a SequenceIterator that iterates over the same items,
     *         positioned before the first item
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error occurs
     * @since 8.4
     */
    public SequenceIterator getAnother() throws XPathException {
        return new FocusIterator((base.getAnother()));
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
