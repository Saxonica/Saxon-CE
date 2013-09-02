package client.net.sf.saxon.ce.tree.wrapper;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.value.Value;

/**
 * A WrappingIterator delivers wrappers for the nodes delivered
 * by its underlying iterator. It is used when no whitespace stripping
 * is actually needed, e.g. for the attribute axis. But we still need to
 * create wrappers, so that further iteration remains in the virtual layer
 * rather than switching to the real nodes.
 */

public class WrappingIterator implements AxisIterator {

    AxisIterator base;
    VirtualNode parent;
    NodeInfo current;
    boolean atomizing = false;
    WrappingFunction wrappingFunction;

    /**
     * Create a WrappingIterator
     *
     * @param base   The underlying iterator
     * @param parent If all the nodes to be wrapped have the same parent,
     *               it can be specified here. Otherwise specify null.
     */

    public WrappingIterator(AxisIterator base, WrappingFunction function, VirtualNode parent) {
        this.base = base;
        this.wrappingFunction = function;
        this.parent = parent;
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


    public Item next() {
        Item n = base.next();
        if (n instanceof NodeInfo && !atomizing) {
            current = wrappingFunction.makeWrapper((NodeInfo) n, parent);
        } else {
            current = (NodeInfo) n;
        }
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return base.position();
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
        return current.iterateAxis(axis, test);
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        return current.getTypedValue();
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        return current.getStringValue();
    }

    public SequenceIterator getAnother() {
        return new WrappingIterator((AxisIterator) base.getAnother(), wrappingFunction, parent);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
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

