package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.SingletonItem;
import client.net.sf.saxon.ce.value.Value;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.GroundedValue;


/**
* SingletonIterator: an iterator over a sequence of zero or one values
*/

public class SingleNodeIterator implements AxisIterator, UnfailingIterator,
        LastPositionFinder, GroundedIterator {

    private NodeInfo item;
    private int position = 0;

    /**
     * Private constructor: external classes should use the factory method
     * @param value the item to iterate over
     */

    private SingleNodeIterator(NodeInfo value) {
        this.item = value;
    }

   /**
    * Factory method.
    * @param item the item to iterate over
    * @return a SingletonIterator over the supplied item, or an EmptyIterator
    * if the supplied item is null.
    */

    public static AxisIterator makeIterator(NodeInfo item) {
       if (item==null) {
           return EmptyIterator.getInstance();
       } else {
           return new SingleNodeIterator(item);
       }
   }

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext() {
        return next() != null;
    }

    public Item next() {
        if (position == 0) {
            position = 1;
            return item;
        } else if (position == 1) {
            position = -1;
            return null;
        } else {
            return null;
        }
    }

    public Item current() {
        if (position == 1) {
            return item;
        } else {
            return null;
        }
    }

    /**
     * Return the current position in the sequence.
     * @return 0 before the first call on next(); 1 before the second call on next(); -1 after the second
     * call on next().
     */
    public int position() {
       return position;
    }

    public int getLastPosition() {
        return 1;
    }

    /**
     * Return an iterator over an axis, starting at the current node.
     *
     * @param axis the axis to iterate over, using a constant such as
     *             {@link client.net.sf.saxon.ce.om.Axis#CHILD}
     * @param test a predicate to apply to the nodes before returning them.
     */

    public AxisIterator iterateAxis(byte axis, NodeTest test) {
        if (position == 1) {
            return item.iterateAxis(axis, test);
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        if (position == 1) {
            return item.getTypedValue();
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        if (position == 1) {
            return item.getStringValue();
        } else {
            throw new NullPointerException();
        }
    }


    public SequenceIterator getAnother() {
        return new SingleNodeIterator(item);
    }

    public SequenceIterator getReverseIterator() {
        return new SingleNodeIterator(item);
    }

    public Item getValue() {
        return item;
    }

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding Value. If the value is a closure or a function call package, it will be
     * evaluated and expanded.
     */

    public GroundedValue materialize() {
        return new SingletonItem(item);
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
        return GROUNDED | LAST_POSITION_FINDER;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
