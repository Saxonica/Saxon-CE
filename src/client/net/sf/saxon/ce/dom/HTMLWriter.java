package client.net.sf.saxon.ce.dom;

import java.util.logging.Logger;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.Controller.APIcommand;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Text;


/**
  * DOMWriter is a Receiver that attaches the result tree to a specified Node in the HTML DOM Document
  */

public class HTMLWriter implements Receiver {

    private PipelineConfiguration pipe;
    private NamePool namePool;
    private Node currentNode;
    private Document document;
    private Node nextSibling;
    private int level = 0;
    private String systemId;
    private Node containerNode;
    private static Logger logger = Logger.getLogger("XSLT20Processor");

    /**
     * Native Javascript method to create a namespaced element. Not available in GWT because
     * it's not supported in IE. But needed for SVG/mathML support
     * @param ns the namespace URI
     * @param name the local name
     * @return the constructed element or null if method not available
     */

    private native Element createElementNS(Document doc,
                                           final String ns,
                                           final String name) /*-{
        if (doc.createElementNS) {
        	return doc.createElementNS(ns, name);
        }
        return null;
    }-*/;
    
    private native Node createProcessingInstruction(Document doc,
            final String target,
            final String data) /*-{
       return doc.createProcessingInstruction(target, data);
    }-*/;
      
    private native Node createComment(Document doc,
            final String data) /*-{
       	return doc.createComment(data);
   }-*/;
    
    private static native boolean attNSSupported(Document doc) /*-{
       	return (typeof doc.createNode == "function" || typeof doc.createAttributeNS == "function");
   }-*/;
    
    public static void setAttribute(Document doc,
    		Element element,
    		String name,
    		String URI,
    		String value,
    		WriteMode wMode) {
    	// fix for IE issue with colspan etc #1570

        name = tableAttributeFix(name, wMode);

    	if (attNSSupported(doc)) {
    		setAttributeJs(doc,element,name,URI,value);
    	} else {
        	String prefix = name.substring(0, name.indexOf(":"));
        	String x = "xmlns";
        	String nsDeclaraction = (prefix.length() == 0)? x : x + ":" + prefix;
    		if (!element.hasAttribute(nsDeclaraction)) {
    			addNamespace(element, prefix, URI);
    		}
        	element.setAttribute(name, value);
            setAttributeProps(element, name, value);
    	}
    }
    
    public static String tableAttributeFix(String name, WriteMode wMode){
    	if (wMode != WriteMode.XML && Configuration.getIeVersion() > 0 && name.length() > 5){
		if (name.equals("rowspan")){
			name = "rowSpan";
		} else if (name.equals("colspan")){
			name = "colSpan";
		} else if (name.equals("cellpadding")){
			name = "cellPadding";
		} else if (name.equals("cellppacing")){
			name = "cellSpacing";
		}
    	}
		return name;
    }
     
    
    //  This throws an exception in IE7 that must be handled, in IE, you can only create a namespace-qualified
    //  attribute using the createNode method of the DOMDocument.
    /**
     *  Creates an attribute with a namespace.
     *  This throws an exception in IE7 that (it seems) must be handled by the caller,
     *  in IE, you can only create a namespace-qualified attribute using the createNode method
     *  of the DOMDocument.
     * 
     */
    public static native void setAttributeJs(Document doc,
    		final Node element,
    		final String name,
    		final String URI,
            final String value) /*-{
        var att;
        if (doc.createNode) {
            att = doc.createNode(2, name, URI);
            att.value = value;
        } else {
            att = doc.createAttributeNS(URI, name);
            att.nodeValue = value;
        } 
        element.setAttributeNode(att);      	
   }-*/;
    

