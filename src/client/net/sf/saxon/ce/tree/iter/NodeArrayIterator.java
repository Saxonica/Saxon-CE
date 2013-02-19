package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Value;

/**
 * An iterator over an array of nodes. This is the same as
 * {@link client.net.sf.saxon.ce.tree.iter.ArrayIterator}, except that the iterator is an {@link AxisIterator}
 */
public class NodeArrayIterator extends ArrayIterator implements AxisIterator {

    public NodeArrayIterator(NodeInfo[] nodes) {
        super(nodes);
    }

    public NodeArrayIterator(NodeInfo[] nodes, int start, int end) {
        super(nodes, start, end);
    }

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext() {
        return (next() != null);
    }

    /**
     * Return an iterator over an axis, starting at the current node.
     *
     * @param axis the axis to iterate over, using a constant such as
     *             {@link client.net.sf.saxon.ce.om.Axis#CHILD}
     * @param test a predicate to apply to the nodes before returning them.
     * @throws NullPointerException if there is no current node
     */

    public AxisIterator iterateAxis(byte axis, NodeTest test) {
        return ((NodeInfo)current()).iterateAxis(axis, test);
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        return ((NodeInfo)current()).getTypedValue();
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        return ((NodeInfo)current()).getStringValueCS();
    }

    /**
     * Get another iterator over the same items
     *
     * @return a new ArrayIterator
     */
    public SequenceIterator getAnother() {
        return new NodeArrayIterator((NodeInfo[])items, start, end);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
