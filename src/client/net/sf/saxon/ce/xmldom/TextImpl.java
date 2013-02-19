package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Text;

/**
 * This class is the implementation of the XML DOM Text interface.
 */
class TextImpl extends CharacterDataImpl implements Text {

  protected TextImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This function delegates to the native method <code>splitText</code> in
   * XMLParserImpl.
   */
  public Text splitText(int offset) {
    try {
      return (Text) NodeXml.build(XMLParserImpl.splitText(this.getJsObject(),
        offset));
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  @Override
  public String toString() {
    StringBuffer b = new StringBuffer();
    String[] x = getData().split("(?=[;&<>\'\"])", -1);
    for (int i = 0; i < x.length; i++) {
      if (x[i].startsWith(";")) {
        b.append("&semi;");
        b.append(x[i].substring(1));
      } else if (x[i].startsWith("&")) {
        b.append("&amp;");
        b.append(x[i].substring(1));
      } else if (x[i].startsWith("\"")) {
        b.append("&quot;");
        b.append(x[i].substring(1));
      } else if (x[i].startsWith("'")) {
        b.append("&apos;");
        b.append(x[i].substring(1));
      } else if (x[i].startsWith("<")) {
        b.append("&lt;");
        b.append(x[i].substring(1));
      } else if (x[i].startsWith(">")) {
        b.append("&gt;");
        b.append(x[i].substring(1));
      } else {
        b.append(x[i]);
      }
    }
    return b.toString();
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


