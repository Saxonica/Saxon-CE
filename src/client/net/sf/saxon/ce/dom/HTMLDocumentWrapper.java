package client.net.sf.saxon.ce.dom;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.Type;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;

import java.util.HashMap;

/**
 * The document node of a tree implemented as a wrapper around an XML DOM Document.
 */

public class HTMLDocumentWrapper extends HTMLNodeWrapper implements DocumentInfo {

    protected Configuration config;
    protected String baseURI;
    protected int documentNumber;
    protected boolean domLevel3;
    private HashMap<String, Object> userData;
    private HashMap<String, HTMLNodeWrapper> idIndex;
    private boolean isHttpRequested;

    /**
     * Wrap a DOM Document or DocumentFragment node
     * @param doc a DOM Document or DocumentFragment node
     * @param baseURI the base URI of the document
     * @param config the Saxon configuration
     */
    public HTMLDocumentWrapper(Node doc, String baseURI, Configuration config) {
    	this(doc, baseURI, config, DocType.UNKNOWN);
    }
    
    public final native String getBaseURI(Document doc) /*-{
    	if (doc.documentURI) {
    		return doc.doumentURI;
    	} else {
    		return null;
    	}
    }-*/;

    public HTMLDocumentWrapper(Node doc, String baseURI, Configuration config, DocType newDocType) {
        super(doc, null, 0);
//        if (doc.getNodeType() != Node.DOCUMENT_NODE) {
//            throw new IllegalArgumentException("Node must be a DOM Document");
//        }

        nodeKind = Type.DOCUMENT;
                
        if ((baseURI == null || baseURI == "") && doc.getNodeType() == Type.DOCUMENT) {
        	baseURI = ((Document)doc).getURL();
        	this.baseURI = (baseURI != null && baseURI != "")? baseURI : getBaseURI((Document)doc);
        } else {
        	this.baseURI = baseURI;
        }
        
        // affects selectID() behaviour:
    	isHttpRequested = (newDocType == DocType.NONHTML);
    	
        docWrapper = this;
        domLevel3 = true;
        setConfiguration(config);
        // crude test for XHTML by seeing if there's an identifying xmlns attribute on any
        // top-level html element
        if (newDocType != DocType.UNKNOWN) {
        	this.htmlType = newDocType;
        	return;
        }
        // need to determine whether HTML, XHTML or neither so as to control case of node names
        // and distinguish other XML/HTML behaviours - like SelectID
        try {
            UnfailingIterator iter = this.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
            while (true) {
                NodeInfo n = (NodeInfo)iter.next();
                if (n == null) {
                	break;
                } else  {
                	String rawLocal = ((HTMLNodeWrapper)n).getRawLocalName().toLowerCase();
                	if (rawLocal.equals("html")) {
                		NamespaceBinding[] nb = n.getDeclaredNamespaces(null);
                		htmlType = DocType.HTML;
                		for (NamespaceBinding nBinding: nb) {
                			if (nBinding.getURI().equals(NamespaceConstant.XHTML)) {
                				htmlType = DocType.XHTML;
                				break;
                			}
                		}
                	} else {
                		htmlType = DocType.NONHTML;
                	}
                	break; // only check first element
                }
            }
        } catch(Exception e) {}
    }
         
    private DocType htmlType = DocType.UNKNOWN;
    
    public enum DocType {
    	XHTML, HTML, UNKNOWN, NONHTML
    }
    
    private boolean getIsXMLObjectTypeFromURIext() {
    	int pos = baseURI.indexOf('?');
    	String testUri = (pos < 0)?  baseURI : baseURI.substring(pos);
    	return testUri.toLowerCase().endsWith("xhtml");
    }
    
    /**
     * @return type of document determined by tag name
     */
    public DocType getDocType() {
    	return htmlType;
    }
    
    /**
     * @return type of document determined by object type
     */
    public boolean isXMLDocumentObject() {
    	return isHttpRequested;
    }
    
    // does not seem to be a prescriptive way of distinguishing object types
    private boolean isNodeXMLDocument() {
    	String nodeString = node.toString();
    	if (nodeString.endsWith("XMLDocument]")) {
    		return true;
    	} else if(nodeString.endsWith("HTMLDocument]")){
    		return false;
    	} else {
    		return !supportsGetElementById((Document)node);
    	}
    }
    
    public static native boolean supportsGetElementById(Document doc) /*-{
	   return !!(doc.getElementById);
    }-*/;

    /**
     * Create a wrapper for a node in this document
     *
     * @param node the DOM node to be wrapped. This must be a node within the document wrapped by this
     *             XMLDocumentWrapper
     * @throws IllegalArgumentException if the node is not a descendant of the Document node wrapped by
     *                                  this XMLDocumentWrapper
     */
    public HTMLNodeWrapper wrap(Node node) {
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
        documentNumber = config.allocateDocumentNumber();
    }

    /**
     * Get the configuration previously set using setConfiguration
     */

    public Configuration getConfiguration() {
        return config;
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
    	Node el;
    	// IE does not support getElementById for XML documents
    	// but it does support it for XHTML if its the host page
    	Document doc = ((Document)node);

    	if (!isHttpRequested) {
	        el = (doc).getElementById(id);
	        if (el == null) {
	            return null;
	        }
	        return wrap(el);
    	} else {
            if (idIndex != null) {
                return idIndex.get(id);
            } else {
                idIndex = new HashMap();
                UnfailingIterator iter = iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
                boolean useNS = isNSok(node);
                while (true) {
                    NodeInfo node = (NodeInfo)iter.next();
                    if (node == null) {
                        break;
                    }
                    Node testNode = ((HTMLNodeWrapper)node).getUnderlyingNode();
                    String xmlId = (useNS)? getXmlIdNS(testNode) : getXmlId(testNode);
                    //String xmlId = ((Element)((HTMLNodeWrapper)node).getUnderlyingNode()).getAttribute("xml:id");
                    if (xmlId != null && !xmlId.isEmpty()) {
                        idIndex.put(xmlId, (HTMLNodeWrapper)node);
                    }
                }
                return idIndex.get(id);
            }
    	}
        
    }
    
    public static native String getXmlIdNS(Node inNode) /*-{
		   return inNode.getAttributeNS('http://www.w3.org/XML/1998/namespace', 'id');   		   
    }-*/;
    
    public static native String getXmlId(Node inNode) /*-{
	   return inNode.getAttribute("xml:id");	   
    }-*/;
    
    private static native boolean isNSok(Node inNode) /*-{
    	return !!(inNode.getAttributeNS);
    }-*/;
    
    
    
    /**
     * Determine whether this is the same node as another node. <br />
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        return other instanceof HTMLDocumentWrapper && node == ((HTMLDocumentWrapper)other).node;
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

    /**
     * Create a DocumentFragment node. Method not available from GWT
     */

    public static native Node createDocumentFragment(Document doc) /*-{
        return doc.createDocumentFragment();
    }-*/;
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.