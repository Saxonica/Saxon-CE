package client.net.sf.saxon.ce.om;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * A SequenceIterator is used to iterate over any XPath 2 sequence (of values or nodes).
 * To get the next item in a sequence, call next(); if this returns null, you've
 * reached the end of the sequence.
 * <p>
 * A SequenceIterator keeps track of the current Item and the current position.
 * The objects returned by the SequenceIterator will always be either nodes
 * (class NodeInfo) or singleton values (class AtomicValue): these are represented
 * collectively by the interface Item.
 * <p>
 * This interface forms part of the public Saxon API. The JavaDoc "since" flag is used from
 * release 8.4 onwards to indicate methods that are considered to be a stable part
 * of the API. Methods without a "since" flag should not be regarded as a stable part
 * of the API.
 * <p>
 * Note that the stability of this interface applies to classes that use the interface,
 * not to classes that implement it. The interface may be extended in future to add new methods.
 *
 * @author Michael H. Kay
 * @since 8.4
 */

public interface SequenceIterator {

    /**
     * Get the next item in the sequence. This method changes the state of the
     * iterator, in particular it affects the result of subsequent calls of
     * position() and current().
     * @throws XPathException if an error occurs retrieving the next item
     * @return the next item, or null if there are no more items. Once a call
     * on next() has returned null, no further calls should be made. The preferred
     * action for an iterator if subsequent calls on next() are made is to return
     * null again, and all implementations within Saxon follow this rule.
     * @since 8.4
     */

    public Item next() throws XPathException;

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next(). This method does not change the state of the iterator.
     *
     * @return the current item, the one most recently returned by a call on
     *     next(). Returns null if next() has not been called, or if the end
     *     of the sequence has been reached.
     * @since 8.4
     */

    public Item current();

    /**
     * Get the current position. This will usually be zero before the first call
     * on next(), otherwise it will be the number of times that next() has
     * been called. Once next() has returned null, the preferred action is
     * for subsequent calls on position() to return -1, but not all existing
     * implementations follow this practice. (In particular, the EmptyIterator
     * is stateless, and always returns 0 as the value of position(), whether
     * or not next() has been called.)
     * <p>
     * This method does not change the state of the iterator.
     *
     * @return the current position, the position of the item returned by the
     *     most recent call of next(). This is 1 after next() has been successfully
     *     called once, 2 after it has been called twice, and so on. If next() has
     *     never been called, the method returns zero. If the end of the sequence
     *     has been reached, the value returned will always be <= 0; the preferred
     *     value is -1.
     *
     * @since 8.4
     */

    public int position();

    /**
     * Get another SequenceIterator that iterates over the same items as the original,
     * but which is repositioned at the start of the sequence.
     * <p>
     * This method allows access to all the items in the sequence without disturbing the
     * current position of the iterator. Internally, its main use is in evaluating the last()
     * function.
     * <p>
     * This method does not change the state of the iterator.
     *
     * @exception XPathException if any error occurs
     * @return a SequenceIterator that iterates over the same items,
     *     positioned before the first item
     * @since 8.4
     */

    public SequenceIterator getAnother() throws XPathException;

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
