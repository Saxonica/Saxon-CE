package client.net.sf.saxon.ce.xmldom;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.CDATASection;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.Text;


/**
 * This class represents the client interface to XML parsing.
 * Warning: This class was 'migrated' from GWT code solely for
 * the purpose of fixing the issue that Internet Explorer can't
 * load XML with DTD declarations using responseText, responseXML
 * must be used.
 * 
 * The only method for this class that should be called is the
 * @wrap method - all others would normally rely on GWT deferred
 * binding, so can't be guaranteed to work in this context where
 * deferred binding is not available.
 */
public class XMLParser {

  private static final XMLParserImpl impl = XMLParserImpl.getInstance();

  /**
   * This method creates a new document, to be manipulated by the DOM API.
   * 
   * @return the newly created document
   */
  public static Document createDocument() {
    return impl.createDocument();
  }

  /**
   * This method parses a new document from the supplied string, throwing a
   * <code>DOMParseException</code> if the parse fails.
   * 
   * @param contents the String to be parsed into a <code>Document</code>
   * @return the newly created <code>Document</code>
   */
  public static Document parse(String contents) {
    return impl.parse(contents);
  }
  
  /**
   * The only method in this class that should be used
   * @param jso the javascript object to wrap
   * @return a document object that's a wrapper for the
   * input parameter
   */
  public static Document wrap(JavaScriptObject jso) {
	return impl.wrapJsDocument(jso);
  }

  /**
   * This method removes all <code>Text</code> nodes which are made up of only
   * white space.
   * 
   * @param n the node which is to have all of its whitespace descendents
   *          removed.
   */
  public static void removeWhitespace(Node n) {
    removeWhitespaceInner(n, null);
  }

  /**
   * This method determines whether the browser supports {@link CDATASection} 
   * as distinct entities from <code>Text</code> nodes.
   * 
   * @return true if the browser supports {@link CDATASection}, otherwise
   *         <code>false</code>.
   */
  public static boolean supportsCDATASection() {
    return impl.supportsCDATASection();
  }

  /*
   * The inner recursive method for removeWhitespace
   */
  private static void removeWhitespaceInner(Node n, Node parent) {
    // This n is removed from the parent if n is a whitespace node
    if (parent != null && n instanceof Text && (!(n instanceof CDATASection))) {
      Text t = (Text) n;
      if (t.getData().matches("[ \t\n]*")) {
        parent.removeChild(t);
      }
    }
    if (n.hasChildNodes()) {
      int length = n.getChildNodes().getLength();
      List<Node> toBeProcessed = new ArrayList<Node>();
      // We collect all the nodes to iterate as the child nodes will change 
      // upon removal
      for (int i = 0; i < length; i++) {
        toBeProcessed.add(n.getChildNodes().item(i));
      }
      // This changes the child nodes, but the iterator of nodes never changes
      // meaning that this is safe
      for (Node childNode : toBeProcessed) {
        removeWhitespaceInner(childNode, n);
      }
    }
  }

  /**
   * Not instantiable.
   */
  private XMLParser() {
  }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

