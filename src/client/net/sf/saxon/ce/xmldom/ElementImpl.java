package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;

/**
 * This method implements the Element interface.
 */
class ElementImpl extends NodeXml implements Element {

  protected ElementImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This function delegates to the native method <code>getAttribute</code> in
   * XMLParserImpl.
   */
  public String getAttribute(String tagName) {
    return XMLParserImpl.getAttribute(this.getJsObject(), tagName);
  }

  /**
   * This function delegates to the native method <code>getAttributeNode</code>
   * in XMLParserImpl.
   */
  public Attr getAttributeNode(String tagName) {
    return (Attr) NodeXml.build(XMLParserImpl.getAttributeNode(
        this.getJsObject(), tagName));
  }

  /**
   * This function delegates to the native method
   * <code>getElementsByTagName</code> in XMLParserImpl.
   */
  public NodeList getElementsByTagName(String tagName) {
    return new NodeListImpl(XMLParserImpl.getElementsByTagName(
        this.getJsObject(), tagName));
  }

  /**
   * This function delegates to the native method <code>getTagName</code> in
   * XMLParserImpl.
   */
  public String getTagName() {
    return XMLParserImpl.getTagName(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>hasAttribute</code> in
   * XMLParserImpl.
   */
  public boolean hasAttribute(String tagName) {
    return getAttribute(tagName) != null;
  }

  /**
   * This function delegates to the native method <code>removeAttribute</code>
   * in XMLParserImpl.
   */
  public void removeAttribute(String name) throws DOMNodeExceptionXml {
    try {
      XMLParserImpl.removeAttribute(this.getJsObject(), name);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>setAttribute</code> in
   * XMLParserImpl.
   */
  public void setAttribute(String name, String value) throws DOMNodeExceptionXml {
    try {
      XMLParserImpl.setAttribute(this.getJsObject(), name, value);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

