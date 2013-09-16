package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.ArrayIterator;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.tree.util.Orphan;
import client.net.sf.saxon.ce.type.Type;

import java.util.HashMap;

/**
* This class represents a temporary tree whose root document node owns a single text node. <BR>
*/

public final class TextFragmentValue extends Orphan implements DocumentInfo {

    private TextFragmentTextNode textNode = null;   // created on demand
    private Configuration config;
    private int documentNumber;
    private HashMap<String, Object> userData;

    /**
    * Constructor: create a result tree fragment containing a single text node
    * @param value a String containing the value
    * @param baseURI the base URI of the document node
    */

    public TextFragmentValue(CharSequence value, String baseURI) {
        setNodeKind(Type.DOCUMENT);
        setNodeName(null);
        setStringValue(value);
        setSystemId(baseURI);
    }

	/**
	* Set the configuration (containing the name pool used for all names in this document)
	*/

	public void setConfiguration(Configuration config) {
        this.config = config;
		documentNumber = -1;    // the document number is allocated lazily because it can cause
                                // contention on the NamePool and is often not needed.
	}

    /**
	* Get the unique document number
	*/

	public int getDocumentNumber() {
        if (documentNumber == -1) {
            documentNumber = config.allocateDocumentNumber();
            // technically this isn't thread-safe; however, TextFragmentValues are invariably used within
            // a single thread
        }
	    return documentNumber;
	}

    /**
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        return this==other;
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
        if (this==other) return 0;
        return -1;
    }

    /**
    * Determine whether the node has any children.
    * @return <code>true</code> if this node has any attributes,
    *   <code>false</code> otherwise.
    */

    public boolean hasChildNodes() {
        return !("".equals(getStringValue()));
    }

    /**
    * Return an enumeration over the nodes reached by the given axis from this node
    * @param axisNumber The axis to be iterated over
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return a AxisIterator that scans the nodes reached by the axis in turn.
    * @see client.net.sf.saxon.ce.om.Axis
    */

