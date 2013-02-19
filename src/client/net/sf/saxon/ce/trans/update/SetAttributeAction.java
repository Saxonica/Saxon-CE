package client.net.sf.saxon.ce.trans.update;

import client.net.sf.saxon.ce.dom.HTMLNodeWrapper;
import client.net.sf.saxon.ce.dom.HTMLWriter;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;



/**
 * A pending update action representing the effect of a delete expression
 */
public class SetAttributeAction extends PendingUpdateAction {

    private Element targetNode;
    private String uri;
    private String localName;
    private String value;

    /**
     * Create a SetAttributeAction
     * @param element the element whose attribute is to be set
     * @param localNname the attribute name
     * @param value the attribute value
     */

    public SetAttributeAction(Element element, String uri, String localNname, String value) {
        this.targetNode = element;
        this.uri = uri;
        this.localName = localNname;
        this.value = value;
    }

    /**
     * Apply the pending update action to the affected node
     *
     * @param context the XPath evaluation context
     */

    public void apply(XPathContext context) throws XPathException {
        if (NamespaceConstant.HTML_PROP.equals(uri)) {
            targetNode.setPropertyString(localName, value);
        } else if (NamespaceConstant.HTML_STYLE_PROP.equals(uri)) {
        	String name;
        	if(localName.length() > 1 && localName.charAt(0) == '_' && localName.charAt(1) == '-') {
        		name = localName.substring(1);
        	} else {
        		name = localName;
        	}
        	name = HTMLWriter.getCamelCaseName(name);
            targetNode.getStyle().setProperty(name, value);
        } else {
        	if (uri.length() == 0) {
        		targetNode.setAttribute(localName, value);
        		HTMLWriter.setAttributeProps(targetNode, localName, value);
        	} else {
        		HTMLWriter.setAttribute(targetNode.getOwnerDocument(), targetNode, localName, uri, value, HTMLWriter.WriteMode.HTML);
        	}
            if (localName.equals("style")) {
                // In IE, setting the style attribute dynamically has no effect on the individual style properties,
                // and does not affect the rendition of the element. So we parse out the content of the attribute,
                // and use it to set the individual properties.
                HTMLWriter.setStyleProperties(targetNode, value);
            }
        }
    }
    
    private native String getNodeNamespace(Node node) /*-{
		if (node.namespaceURI) {
			return node.namespaceURI;
		} else {
			return null;
		}
    }-*/;

    /**
     * Get the target node of the update action
     * @return the target node, the node to which this update action applies.
     */

    public Node getTargetNode() {
        return targetNode;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

