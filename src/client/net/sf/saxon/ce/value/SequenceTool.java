package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.js.JSObjectType;
import client.net.sf.saxon.ce.js.JSObjectValue;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;

/**
* Utility class providing methods that operate on arbitrary sequences
*/

public abstract class SequenceTool {

    private SequenceTool() {}

    /**
     * Static method to make an Item from a Value
     * @param value the value to be converted
     * @return null if the value is an empty sequence; or the only item in the value
     * if it is a singleton sequence
     * @throws XPathException if the Value contains multiple items
     */

    public static Item asItem(Sequence value) throws XPathException {
        if (value instanceof Item) {
            return (Item)value;
        } else {
            SequenceIterator iter = value.iterate();
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
     * Determine the data type of the items in the sequence
     * @return for the default implementation: AnyItemType (not known)
     */

    public static ItemType getItemTypeOfValue(Sequence val) {
        UnfailingIterator iter = val.iterate();
        ItemType type;
        Item item = iter.next();
        if (item == null) {
            return EmptySequenceTest.getInstance();
        } else {
            type = getItemType(item);
        }
        while (true) {
            if (type == AnyItemType.getInstance()) {
                return type;
            }
            item = iter.next();
            if (item == null) {
                break;
            }
            type = Type.getCommonSuperType(type, getItemType(item));
        }
        return type;
    }

    public static ItemType getItemType(Item item) {
        if (item instanceof NodeInfo) {
            NodeInfo node = ((NodeInfo)item);
            switch (node.getNodeKind()) {
                case Type.DOCUMENT:
                    // Need to know whether the document is well-formed and if so what the element type is
                    UnfailingIterator iter = node.iterateAxis(Axis.CHILD, AnyNodeTest.getInstance());
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
                            elementType = getItemType(n);
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


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
