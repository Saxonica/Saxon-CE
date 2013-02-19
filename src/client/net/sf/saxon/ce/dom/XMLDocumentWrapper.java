package client.net.sf.saxon.ce.dom;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.type.Type;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;

import java.util.HashMap;

/**
 * The document node of a tree implemented as a wrapper around an XML DOM Document.
 */

public class XMLDocumentWrapper extends XMLNodeWrapper implements DocumentInfo {

    protected Configuration config;
    protected String baseURI;
    protected int documentNumber;
    protected boolean domLevel3;
    private HashMap<String, Object> userData;
    private HashMap<String, XMLNodeWrapper> idIndex;

    /**
     * Wrap a DOM Document or DocumentFragment node
     * @param doc a DOM Document or DocumentFragment node
     * @param baseURI the base URI of the document
     * @param config the Saxon configuration
     */

    public XMLDocumentWrapper(com.google.gwt.xml.client.Node doc, String baseURI, Configuration config) {
        super(doc, null, 0);
        if (doc.getNodeType() != Node.DOCUMENT_NODE && doc.getNodeType() != Node.DOCUMENT_FRAGMENT_NODE) {
            throw new IllegalArgumentException("Node must be a DOM Document or DocumentFragment");
        }
        node = doc;
        nodeKind = Type.DOCUMENT;
        this.baseURI = baseURI;
        docWrapper = this;
        domLevel3 = true;
        setConfiguration(config);
    }

    /**
     * Create a wrapper for a node in this document
     *
     * @param node the DOM node to be wrapped. This must be a node within the document wrapped by this
     *             XMLDocumentWrapper
     * @throws IllegalArgumentException if the node is not a descendant of the Document node wrapped by
     *                                  this XMLDocumentWrapper
     */

    public XMLNodeWrapper wrap(Node node) {
        if (node == this.node) {
            return this;
        }
        Document doc = node.getOwnerDocument();
        if (doc == this.node) {
            return makeWrapper(node, this);
        } else {
            throw new IllegalArgumentException(
                "XMLDocumentWrapper#wrap: supplied node does not belong to the wrapped DOM document");
        }
    }

    /**
     * Set the Configuration that contains this document
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
        documentNumber = config.getDocumentNumberAllocator().allocateDocumentNumber();
    }

    /**
     * Get the configuration previously set using setConfiguration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the name pool used for the names in this document
     */

    public NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
     * Get the unique document number
     */

    public int getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Get the element with a given ID, if any
     *
     * @param id the required ID value
     * @return a NodeInfo representing the element with the given ID, or null if there
     *         is no such element. This relies on the getElementById() method in the
     *         underlying DOM.
     */

    public NodeInfo selectID(String id) {
        // Calling getElementById() seems to achieve nothing useful, as in general the parser does not read the DTD.
        // So we content ourselves with finding xml:id attributes
        if (idIndex != null) {
            return idIndex.get(id);
        } else {
            idIndex = new HashMap();
            AxisIterator iter = iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
            while (true) {
                NodeInfo node = (NodeInfo)iter.next();
                if (node == null) {
                    break;
                }
                String xmlId = ((Element)((XMLNodeWrapper)node).getUnderlyingNode()).getAttribute("xml:id");
                if (xmlId != null && !xmlId.isEmpty()) {
                    idIndex.put(xmlId, (XMLNodeWrapper)node);
                }
            }
            return idIndex.get(id);
        }
    }

    /**
     * Determine whether this is the same node as another node. <br />
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        return other instanceof XMLDocumentWrapper && node == ((XMLDocumentWrapper)other).node;
    }

    /**
     * Get the type annotation. Always XS_UNTYPED.
     */

    public int getTypeAnnotation() {
        return StandardNames.XS_UNTYPED;
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