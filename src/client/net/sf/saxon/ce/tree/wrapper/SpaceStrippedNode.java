package client.net.sf.saxon.ce.tree.wrapper;

import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.event.Stripper;
import client.net.sf.saxon.ce.expr.ItemMappingFunction;
import client.net.sf.saxon.ce.expr.UnfailingItemMappingIterator;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.ListIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.UntypedAtomicValue;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.ArrayList;
import java.util.List;


/**
 * A StrippedNode is a view of a node, in a virtual tree that has whitespace
 * text nodes stripped from it. All operations on the node produce the same result
 * as operations on the real underlying node, except that iterations over the axes
 * take care to skip whitespace-only text nodes that are supposed to be stripped.
 * Note that this class is only used in cases where a pre-built tree is supplied as
 * the input to a transformation, and where the stylesheet does whitespace stripping;
 * if a SAXSource or StreamSource is supplied, whitespace is stripped as the tree
 * is built.
*/

public class SpaceStrippedNode extends AbstractVirtualNode {

    protected SpaceStrippedNode() {}

    /**
     * This constructor is protected: nodes should be created using the makeWrapper
     * factory method
     * @param node    The node to be wrapped
     * @param parent  The StrippedNode that wraps the parent of this node
     */

    protected SpaceStrippedNode(NodeInfo node, SpaceStrippedNode parent) {
        this.node = node;
        this.parent = parent;
    }

    /**
     * Factory method to wrap a node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The underlying node
     * @param docWrapper  The wrapper for the document node (must be supplied)
     * @param parent      The wrapper for the parent of the node (null if unknown)
     * @return            The new wrapper for the supplied node
     */

    protected static SpaceStrippedNode makeWrapper(NodeInfo node,
                                       SpaceStrippedDocument docWrapper,
                                       SpaceStrippedNode parent) {
        SpaceStrippedNode wrapper = new SpaceStrippedNode(node, parent);
        wrapper.docWrapper = docWrapper;
        return wrapper;
    }

    /**
     * Factory method to wrap a node within the same document as this node with a VirtualNode
     * @param node        The underlying node
     * @param parent      The wrapper for the parent of the node (null if unknown)
     * @return            The new wrapper for the supplied node
     */

    public SpaceStrippedNode makeWrapper(NodeInfo node, SpaceStrippedNode parent) {
        SpaceStrippedNode wrapper = new SpaceStrippedNode(node, parent);
        wrapper.docWrapper = this.docWrapper;
        return wrapper;
    }

    /**
     * Get the typed value. The result of this method will always be consistent with the method
     * {@link client.net.sf.saxon.ce.om.Item#getTypedValue()}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @return the typed value. If requireSingleton is set to true, the result will always be an
     *         AtomicValue. In other cases it may be a Value representing a sequence whose items are atomic
     *         values.
     * @since 8.5
     */

    public AtomicValue getTypedValue() {
        return new UntypedAtomicValue(getStringValue());
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (other instanceof SpaceStrippedNode) {
            return node.isSameNodeInfo(((SpaceStrippedNode)other).node);
        } else {
            return node.isSameNodeInfo(other);
        }
    }


    /**
    * Determine the relative position of this node and another node, in document order.
    * The other node will always be in the same document.
    * @param other The other node, whose position is to be compared with this node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
    */

    public int compareOrder(NodeInfo other) {
        if (other instanceof SpaceStrippedNode) {
            return node.compareOrder(((SpaceStrippedNode)other).node);
        } else {
            return node.compareOrder(other);
        }
    }

    /**
    * Get the NodeInfo object representing the parent of this node
    */

    public NodeInfo getParent() {
        if (parent==null) {
            NodeInfo realParent = node.getParent();
            if (realParent != null) {
                parent = makeWrapper(realParent, (SpaceStrippedDocument)docWrapper, null);
            }
        }
        return parent;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public UnfailingIterator iterateAxis(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ATTRIBUTE:
            case Axis.NAMESPACE:
                UnfailingIterator iter = iterateAxis(axisNumber);
                List<NodeInfo> wrappedNodes = new ArrayList<NodeInfo>();
                while (true) {
                    NodeInfo att = (NodeInfo)iter.next();
                    if (node == null) {
                        break;
                    }
                    SpaceStrippedNode wrapper = new SpaceStrippedNode(att, this);
                    wrapper.docWrapper = this.docWrapper;
                    wrappedNodes.add(wrapper);
                }
                return new ListIterator(wrappedNodes);

            case Axis.CHILD:
                return makeStrippingIterator(node.iterateAxis(axisNumber), this);
            case Axis.FOLLOWING_SIBLING:
            case Axis.PRECEDING_SIBLING:
                SpaceStrippedNode parent = (SpaceStrippedNode)getParent();
                if (parent == null) {
                    return EmptyIterator.getInstance();
                } else {
                    return makeStrippingIterator(node.iterateAxis(axisNumber), parent);
                }
            default:
                return makeStrippingIterator(node.iterateAxis(axisNumber), null);
        }
    }

    /**
    * Copy this node to a given outputter (deep copy)
    */

    public void copy(Receiver out, int copyOptions) throws XPathException {
        // The underlying code does not do whitespace stripping. So we need to interpose
        // a stripper.
        Stripper stripper = ((SpaceStrippedDocument)docWrapper).getStripper().getAnother();
        stripper.setUnderlyingReceiver(out);
        node.copy(stripper, copyOptions);
    }

    private UnfailingIterator makeStrippingIterator(UnfailingIterator base, SpaceStrippedNode parent) {
        return new UnfailingItemMappingIterator(base, new StrippingMappingFunction((SpaceStrippedDocument)docWrapper, parent));
    }

    private static class StrippingMappingFunction implements ItemMappingFunction {

        private SpaceStrippedDocument docWrapper;
        private SpaceStrippedNode parent;

        public StrippingMappingFunction(SpaceStrippedDocument docWrapper, SpaceStrippedNode parent) {
            this.docWrapper = docWrapper;
            this.parent = parent;
        }

        public Item mapItem(Item item) {
            if (isPreserved((NodeInfo)item)) {
                return makeWrapper((NodeInfo)item, docWrapper, parent);
            } else {
                return null;
            }
        }

        private boolean isPreserved(NodeInfo realNode) {
            if (realNode.getNodeKind() != Type.TEXT) {
                return true;
            }
            if (!Whitespace.isWhite(realNode.getStringValue())) {
                return true;
            }
            NodeInfo actualParent =
                    (parent==null ? realNode.getParent() : parent.node);

            if (docWrapper.containsPreserveSpace()) {
                NodeInfo p = actualParent;
                // the document contains one or more xml:space="preserve" attributes, so we need to see
                // if one of them is on an ancestor of this node
                while (p.getNodeKind() == Type.ELEMENT) {
                    String val = Navigator.getAttributeValue(p, NamespaceConstant.XML, "space");
                    if (val != null) {
                        if ("preserve".equals(val)) {
                            return true;
                        } else if ("default".equals(val)) {
                            break;
                        }
                    }
                    p = p.getParent();
                }
            }

            try {
                StructuredQName parentName = new StructuredQName("", actualParent.getURI(), actualParent.getLocalPart());
                byte preserve = docWrapper.getStripper().isSpacePreserving(parentName);
                return preserve == Stripper.ALWAYS_PRESERVE;
            } catch (XPathException e) {
                // Ambiguity between strip-space and preserve-space. Because we're in an axis iterator,
                // we don't get an opportunity to fail, so take the recovery action.
                return true;
            }
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.