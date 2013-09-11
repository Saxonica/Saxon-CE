package client.net.sf.saxon.ce.value;
import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.ExpressionTool;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.functions.Count;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;

/**
* A value is the result of an expression but it is also an expression in its own right.
* Note that every value can be regarded as a sequence - in many cases, a sequence of
* length one.
*/

public abstract class Value
        implements ValueRepresentation {

    /**
     * Static method to make a Value from a given Item (which may be either an AtomicValue
     * or a NodeInfo or a FunctionItem
     * @param val       The supplied value, or null, indicating the empty sequence.
     * @return          The supplied value, if it is a value, or a SingletonNode that
     *                  wraps the item, if it is a node. If the supplied value was null,
     *                  return an EmptySequence
     */

    public static Value asValue(ValueRepresentation val) {
        if (val instanceof Value) {
            return (Value)val;
        } else if (val == null) {
            return EmptySequence.getInstance();
        } else {
            return new SingletonItem((Item)val);
        }
    }

    /**
     * Static method to make an Item from a Value
     * @param value the value to be converted
     * @return null if the value is an empty sequence; or the only item in the value
     * if it is a singleton sequence
     * @throws XPathException if the Value contains multiple items
     */

    public static Item asItem(ValueRepresentation value) throws XPathException {
        if (value instanceof Item) {
            return (Item)value;
        } else {
            SequenceIterator iter = ((Value)value).iterate();
            Item item = iter.next();
            if (item == null) {
                return null;
            } else if (iter.next() != null) {
                throw new XPathException("Attempting to access a sequence as a singleton item");
            } else {
                return item;
            }
        }
    }

    /**
     * Static method to get an Iterator over any ValueRepresentation (which may be either a Value
     * or a NodeInfo or a FunctionItem
     * @param val       The supplied value, or null, indicating the empty sequence.
     * @return          The supplied value, if it is a value, or a SingletonNode that
     *                  wraps the item, if it is a node. If the supplied value was null,
     *                  return an EmptySequence
     */

    public static SequenceIterator asIterator(ValueRepresentation val) throws XPathException {
        if (val instanceof Value) {
            return ((Value)val).iterate();
        } else if (val == null) {
            return EmptyIterator.getInstance();
        } else {
            return SingletonIterator.makeIterator((Item)val);
        }
    }

    /**
     * Iterate over the items contained in this value.
     * @return an iterator over the sequence of items
     * @throws XPathException if a dynamic error occurs. This is possible only in the case of values
     * that are materialized lazily, that is, where the iterate() method leads to computation of an
     * expression that delivers the values.
     */

    public abstract SequenceIterator iterate() throws XPathException;


    /**
     * Determine the data type of the items in the expression, if possible
     * @return for the default implementation: AnyItemType (not known)
     */

    public ItemType getItemType() {
        return AnyItemType.getInstance();
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     * numbered zero. If n is negative or >= the length of the sequence, returns null.
     */

    public Item itemAt(int n) throws XPathException {
        if (n < 0) {
            return null;
        }
        int i = 0;        // indexing is zero-based
        SequenceIterator iter = iterate();
        while (true) {
            Item item = iter.next();
            if (item == null) {
                return null;
            }
            if (i++ == n) {
                return item;
            }
        }
    }

    /**
     * Get the length of the sequence
     * @return the number of items in the sequence
     */

    public int getLength() throws XPathException {
        return Count.count(iterate());
    }

    /**
      * Process a value in push mode, without returning any tail calls
      * @param iterator iterator over the value to be pushed
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public static void process(SequenceIterator iterator, XPathContext context) throws XPathException {
        SequenceReceiver out = context.getReceiver();
        while (true) {
            Item it = iterator.next();
            if (it==null) break;
            out.append(it, NodeInfo.ALL_NAMESPACES);
        }
    }


    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list.
     * @throws XPathException The method can fail if evaluation of the value
     * has been deferred, and if a failure occurs during the deferred evaluation.
     * No failure is possible in the case of an AtomicValue.
     */

    public String getStringValue() throws XPathException {
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        SequenceIterator iter = iterate();
        Item item = iter.next();
        if (item != null) {
            while (true) {
                sb.append(item.getStringValue());
                item = iter.next();
                if (item == null) {
                    break;
                }
                sb.append(' ');
            }
        }
        return sb.toString();
    }


    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue() throws XPathException {
        return ExpressionTool.effectiveBooleanValue(iterate());
    }

    /**
     * Compare two (sequence) values for equality. This method throws an UnsupportedOperationException,
     * because it should not be used: there are too many "equality" operators that can be defined on
     * values for the concept to be meaningful.
     * <p>Consider creating an XPathComparable from each value, and comparing those; or creating a
     * SchemaComparable to achieve equality comparison as defined in XML Schema.</p>
     * @throws UnsupportedOperationException (always)
     */

    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("Value.equals()");
    }

    public int hashCode() {
        return 42;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
