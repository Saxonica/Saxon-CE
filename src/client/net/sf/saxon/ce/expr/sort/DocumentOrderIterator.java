package client.net.sf.saxon.ce.expr.sort;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.SequenceExtent;

/**
* DocumentOrderIterator takes as input an iteration of nodes in any order, and
* returns as output an iteration of the same nodes in document order, eliminating
* any duplicates.
*/

public final class DocumentOrderIterator implements SequenceIterator, Sortable {

    private SequenceIterator iterator;
    private SequenceExtent sequence;
    private NodeOrderComparer comparer;
    private NodeInfo current = null;

    /**
    * Iterate over a sequence in document order.
    */

    public DocumentOrderIterator(SequenceIterator base, NodeOrderComparer comparer) throws XPathException {

        this.comparer = comparer;

        sequence = new SequenceExtent(base);
        //System.err.println("sort into document order: sequence length = " + sequence.getLength());
        if (sequence.getLength()>1) {
            //QuickSort.sort(this, 0, sequence.getLength()-1);
            GenericSorter.quickSort(0, sequence.getLength(), this);
            //GenericSorter.mergeSort(0, sequence.getLength(), this);
        }
        iterator = sequence.iterate();
    }

    /**
    * Private constructor used only by getAnother()
    */

    private DocumentOrderIterator() {}

    /**
    * Compare two nodes in document sequence
    * (needed to implement the Sortable interface)
    */

    public int compare(int a, int b) {
        //System.err.println("compare " + a + " with " + b);
        return comparer.compare((NodeInfo)sequence.itemAt(a),
                                (NodeInfo)sequence.itemAt(b));
    }

    /**
    * Swap two nodes (needed to implement the Sortable interface)
    */

    public void swap(int a, int b) {
        sequence.swap(a, b);
    }

    // Implement the SequenceIterator as a wrapper around the underlying iterator
    // over the sequenceExtent, but looking ahead to remove duplicates.

    public Item next() throws XPathException {
        while (true) {
            NodeInfo next = (NodeInfo)iterator.next();
            if (next == null) {
                current = null;
                return null;
            }
            if (current != null && next.isSameNodeInfo(current)) {
                continue;
            } else {
                current = next;
                return current;
            }
        }
    }

    public SequenceIterator getAnother() throws XPathException {
        DocumentOrderIterator another = new DocumentOrderIterator();
        another.iterator = iterator.getAnother();    // don't need to sort it again
        return another;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
