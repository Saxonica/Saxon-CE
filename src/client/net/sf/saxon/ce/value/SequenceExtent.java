package client.net.sf.saxon.ce.value;
import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.ArrayIterator;
import client.net.sf.saxon.ce.tree.iter.GroundedIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * A sequence value implemented extensionally. That is, this class represents a sequence
 * by allocating memory to each item in the sequence.
 */

public final class SequenceExtent implements Sequence {
    transient private Item[] value;
    private ItemType itemType = null;   // memoized

//    private static int instances = 0;

    /**
     * Construct an sequence from an array of items. Note, the array of items is used as is,
     * which means the caller must not subsequently change its contents.
     *
     * @param items the array of items to be included in the sequence
     */

    public SequenceExtent(Item[] items) {
        value = items;
    }

    /**
     * Construct a SequenceExtent from a List. The members of the list must all
     * be Items
     *
     * @param list the list of items to be included in the sequence
     */

    public SequenceExtent(List<? extends Item> list) {
        Item[] array = new Item[list.size()];
        value = list.toArray(array);
    }

    /**
     * Construct a sequence containing all the items in a SequenceIterator.
     *
     * @exception client.net.sf.saxon.ce.trans.XPathException if reading the items using the
     *     SequenceIterator raises an error
     * @param iter The supplied sequence of items. This must be positioned at
     *     the start, so that hasNext() returns true if there are any nodes in
     *      the node-set, and next() returns the first node.
     */

    public SequenceExtent(SequenceIterator iter) throws XPathException {
        int allocated = -1;
        if (iter instanceof LastPositionFinder) {
            allocated = ((LastPositionFinder)iter).getLastPosition();
        }
        if (allocated == -1) {
            List<Item> list = new ArrayList<Item>(20);
            while (true) {
                Item it = iter.next();
                if (it == null) {
                    break;
                }
                list.add(it);
            }
            Item[] array = new Item[list.size()];
            value = list.toArray(array);
        } else {
            value = new Item[allocated];
            int i = 0;
            while (true) {
                Item it = iter.next();
                if (it == null) {
                    break;
                }
                value[i++] = it;
            }
        }
    }

    /**
     * Factory method to make a Value holding the contents of any SequenceIterator
     * @param iter a Sequence iterator that will be consumed to deliver the items in the sequence
     * @return a ValueRepresentation holding the items delivered by the SequenceIterator. If the
     * sequence is empty the result will be an instance of {@link EmptySequence}. If it is of length
     * one, the result will be an {@link Item}. In all other cases, it will be an instance of
     * {@link SequenceExtent}.
     * @throws XPathException if a dynamic error occurs while evaluating the iterator
     */

    public static Sequence makeSequenceExtent(SequenceIterator iter) throws XPathException {
        if (iter instanceof GroundedIterator) {
            Sequence value = ((GroundedIterator)iter).materialize();
            if (value != null) {
                return value;
            }
        }
        SequenceExtent extent = new SequenceExtent(iter);
        int len = extent.getLength();
        if (len==0) {
            return EmptySequence.getInstance();
        } else if (len==1) {
            return extent.itemAt(0);
        } else {
            return extent;
        }
    }

    /**
     * Factory method to make a Value holding the supplied items in reverse order
     * @param iter iterator over a List containing the items in the sequence
     * @return a ValueRepresentation holding the items in the list, in reverse
     * order of the supplied iterator
     * @throws XPathException if an error occurs evaluating the sequence
     */

    public static SequenceExtent makeReversed(SequenceIterator iter) throws XPathException {
        LinkedList<Item> list = new LinkedList<Item>();
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            list.addFirst(item);
        }
        return new SequenceExtent(list);
    }

    /**
     * Factory method to make a Value holding the contents of any List of items
     * @param input a List containing the items in the sequence
     * @return a ValueRepresentation holding the items in the list. If the
     * sequence is empty the result will be an instance of {@link EmptySequence}. If it is of length
     * one, the result will be an {@link Item}. In all other cases, it will be an instance of
     * {@link SequenceExtent}.
     */

    public static Sequence makeSequenceExtent(List<Item> input) throws XPathException {
        int len = input.size();
        if (len==0) {
            return EmptySequence.getInstance();
        } else if (len==1) {
            return input.get(0);
        } else {
            return new SequenceExtent(input);
        }
    }

    /**
     * Simplify this SequenceExtent
     * @return a Value holding the items delivered by the SequenceIterator. If the
     * sequence is empty the result will be an instance of {@link EmptySequence}. If it is of length
     * one, the result will be an {@link AtomicValue} or a {@link client.net.sf.saxon.ce.om.NodeInfo}.
     * In all other cases, the {@link SequenceExtent} will be returned unchanged.
     */

    public Sequence simplify() {
        int n = getLength();
        if (n == 0) {
            return EmptySequence.getInstance();
        } else if (n == 1) {
            return itemAt(0);
        } else {
            return this;
        }
    }

    /**
     * Get the number of items in the sequence
     *
     * @return the number of items in the sequence
     */

    public int getLength() {
        return value.length;
    }

    /**
     * Get the (lowest common) item type
     *
     * @return integer identifying an item type to which all the items in this
     *      sequence conform
     */

    public ItemType getItemType() {
        if (itemType != null) {
            // only calculate it the first time
            return itemType;
        }
        itemType = Type.getItemType(value[0]);
        for (int i=1; i<value.length; i++) {
            if (itemType == AnyItemType.getInstance()) {
                // make a quick exit
                return itemType;
            }
            itemType = Type.getCommonSuperType(itemType, Type.getItemType(value[i]));
        }
        return itemType;
    }

    /**
     * Get the n'th item in the sequence (starting with 0 as the first item)
     *
     * @param n the position of the required item
     * @return the n'th item in the sequence, zero-based, or null if n is out of range
     */

    public Item itemAt(int n) {
        if (n<0 || n>=getLength()) {
            return null;
        } else {
            return value[n];
        }
    }

    /**
     * Swap two items (needed to support sorting)
     *
     * @param a the position of the first item to be swapped
     * @param b the position of the second item to be swapped
     */

    public void swap(int a, int b) {
        Item temp = value[a];
        value[a] = value[b];
        value[b] = temp;
    }

    /**
     * Return an iterator over this sequence.
     *
     * @return the required SequenceIterator, positioned at the start of the
     *     sequence
     */

    public UnfailingIterator iterate() {
        return new ArrayIterator(value);
    }


    public String toString() {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
        fsb.append('(');
        for (int i=0; i<value.length; i++) {
            fsb.append(value[i].toString());
            if (i != value.length-1) {
                fsb.append(", ");
            }
        }
        fsb.append(')');
        return fsb.toString();
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