    public UnfailingIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        switch (axisNumber) {
            case Axis.ANCESTOR:
            case Axis.ATTRIBUTE:
            case Axis.FOLLOWING:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PARENT:
            case Axis.PRECEDING:
            case Axis.PRECEDING_SIBLING:
                return EmptyIterator.getInstance();

            case Axis.SELF:
            case Axis.ANCESTOR_OR_SELF:
                return Navigator.filteredSingleton(this, nodeTest);

            case Axis.CHILD:
            case Axis.DESCENDANT:
                return Navigator.filteredSingleton(getTextNode(), nodeTest);

            case Axis.DESCENDANT_OR_SELF:
                boolean b1 = nodeTest.matches(this);
                NodeInfo textNode2 = getTextNode();
                boolean b2 = nodeTest.matches(textNode2);
                if (b1) {
                    if (b2) {
                        NodeInfo[] pair = {this, textNode2};
                        return new ArrayIterator(pair);
                    } else {
                        return SingletonIterator.makeIterator(this);
                    }
                } else {
                    if (b2) {
                        return SingletonIterator.makeIterator(textNode2);
                    } else {
                        return EmptyIterator.getInstance();
                    }
                }

            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document
    */

    public DocumentInfo getDocumentRoot() {
        return this;
    }

    /**
    * Copy the result tree fragment value to a given Outputter
    */

    public void copy(Receiver out, int copyOptions)
    throws XPathException {
        out.characters(getStringValue());
    }

    /**
    * Get the element with a given ID.
    * @param id The unique ID of the required element
    * @return null (this kind of tree contains no elements)
    */

    public NodeInfo selectID(String id) {
        return null;
    }


    /**
    * Make an instance of the text node
    */

    private TextFragmentTextNode getTextNode() {
        if (textNode==null) {
            textNode = new TextFragmentTextNode(getStringValue(), getSystemId());
        }
        return textNode;
    }

    /**
    * Inner class representing the text node; this is created on demand
    */

    private class TextFragmentTextNode extends Orphan implements NodeInfo {

        public TextFragmentTextNode(CharSequence value, String systemId) {
            setStringValue(value);
            setSystemId(systemId);
            setNodeKind(Type.TEXT);
            setNodeName(null);
        }

        /**
        * Get a character string that uniquely identifies this node
         */

        public void generateId(FastStringBuffer buffer) {
            getParent().generateId(buffer);
            buffer.append("t1");
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
            if (this==other) return 0;
            return +1;
        }

        /**
         * Get the document number of the document containing this node. For a free-standing
         * orphan node, just return the hashcode.
         */

        public int getDocumentNumber() {
            return getDocumentRoot().getDocumentNumber();
        }

        /**
        * Return an enumeration over the nodes reached by the given axis from this node
        * @param axisNumber the axis to be iterated over
        * @param nodeTest A pattern to be matched by the returned nodes
        * @return a AxisIterator that scans the nodes reached by the axis in turn.
        */

        public UnfailingIterator iterateAxis( byte axisNumber, NodeTest nodeTest) {
            switch (axisNumber) {
                case Axis.ANCESTOR:
                case Axis.PARENT:
                    return Navigator.filteredSingleton(TextFragmentValue.this, nodeTest);

                case Axis.ANCESTOR_OR_SELF:
                    boolean matchesDoc = nodeTest.matches(TextFragmentValue.this);
                    boolean matchesText = nodeTest.matches(this);
                    if (matchesDoc && matchesText) {
                        NodeInfo[] nodes = {this, TextFragmentValue.this};
                        return new ArrayIterator(nodes);
                    } else if (matchesDoc && !matchesText) {
                        return SingletonIterator.makeIterator(TextFragmentValue.this);
                    } else if (matchesText && !matchesDoc) {
                        return SingletonIterator.makeIterator(this);
                    } else {
                        return EmptyIterator.getInstance();
                    }

                case Axis.ATTRIBUTE:
                case Axis.CHILD:
                case Axis.DESCENDANT:
                case Axis.FOLLOWING:
                case Axis.FOLLOWING_SIBLING:
                case Axis.NAMESPACE:
                case Axis.PRECEDING:
                case Axis.PRECEDING_SIBLING:
                    return EmptyIterator.getInstance();

                case Axis.SELF:
                case Axis.DESCENDANT_OR_SELF:
                    return Navigator.filteredSingleton(this, nodeTest);

                default:
                     throw new IllegalArgumentException("Unknown axis number " + axisNumber);
            }
        }

        /**
         * Find the parent node of this node.
         * @return The Node object describing the containing element or root node.
         */

        public NodeInfo getParent() {
            return TextFragmentValue.this;
        }

        /**
        * Get the root node
        * @return the NodeInfo representing the root of this tree
        */

        public NodeInfo getRoot() {
            return TextFragmentValue.this;
        }

        /**
        * Get the root (document) node
        * @return the DocumentInfo representing the containing document
        */

        public DocumentInfo getDocumentRoot() {
            return TextFragmentValue.this;
        }

        /**
        * Copy the node to a given Outputter
        */

        public void copy(Receiver out, int copyOptions)
        throws XPathException {
            out.characters(getStringValue());
        }


    }

    /**
     * Set user data on the document node. The user data can be retrieved subsequently
     * using {@link #getUserData}
     * @param key   A string giving the name of the property to be set. Clients are responsible
     *              for choosing a key that is likely to be unique. Must not be null.
     * @param value The value to be set for the property. May be null, which effectively
     *              removes the existing value for the property.
     */

    public void setUserData(String key, Object value) {
        if (userData == null) {
            userData = new HashMap(4);
        }
        if (value == null) {
            userData.remove(key);
        } else {
            userData.put(key, value);
        }
    }

    /**
     * Get user data held in the document node. This retrieves properties previously set using
     * {@link #setUserData}
     * @param key A string giving the name of the property to be retrieved.
     * @return the value of the property, or null if the property has not been defined.
     */

    public Object getUserData(String key) {
        if (userData == null) {
            return null;
        } else {
            return userData.get(key);
        }
    }    

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

