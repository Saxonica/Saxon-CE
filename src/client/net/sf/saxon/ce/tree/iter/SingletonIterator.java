package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.SingletonItem;
import client.net.sf.saxon.ce.value.Value;


/**
* SingletonIterator: an iterator over a sequence of zero or one values
*/

public class SingletonIterator implements UnfailingIterator,
        LastPositionFinder, GroundedIterator {

    private Item item;
    private int position = 0;

    /**
     * Private constructor: external classes should use the factory method
     * @param value the item to iterate over
     */

    private SingletonIterator(Item value) {
        this.item = value;
    }

   /**
    * Factory method.
    * @param item the item to iterate over
    * @return a SingletonIterator over the supplied item, or an EmptyIterator
    * if the supplied item is null.
    */

    public static UnfailingIterator makeIterator(Item item) {
       if (item==null) {
           return EmptyIterator.getInstance();
       } else {
           return new SingletonIterator(item);
       }
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

    public SequenceIterator getAnother() {
        return new SingletonIterator(item);
    }

    public SequenceIterator getReverseIterator() {
        return new SingletonIterator(item);
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

    public Value materialize() {
        if (item instanceof AtomicValue) {
            return (AtomicValue)item;
        } else {
            return new SingletonItem(item);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
