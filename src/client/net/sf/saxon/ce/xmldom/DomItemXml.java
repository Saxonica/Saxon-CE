package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class is the base class for all DOM object wrappers.
 */
public class DomItemXml {

  private JavaScriptObject jsObject;

  protected DomItemXml(JavaScriptObject jso) {
    this.jsObject = jso;
  }

  /**
   * This method determines equality for DOMItems.
   * 
   * @param o - the other object being tested for equality
   * @return true iff the two objects are equal.
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object o) {
    if (o instanceof DomItemXml) {
      return this.getJsObject() == ((DomItemXml) o).getJsObject();
    }
    return false;
  }
  
  /**
   * Returns the hash code for this DOMItem.
   */
  @Override
  public int hashCode() {
    return jsObject.hashCode();
  }

  JavaScriptObject getJsObject() {
    return jsObject;
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

