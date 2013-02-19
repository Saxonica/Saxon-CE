package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.value.Cardinality;


/**
 * CardinalityCheckingIterator returns the items in an underlying sequence
 * unchanged, but checks that the number of items conforms to the required
 * cardinality. Because cardinality checks are required to take place even
 * if the consumer of the sequence does not require all the items, we read
 * the first two items at initialization time. This is sufficient to perform
 * the checks; after that we can simply return the items from the base sequence.
 */

public final class CardinalityCheckingIterator implements SequenceIterator {

    private SequenceIterator base;
    private int requiredCardinality;
    private RoleLocator role;
    private SourceLocator locator;
    private Item first = null;
    private Item second = null;
    private Item current = null;
    private int position = 0;

    /**
     * Construct an CardinalityCheckingIterator that will return the same items as the base sequence,
     * checking how many there are
     *
     * @param base   the base iterator
     * @param requiredCardinality the required Cardinality
     */

    public CardinalityCheckingIterator(SequenceIterator base, int requiredCardinality,
                                       RoleLocator role, SourceLocator locator)
            throws XPathException {
        this.base = base;
        this.requiredCardinality = requiredCardinality;
        this.role = role;
        this.locator = locator;
        first = base.next();
        if (first==null) {
            if (!Cardinality.allowsZero(requiredCardinality)) {
                typeError("An empty sequence is not allowed as the " +
                             role.getMessage(), role.getErrorCode());
            }
        } else {
            if (requiredCardinality == StaticProperty.EMPTY) {
                typeError("The only value allowed for the " +
                             role.getMessage() + " is an empty sequence", role.getErrorCode());
            }
            second = base.next();
            if (second!=null && !Cardinality.allowsMany(requiredCardinality)) {
                typeError( "A sequence of more than one item is not allowed as the " +
                            role.getMessage() + CardinalityChecker.depictSequenceStart(base.getAnother(), 2),
                            role.getErrorCode());
            }
        }
    }

    public Item next() throws XPathException {
        if (position < 2) {
            if (position == 0) {
                current = first;
                position = (first==null ? -1 : 1);
                return current;
            } else if (position == 1) {
                current = second;
                position = (second==null ? -1 : 2);
                return current;
            } else {
                // position == -1
                return null;
            }
        }
        current = base.next();
        if (current == null) {
            position = -1;
        } else {
            position++;
        }
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        return new CardinalityCheckingIterator(base.getAnother(), requiredCardinality, role, locator);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link client.net.sf.saxon.ce.om.SequenceIterator#GROUNDED},
     *         {@link client.net.sf.saxon.ce.om.SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link client.net.sf.saxon.ce.om.SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }

    private void typeError(String message, String errorCode) throws XPathException {
        XPathException e = new XPathException(message, locator);
        e.setIsTypeError(true);
        e.setErrorCode(errorCode);
        throw e;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.