    /**
    * Set the pipelineConfiguration
    */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        namePool = pipe.getConfiguration().getNamePool();
    }

    /**
    * Get the pipeline configuration used for this document
    */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Set the System ID of the destination tree
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the system identifier that was set with setSystemId.
     *
     * @return The system identifier that was set with setSystemId,
     *         or null if setSystemId was not called.
     */
    public String getSystemId() {
        return systemId;
    }

    /**
    * Start of the document.
    */

    public void open () {}

    /**
    * End of the document.
    */

    public void close () {}

    /**
     * Start of a document node.
    */

    public void startDocument() throws XPathException {
    	// not required - setNode is called instead:
    	//document = XMLDOM.createDocument();
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {}

    /**
    * Start of an element.
    */

    public void startElement(int nameCode, int properties) throws XPathException {
        String localName = namePool.getLocalName(nameCode);
        String prefix = namePool.getPrefix(nameCode);
        String uri = namePool.getURI(nameCode);
        
        // TODO: For XML Writer it should write prefixes in a
        // way compliant with the XSLT2.0 specification - using xsl:output attributes
        Element element = null;
        if (uri != null && !uri.isEmpty()) {
        	// no svg specific prefix now used, for compliance with HTML5
            element = createElementNS(document, uri, localName);
        }
        
        // if there's no namespace - or no namespace support
        if (element == null) {
            element = document.createElement(localName);
        }
        // special case for html element: write to the document node
        Controller controller = pipe.getController();
        if (controller != null && controller.getApiCommand() == APIcommand.UPDATE_HTML
            && (localName.equals("html") || localName.equals("head") || localName.equals("body"))) {
        	if (localName.equals("html")){
        		element = (Element)document.getFirstChild();
        	} else {
        		element = (Element)document.getElementsByTagName(localName.toUpperCase()).getItem(0);
        		NodeList<Node> nodes = element.getChildNodes();
        		for (int n = 0; n < nodes.getLength(); n++) {
        			Node node = nodes.getItem(n);
        			node.removeFromParent();
        		}
        	}
        	currentNode = element;
        	level++;
        	return;
        }
        if (nextSibling != null && level == 0) {
            currentNode.insertBefore(element, nextSibling);
        } else  {
        	try {
        		currentNode.appendChild(element);
        	} catch(JavaScriptException err) {
        		if(uri.equals(NamespaceConstant.IXSL)) {
	        				XPathException xpe = new XPathException("Error on adding IXSL element to the DOM, the IXSL namespace should be added to the 'extension-element-prefixes' list.");
	        				throw(xpe);
        		} else {       			
        			throw(new XPathException(err.getMessage()));
        		}
        	} catch(Exception exc) {
        		XPathException xpe = new XPathException("Error on startElement in HTMLWriter for element '" + localName + "': " + exc.getMessage());
        		throw(xpe);
        	}
        }
        currentNode = element;
        level++;
    }

    private static void addNamespace(Element element, String prefix, String uri) {
        String attName = (prefix.isEmpty() ? "xmlns" : "xmlns:" + prefix);
        element.setAttribute(attName, uri);
    }

    public void namespace (NamespaceBinding nsBinding, int properties) throws XPathException {
        //try {
//        	String prefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
//    		String uri = namePool.getURIFromNamespaceCode(namespaceCode);
//    		Element element = (Element)currentNode;
//            if (!(uri.equals(NamespaceConstant.XML))) {
//                addNamespace(element, prefix, uri);
//            }
//        } catch (DOMException err) {
//            throw new XPathException(err);
//        }
    }

    public void attribute(int nameCode, CharSequence value)
    throws XPathException {
        String localName = namePool.getLocalName(nameCode);
        String uri = namePool.getURI(nameCode);
        String val = value.toString();
        Element element = (Element)currentNode;

        // must be HTML write mode
        if (mode != WriteMode.XML && NamespaceConstant.HTML_PROP.equals(uri)) {
            element.setPropertyString(localName, val);
        } else if (mode != WriteMode.XML && NamespaceConstant.HTML_STYLE_PROP.equals(uri)) {
        	// if localName starts with '_-' then remove the underscore e.g _-webkit-transition
        	if(localName.length() > 1 && localName.charAt(0) == '_' && localName.charAt(1) == '-') {
        		localName = localName.substring(1);
        	}
        	localName = HTMLWriter.getCamelCaseName(localName);
            element.getStyle().setProperty(localName, val);
        } else if (uri != null && !uri.isEmpty()){
            String fullname = namePool.getDisplayName(nameCode);
            setAttribute(document, element, fullname, uri, val, mode);
        } else {
        	localName = tableAttributeFix(localName, mode);
            element.setAttribute(localName, val);
            setAttributeProps(element, localName, val);
        }
    }
    
    /**
     * Method for backward compatibility with IE8 and previous where
     * properties and attributes were handled separately
     */
    public static void setAttributePropsOriginal(Element element, String localName, String val){
    	if (Configuration.getIeVersion() > 0 && Configuration.getIeVersion() < 9) {
	        if (localName.length() == 5) {
	            if (localName.equals("style")) {
	                // In IE, setting the style attribute dynamically has no effect on the individual style properties,
	                // and does not affect the rendition of the element. So we parse out the content of the attribute,
	                // and use it to set the individual properties.
	            	if (hasStyle(element)) {
	            		setStyleProperties(element, val);
	            	}
	            } else if (localName.equals("class")) {
	            	setClass(element, val);
	            } else if (localName.equals("title")) {
	            	setTitle(element, val);
//	            } else if (localName.equals("align") || localName.equals("width")) {
//	            	setElementProperty(element, localName, val);
		        } else {	        	
		        	setElementProperty(element, localName, val);
		        }
	        } else if (localName.length() == 2 && localName.equals("id")) {
	        	setId(element, val);
//	        } else if (localName.length() == 7 && localName.equals("colSpan") || localName.equals("rowSpan") ) {
//	        	setElementProperty(element, localName, val);
	        } else {	        	
	        	setElementProperty(element, localName, val);
	        }
    	}
    }
    
    /**
     * following setElementProperty method call doesn't work consistently
     * because in IE some element are initially undefined?
     */
    public static void setAttributeProps(Element element, String localName, String val){
    	if (Configuration.getIeVersion() > 0 && Configuration.getIeVersion() < 9) {
	            if (localName.equals("style")) {
	            	if (hasStyle(element)) {
	            		setStyleProperties(element, val);
	            	}
	            }
	            else {
	            	localName = (localName == "class")? "className" : localName;
	            	try {
	            		setElementProperty(element, localName, val);
	            	} catch(Exception e) {
	            		// some IE8 properties exist but appear to be read-only
	            		logger.warning("Unable to set '" + localName + "' property for element.");
	            	}
	            }
    	}
    }
    
    private static native boolean hasStyle(Element element) /*-{
       	return (typeof element.style !== "undefined");
    }-*/;
    
    private static native boolean setClass(Element element, String value) /*-{
   	 	if (typeof element.className !== "undefined") {
   	 		element.className = value;
   	 	}
    }-*/;
    
    private static native boolean setId(Element element, String value) /*-{
	 	if (typeof element.id !== "undefined") {
	 		element.id = value;
	 	}
    }-*/;
    
    private static native boolean setTitle(Element element, String value) /*-{
	 	if (typeof element.title !== "undefined") {
	 		element.title = value;
	 	}
    }-*/;
    
    private static native void setElementProperty(Element element, String name, String value) /*-{
    	if (typeof element[name] !== "undefined") {
			element[name] = value;
    	}
    }-*/;
    
    /**
     * Parse the value of the style attribute and use it to set individual properties of the style object
     * @param element the element whose style properties are to be updated
     * @param styleAttribute the raw value of the style attribute
     * @throws XPathException
     */

    public static void setStyleProperties(Element element, String styleAttribute) {
        int semi = styleAttribute.indexOf(';');
        String first = (semi < 0 ? styleAttribute : styleAttribute.substring(0, semi));
        int colon = first.indexOf(':');
        if (colon > 0 && colon < first.length() - 1) {
            String prop = first.substring(0, colon).trim();
            // Turn the style name into camelCase
            prop = getCamelCaseName(prop);
            String value = first.substring(colon+1).trim();
            try {
            	element.getStyle().setProperty(prop, value);
            }  // IE throws illegal argument exception if property name is
               // not valid - ignore exception for consistency
            catch (JavaScriptException jex) {}
        }
        if (semi > 0 && semi < styleAttribute.length() - 2) {
        	setStyleProperties(element, styleAttribute.substring(semi+1));
        }
    }
    
    public static String getCamelCaseName(String prop) {
        while (prop.contains("-")) {
            int h = prop.indexOf('-');
            if (h > 0) { // preserve first char
	            String p = prop.substring(0, h) + Character.toUpperCase(prop.charAt(h+1));
	            if (h+2 < prop.length()) {
	                p += prop.substring(h+2);
	            }
	            prop = p;
            }
        }
        return prop;
    }

    public void startContent() throws XPathException {}

    /**
    * End of an element.
    */

    public void endElement () throws XPathException {
        currentNode = currentNode.getParentNode();
        level--;
    }


    /**
    * Character data.
    */

    public void characters(CharSequence chars) throws XPathException
    {
        if (level == 0 && nextSibling == null && Whitespace.isWhite(chars)) {
            return; // no action for top-level whitespace
        }

        try {
	        Text text = document.createTextNode(chars.toString());
	        if (nextSibling != null && level == 0) {
	            currentNode.insertBefore(text, nextSibling);
	        } else {
	            currentNode.appendChild(text);
	        }
        } catch(Exception e) {
        	String desc = (nextSibling != null && level == 0) ? "inserting" : "appending";
        	throw(new XPathException("DOM error " + desc + " text node with value: '" + chars.toString() + "' to node with name: " + currentNode.getNodeName()));
        }
    }


    /**
    * Handle a processing instruction.
    */

    public void processingInstruction(String target, CharSequence data)
        throws XPathException
    {
        // Processing instructions not in HTML DOM - but added for XML DOM
    	// TODO: Note that Node is a GWT HTML DOM object so an exception may be raised
    	// by appending the wrong node type!
    	if (mode == WriteMode.XML) {
	    	JavaScriptObject pi = createProcessingInstruction(document, target, data.toString());
	    	addNode(pi, "processing-instruction");
    	}	
    }

    /**
    * Handle a comment.
    */

    public void comment(CharSequence chars) throws XPathException
    {
        // Added for XML compatibility
    	if (mode == WriteMode.XML) {
	    	JavaScriptObject comment = createComment(document, chars.toString());
	    	addNode(comment, "comment");
    	}
    }
    
    public void addNode(JavaScriptObject newNode, String nodeType) throws XPathException {
        try {
	        if (nextSibling != null && level == 0) {
	        	insertBefore(nextSibling, newNode);
	        } else {
	            appendChild(currentNode, newNode);
	        }
        } catch(Exception e) {
        	String desc = (nextSibling != null && level == 0) ? "inserting" : "appending";
        	throw(new XPathException("DOM error " + desc + " " + nodeType + " node to node with name: " + currentNode.getNodeName()));
        }
    }
    
    public final native JavaScriptObject appendChild(JavaScriptObject parent, JavaScriptObject newChild) /*-{
       return parent.appendChild(newChild);
    }-*/;
    
    public final native JavaScriptObject insertBefore(JavaScriptObject targetNode, JavaScriptObject newNode) /*-{
       return targetNode.insertBefore(newNode);
    }-*/;

    /**
     * Set the attachment point for the new subtree
     * @param node the node to which the new subtree will be attached
    */
    
    public enum WriteMode {
    	NONE, XML, HTML
    }
    

    
    private WriteMode mode = WriteMode.NONE;

    public void setNode (Node node) {
        if (node == null) {
            return;
        }
        currentNode = node;
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            document = (Document)node;
        } else {
            document = currentNode.getOwnerDocument();
            if (document == null) {
                // which might be because currentNode() is a parentless ElementOverNodeInfo.
                // we create a DocumentOverNodeInfo, which is immutable, and will cause the DOMWriter to fail
                //document = new DocumentOverNodeInfo();
                // TODO:CLAXON - check this
            }
        }
        if (mode == WriteMode.NONE) {
        	Controller.APIcommand cmd = pipe.getController().getApiCommand();
        	mode = (cmd == APIcommand.TRANSFORM_TO_DOCUMENT || cmd == APIcommand.TRANSFORM_TO_FRAGMENT)?
        			WriteMode.XML : WriteMode.HTML;
        }
    }
    
    public Node getNode() {
    	// though this is changed by startElement it's reset by endElement and therefore,
    	// when called after close, should always return the initial container node
    	// - the document node or the documentFragment node.
    	return currentNode;
    }

    /**
     * Set next sibling
     * @param nextSibling the node, which must be a child of the attachment point, before which the new subtree
     * will be created. If this is null the new subtree will be added after any existing children of the
     * attachment point.
     */

    public void setNextSibling(Node nextSibling) {
        this.nextSibling = nextSibling;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
