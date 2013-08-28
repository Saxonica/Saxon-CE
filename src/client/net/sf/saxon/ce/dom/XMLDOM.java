package client.net.sf.saxon.ce.dom;

import client.net.sf.saxon.ce.trans.XPathException;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;

/**
 * A collection of static utility methods for accessing the XML capabilities (parsing and
 * serializing) of the browser Javascript platform. These are used in preference to the
 * XML capabilities offered in the GWT library.
 */

public class XMLDOM {
	       
	  /**
	   * Create a new XML DOM Document object
	   * @return a new empty XML DOM Document
	   */

	  public static native Document createDocument(String baseURI) /*-{
	  	var doc;
	  	if (document.implementation && document.implementation.createDocument) {
	    	doc = document.implementation.createDocument("", "", null);
	  	} else {
	  		try {
	  			doc = new ActiveXObject("MSXML2.DOMDocument");	  			
	  		} catch(e) {
	  			doc = new ActiveXObject("Microsoft.XMLDOM");
	  		}
	        doc.preserveWhiteSpace = true;
	        try {
		       if (xmlDoc.setProperty) {
				  xmlDoc.setProperty("ProhibitDTD", false);
			   }
		    } catch (e) { }
	  	}
	  	if (baseURI != null && doc.URL) {
	      	doc.URL = baseURI;
	     }
	     return doc;
	  }-*/;
	  
	  public static String makeHTTPRequest(String url) throws XPathException {
		  try {
			  return makeNativeHTTPRequest(url);
		  } catch(Exception e) {
			  throw new XPathException("error in Saxon.makeHTTPRequest: " + e.getMessage()); 
		  }
	  }
	  
	  public static JavaScriptObject parseXML(String text) throws XPathException {
		  try {
			  return parseNativeXML(text);
		  } catch (JavaScriptException je){
			  throw new XPathException("JS error in Saxon.parseXML: " + je.getMessage());			  
		  } catch(Exception e) {
			  throw new XPathException("error in Saxon.parseXML: " + e.getMessage()); 
		  }
	  }
	  
	  public static String serializeXML(Node node) throws XPathException {
		  try {
			  return serializeNativeXML(node);
		  } catch(Exception e) {
			  throw new XPathException("error in Saxon.serializeXML: " + e.getMessage()); 
		  }
	  }
	  
      public static native String makeNativeHTTPRequest(String url)/*-{
	    if (typeof XMLHttpRequest == "undefined") {
	        XMLHttpRequest = function () {
	            return new ActiveXObject("Msxml2.XMLHTTP.6.0");
	        };
	    }
	
	    var req = new XMLHttpRequest();
	
	    // false -> make a synchronous request
	    req.open("GET", url, false);
	    // for now, prevent caching
	    // pf note: this caused HTTP error with IIS7, so is commented out:
	    //req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2005 00:00:00 GMT");
	    // the following IE workaround is lossy - therefore IE restriction
	    // for non-ascii (unless utf-8 encoded) remains:
	    //req.setRequestHeader("Accept-Charset", "utf-8"); 
	    req.send(null);
	    var status = req.status;
	    if (status == 200 || status == 0) {
	        return req.responseText;
	    } else {
	        throw "HTTP request for " + url + " failed with status code: " + status;
	    }
      }-*/;
	        
	  public static native JavaScriptObject parseNativeXML(String text) /*-{
	  	  var xmlDoc;
	  	  if (window.DOMParser) // Most browsers
		  {
			   parser=new DOMParser();
			   xmlDoc=parser.parseFromString(text,"text/xml");
		  }
		  else // Internet Explorer
		  {
			   xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
			   xmlDoc.async=false;
			   xmlDoc.preserveWhiteSpace = true;
			   try {
			      if (xmlDoc.setProperty) {
					  xmlDoc.setProperty("ProhibitDTD", false);
				  }
			   } catch (e) {}
			   
		   	   xmlDoc.loadXML(text); 
		  }
		  return xmlDoc;
      }-*/;
	  
	  public static native String serializeNativeXML(Node node) /*-{
         if (typeof XMLSerializer != "undefined")
            return (new XMLSerializer()).serializeToString(node) ;
         else return node.xml;
      }-*/;
	
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

