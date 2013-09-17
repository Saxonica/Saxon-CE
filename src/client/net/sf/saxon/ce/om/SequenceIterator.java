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
