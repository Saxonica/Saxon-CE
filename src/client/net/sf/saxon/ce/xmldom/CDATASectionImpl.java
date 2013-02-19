package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.CDATASection;

/**
 * This class implements the CDATASectionImpl interface.  
 */

class CDATASectionImpl extends TextImpl implements CDATASection {
  protected CDATASectionImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This method returns the string representation of this 
   * <code>CDATASectionImpl</code>.
   * @return the string representation of this <code>CDATASectionImpl</code>.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer b = new StringBuffer("<![CDATA[");
    b.append(getData());
    b.append("]]>");
    return b.toString();
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

