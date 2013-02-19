package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.ProcessingInstruction;

/**
 * This class implements the XML DOM ProcessingInstruction interface.
 */
class ProcessingInstructionImpl extends NodeXml implements
    ProcessingInstruction {

  protected ProcessingInstructionImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This function delegates to the native method <code>getData</code> in
   * XMLParserImpl.
   */
  public String getData() {
    return XMLParserImpl.getData(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>getTarget</code> in
   * XMLParserImpl.
   */
  public String getTarget() {
    return XMLParserImpl.getTarget(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>setData</code> in
   * XMLParserImpl.
   */
  public void setData(String data) {
    try {
      XMLParserImpl.setData(this.getJsObject(), data);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_CHARACTER_ERR, e, this);
    }
  }
  
  @Override
  public String toString() {
    return XMLParserImpl.getInstance().toStringImpl(this);
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

