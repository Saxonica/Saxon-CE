package client.net.sf.saxon.ce.xmldom;

import com.google.gwt.xml.client.DOMException;

/**
 * Thrown when a particular DOM item causes an exception.
 */
public class DOMNodeExceptionXml extends DOMException {

  private DomItemXml item;

  public DOMNodeExceptionXml() {
    super((short) 0, "node exception");
  }

  public DOMNodeExceptionXml(short code, Throwable e, DomItemXml item) {
    // This item must be initialized during construction, and Java does not
    // allow any statements before the super, so
    // toString must be evaluated twice
    super(code, "Error during DOM manipulation of: "
        + summarize(item.toString()));
    initCause(e);
    this.item = item;
  }
  
  private static final int MAX_SUMMARY_LENGTH = 128;

  public static String summarize(String text) {
    return text.substring(0, Math.min(text.length(), MAX_SUMMARY_LENGTH));
  }

  public DomItemXml getItem() {
    return item;
  }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
