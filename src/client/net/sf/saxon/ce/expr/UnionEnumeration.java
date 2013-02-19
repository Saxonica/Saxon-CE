package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.expr.sort.NodeOrderComparer;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* An enumeration representing a nodeset that is a union of two other NodeSets.
*/

public class UnionEnumeration implements SequenceIterator {

    private SequenceIterator e1;
    private SequenceIterator e2;
    private NodeInfo nextNode1 = null;
    private NodeInfo nextNode2 = null;
    private NodeOrderComparer comparer;
    private NodeInfo current = null;
    private int position = 0;

    /**
    * Create the iterator. The two input iterators must return nodes in document
    * order for this to work.
     * @param p1 iterator over the first operand sequence (in document order)
     * @param p2 iterator over the second operand sequence
     * @param comparer used to test whether nodes are in document order. Different versions
     * are used for intra-document and cross-document operations
    */

    public UnionEnumeration(SequenceIterator p1, SequenceIterator p2,
                            NodeOrderComparer comparer) throws XPathException {
        this.e1 = p1;
        this.e2 = p2;
        this.comparer = comparer;

        nextNode1 = next(e1);
        nextNode2 = next(e2);
    }

    /**
     * Get the next item from one of the input sequences,
     * checking that it is a node.
     * @param iter the sequence from which a node is to be read
     * @return the node that was read
    */

    private NodeInfo next(SequenceIterator iter) throws XPathException {
        return (NodeInfo)iter.next();
        // we rely on the type-checking mechanism to prevent a ClassCastException here
    }

    public Item next() throws XPathException {

        // main merge loop: take a value from whichever set has the lower value

        position++;
        if (nextNode1 != null && nextNode2 != null) {
            int c = comparer.compare(nextNode1, nextNode2);
            if (c<0) {
                current = nextNode1;
                nextNode1 = next(e1);
                return current;

            } else if (c>0) {
                current = nextNode2;
                nextNode2 = next(e2);
                return current;

            } else {
                current = nextNode2;
                nextNode2 = next(e2);
                nextNode1 = next(e1);
                return current;
            }
        }

        // collect the remaining nodes from whichever set has a residue

        if (nextNode1!=null) {
            current = nextNode1;
            nextNode1 = next(e1);
            return current;
        }
        if (nextNode2!=null) {
            current = nextNode2;
            nextNode2 = next(e2);
            return current;
        }
        current = null;
        position = -1;
        return null;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        return new UnionEnumeration(e1.getAnother(), e2.getAnother(), comparer);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER}. It is always
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
