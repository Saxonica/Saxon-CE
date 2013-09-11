package client.net.sf.saxon.ce.value;
import client.net.sf.saxon.ce.js.JSObjectType;
import client.net.sf.saxon.ce.js.JSObjectValue;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.DocumentNodeTest;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;

/**
* A value that is a sequence containing zero or one items. Used only for items that are not atomic values
 * (that is, nodes, and function items)
*/

public class SingletonItem extends Value implements GroundedValue{

    protected Item item = null;


    /**
     * Create a node-set containing zero or one nodes
     * @param item The node or function-item to be contained in the node-set, or null if the sequence
     * is to be empty
    */

    public SingletonItem(Item item) {
        this.item = item;
    }


    /**
     * Determine the data type of the items in the expression. This method determines the most
     * precise type that it can, because it is called when testing that the node conforms to a required
     * type.
     * @return the most precise possible type of the node.
     */

    public ItemType getItemType() {
        if (item instanceof NodeInfo) {
            NodeInfo node = ((NodeInfo)item);
            switch (node.getNodeKind()) {
                case Type.DOCUMENT:
                    // Need to know whether the document is well-formed and if so what the element type is
                    UnfailingIterator iter = node.iterateAxis(Axis.CHILD);
                    ItemType elementType = null;
                    while (true) {
                        NodeInfo n = (NodeInfo)iter.next();
                        if (n==null) {
                            break;
                        }
                        int kind = n.getNodeKind();
                        if (kind==Type.TEXT) {
                            elementType = null;
                            break;
                        } else if (kind==Type.ELEMENT) {
                            if (elementType != null) {
                                elementType = null;
                                break;
                            }
                            elementType = new SingletonItem(n).getItemType();
                        }
                    }
                    if (elementType == null) {
                        return NodeKindTest.DOCUMENT;
                    } else {
                        return new DocumentNodeTest((NodeTest)elementType);
                    }

                case Type.ELEMENT:
                    return new NameTest(Type.ELEMENT, node.getNodeName());

                case Type.ATTRIBUTE:
                    return new NameTest(Type.ATTRIBUTE, node.getNodeName());

                case Type.TEXT:
                    return NodeKindTest.TEXT;

                case Type.COMMENT:
                    return NodeKindTest.COMMENT;

                case Type.PROCESSING_INSTRUCTION:
                     return NodeKindTest.PROCESSING_INSTRUCTION;

                case Type.NAMESPACE:
                    return NodeKindTest.NAMESPACE;

                default:
                    throw new IllegalArgumentException("Unknown node kind " + node.getNodeKind());
            }
        // context item may be a JSObjectValue for non-DOM event handlers
        } else if (item instanceof JSObjectValue){
        	return new JSObjectType();
        	
        } else {
            // it must be an atomic value, though we don't use this option
            return ((AtomicValue)item).getItemType();
        }
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        return (item ==null ? 0 : 1);
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     */

    public Item itemAt(int n) {
        if (n==0 && item !=null) {
            return item;
        } else {
            return null;
        }
    }


    /**
     * Get a subsequence of the value
     *
     * @param start  the index of the first item to be included in the result, counting from zero.
     *               A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     *               sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     *               get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     *               is returned. If the value goes off the end of the sequence, the result returns items up to the end
     *               of the sequence
     * @return the required subsequence. If min is
     */

    public GroundedValue subsequence(int start, int length) {
        if (item != null && start <= 0 && start+length > 0) {
            return this;
        } else {
            return EmptySequence.getInstance();
        }
    }

    /**
    * Get the node that forms the node-set. Return null if there is none.
    */

    public Item getItem() {
        return item;
    }

    /**
    * Return an enumeration of this nodeset value.
    */

    public SequenceIterator iterate() {
        return SingletonIterator.makeIterator(item);
    }

    /**
     * Get the effective boolean value
     */

    public boolean effectiveBooleanValue() {
        return (item != null);
    }

    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. For QNames and NOTATIONS, or lists
     * containing them, it fails.
     */

    public String getStringValue() {
        return (item ==null ? "" : item.getStringValue());
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

