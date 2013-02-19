package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.expr.sort.NodeOrderComparer;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* An enumeration representing a nodeset that is an intersection of two other NodeSets.
* This implements the XPath 2.0 operator "intersect".
*/


public class IntersectionEnumeration implements SequenceIterator {

    private SequenceIterator e1;
    private SequenceIterator e2;
    private NodeInfo nextNode1 = null;
    private NodeInfo nextNode2 = null;
    private NodeOrderComparer comparer;

    private NodeInfo current = null;
    private int position = 0;

    /**
    * Form an enumeration of the intersection of the nodes in two nodesets
    * @param p1 the first operand: must be in document order
    * @param p2 the second operand: must be in document order
    * @param comparer Comparer to be used for putting nodes in document order
    */

    public IntersectionEnumeration(SequenceIterator p1, SequenceIterator p2,
                                    NodeOrderComparer comparer ) throws XPathException {
        e1 = p1;
        e2 = p2;
        this.comparer = comparer;

        // move to the first node in each input nodeset

        nextNode1 = next(e1);
        nextNode2 = next(e2);
    }

    /**
     * Get the next item from one of the input sequences,
     * checking that it is a node.
     * @param iter the iterator from which the next item is to be taken
     * @return the next value returned by that iterator
    */

    private NodeInfo next(SequenceIterator iter) throws XPathException {
        return (NodeInfo)iter.next();
        // rely on type-checking to prevent a ClassCastException
    }

    public Item next() throws XPathException {
        // main merge loop: iterate whichever sequence has the lower value, returning when a pair
        // is found that match.

        if (nextNode1 == null || nextNode2 == null) {
            current = null;
            position = -1;
            return null;
        }

        while (nextNode1 != null && nextNode2 != null) {
            int c = comparer.compare(nextNode1, nextNode2);
            if (c<0) {
                nextNode1 = next(e1);
            } else if (c>0) {
                nextNode2 = next(e2);
            } else {            // keys are equal
                current = nextNode2;    // which is the same as nextNode1
                nextNode2 = next(e2);
                nextNode1 = next(e1);
                position++;
                return current;
            }
        }
        return null;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        return new IntersectionEnumeration(e1.getAnother(), e2.getAnother(), comparer);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link SequenceIterator#GROUNDED}, {@link SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
