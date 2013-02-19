package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.Attr;

/**
 * This class implements the XML Attr interface.
 */
class AttrImpl extends NodeXml implements Attr {
  protected AttrImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This function delegates to the native method <code>getName</code> in
   * XMLParserImpl.
   */
  public String getName() {
    return XMLParserImpl.getName(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>getSpecified</code> in
   * XMLParserImpl.
   */
  public boolean getSpecified() {
    return XMLParserImpl.getSpecified(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>getValue</code> in
   * XMLParserImpl.
   */
  public String getValue() {
    return XMLParserImpl.getValue(this.getJsObject());
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
