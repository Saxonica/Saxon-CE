package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.CharacterData;
import com.google.gwt.xml.client.DOMException;

/**
 * This class implements the CharacterData interface.
 */
abstract class CharacterDataImpl extends NodeXml implements
    CharacterData {

  protected CharacterDataImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This function delegates to the native method <code>appendData</code> in
   * XMLParserImpl.
   */
  public void appendData(String arg) {
    try {
      XMLParserImpl.appendData(this.getJsObject(), arg);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>deleteData</code> in
   * XMLParserImpl.
   */
  public void deleteData(int offset, int count) {
    try {
      XMLParserImpl.deleteData(this.getJsObject(), offset, count);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>getData</code> in
   * XMLParserImpl.
   */
  public String getData() {
    return XMLParserImpl.getData(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>getLength</code> in
   * XMLParserImpl.
   */
  public int getLength() {
    return XMLParserImpl.getLength(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>insertData</code> in
   * XMLParserImpl.
   */
  public void insertData(int offset, String arg) {
    try {
      XMLParserImpl.insertData(this.getJsObject(), offset, arg);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>replaceData</code> in
   * XMLParserImpl.
   */
  public void replaceData(int offset, int count, String arg) {
    try {
      XMLParserImpl.replaceData(this.getJsObject(), offset, count, arg);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>setData</code> in
   * XMLParserImpl.
   */
  public void setData(String data) {
    try {
      XMLParserImpl.setData(this.getJsObject(), data);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>substringData</code>
   * in XMLParserImpl.
   */
  public String substringData(final int offset, final int count) {
    try {
      return XMLParserImpl.substringData(this.getJsObject(), offset, count);
    } catch (JavaScriptException e) {
      throw new DOMNodeExceptionXml(DOMException.INVALID_ACCESS_ERR, e, this);
    }
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


