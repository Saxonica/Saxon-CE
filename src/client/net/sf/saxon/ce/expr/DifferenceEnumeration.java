package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.expr.sort.NodeOrderComparer;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* An enumeration representing a nodeset that is teh difference of two other NodeSets.
* There is an "except" operator in XPath 2.0 to create such an expression.
*/


public class DifferenceEnumeration implements SequenceIterator {


    private SequenceIterator p1;
    private SequenceIterator p2;

    private NodeInfo nextNode1 = null;
    private NodeInfo nextNode2 = null;
    private NodeOrderComparer comparer;

    //private NodeInfo nextNode = null;
    private NodeInfo current = null;
    private int position = 0;

    /**
    * Form an enumeration of the difference of two nodesets, that is, the nodes
    * that are in p1 and that are not in p2.
    * @param p1 the first operand, with nodes delivered in document order
    * @param p2 the second operand, with nodes delivered in document order
    * @param comparer the comparer
    */

    public DifferenceEnumeration(SequenceIterator p1, SequenceIterator p2,
                                 NodeOrderComparer comparer) throws XPathException {
        this.p1 = p1;
        this.p2 = p2;
        this.comparer = comparer;

        // move to the first node in each input nodeset

        nextNode1 = next(p1);
        nextNode2 = next(p2);
    }

    /**
    * Get the next item from one of the input sequences,
    * checking that it is a node.
     * @param iter the iterator from which the next node is to be read
     * @return the node that was read, or null if the stream is exhausted
    */

    private NodeInfo next(SequenceIterator iter) throws XPathException {
        return (NodeInfo)iter.next();
        // rely on type-checking to prevent a ClassCastException
    }

    public Item next() throws XPathException {
        // main merge loop: if the node in p1 has a lower key value that that in p2, return it;
        // if they are equal, advance both nodesets; if p1 is higher, advance p2.

        while (true) {

            if (nextNode1 == null) {
                current = null;
                position = -1;
                return null;
            }

            if (nextNode2 == null) {
                // second node-set is exhausted; return the next node from the first node-set
                return deliver();
            }

            int c = comparer.compare(nextNode1, nextNode2);
            if (c<0) {                              // p1 is lower
                return deliver();

            } else if (c>0) {                       // p1 is higher
                nextNode2 = next(p2);
                if (nextNode2 == null) {
                    return deliver();
                }

            } else {                                // keys are equal
                nextNode2 = next(p2);
                nextNode1 = next(p1);
            }
        }
    }

    /**
     * Deliver the next node from the first node-set, advancing the iterator to
     * look-ahead for the next item, and setting the current and position variables.
     * @return the next node from the first node-set
     * @throws XPathException
     */
    private NodeInfo deliver() throws XPathException {
        current = nextNode1;
        nextNode1 = next(p1);
        position++;
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        return new DifferenceEnumeration(p1.getAnother(), p2.getAnother(), comparer);
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
