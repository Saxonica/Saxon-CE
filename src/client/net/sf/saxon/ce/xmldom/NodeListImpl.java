package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

/**
 * This class implements the NodeList interface using the underlying
 * JavaScriptObject's implementation.
 */
class NodeListImpl extends DomItemXml implements NodeList {

  protected NodeListImpl(JavaScriptObject o) {
    super(o);
  }

  public int getLength() {
    return XMLParserImpl.getLength(this.getJsObject());
  }

  /**
   * This method gets the index item.
   * 
   * @param index - the index to be retrieved
   * @return the item at this index
   * @see com.google.gwt.xml.client.NodeList#item(int)
   */
  public Node item(int index) {
    return NodeXml.build(XMLParserImpl.item(this.getJsObject(), index));
  }

  @Override
  public String toString() {
    StringBuffer b = new StringBuffer();
    for (int i = 0; i < getLength(); i++) {
      b.append(item(i).toString());
    }
    return b.toString();
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
