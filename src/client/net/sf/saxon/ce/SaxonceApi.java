package client.net.sf.saxon.ce;

import java.util.logging.Logger;

import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.logging.client.LogConfiguration;
import com.google.gwt.user.client.Window;

/**
 * A class to provide static utiltiy functions for the JavaScript API. All API
 * functions must be registered within the JSNI <code>register</code> method.
 * Notes: GWT-Exporter not needed here because registering static methods is
 * quite straightforwards using JSNI, plus the Eclipse plug-in provides features
 * such as auto-complete which makes defining function signatures simpler.
 */
public class SaxonceApi {

	private static boolean processorWasJsInitiated = false;

	public static void setProcessorWasJsInitiated() {
		processorWasJsInitiated = true;
	}

	/**
	 * @return boolean to indicated to logging and other code that the
	 *         XSLTProcessor was initiated from the JavaScript API
	 */
	public static boolean doThrowJsExceptions() {
		return processorWasJsInitiated && (handler == null);
	}

	// API
	public static JavaScriptObject requestXML(String URI) throws Exception {

		try {
			String pageHref = Window.Location.getHref();
			String absSourceURI = (new URI(pageHref).resolve(URI)).toString();
			return createAsyncDoc(absSourceURI);
		} catch (Exception e) {
			// re-throw exception back to calling JavaScript
			throw (e);
		}
	}

	/**
	 * Returns a DocumentInfo object that wraps a XML DOM document.
	 * 
	 * If the JavaScript object passed as a parameter is not a DOM document, but
	 * simply a place-holder, then the Document is first fetched synchronously
	 * before wrapping.
	 * 
	 * @param obj
	 *            the DOM document or a place-holder
	 * @param config
	 *            The Saxon-CE configuration
	 * @return a DocumentInfo object
	 */
	public static DocumentInfo getDocSynchronously(JavaScriptObject obj,
			Configuration config) throws XPathException {
		String absSourceURI = getAsyncUri(obj);
		Document doc;
		try {
			if (absSourceURI != null) {
				try {
					String xml = XMLDOM.makeHTTPRequest(absSourceURI);
					doc = (Document) XMLDOM.parseXML(xml);
				} catch (Exception e) {
					throw new XPathException(
							"Synchronous HTTP GET failed for: " + absSourceURI);
				}
			} else {
				doc = (Document) obj;
			}
			// check there's a document element
			if (doc.getDocumentElement() == null) {
				throw new XPathException("no document element");
			}
		} catch (Exception e) {
			throw new XPathException("Error resolving document: "
					+ e.getMessage());
		}
		return (DocumentInfo) config.wrapXMLDocument(doc, absSourceURI);
	}

	/**
	 * Registers static methods of this SaxonApi class and the LogController
	 * class as static methods for use in the JavaScript API, within the Saxonce
	 * namespace. This method must be called when the Saxonce GWT module is
	 * first loaded.
	 */
	public static native void register() /*-{
		$wnd.Saxon = {};
		$wnd.Saxon.requestXML = $entry(function(url) {
			return @client.net.sf.saxon.ce.SaxonceApi::requestXML(Ljava/lang/String;)(url)
		});
		$wnd.Saxon.parseXML = $entry(function(text) {
			return @client.net.sf.saxon.ce.dom.XMLDOM::parseXML(Ljava/lang/String;)(text)
		});
		$wnd.Saxon.serializeXML = $entry(function(node) {
			return @client.net.sf.saxon.ce.dom.XMLDOM::serializeXML(Lcom/google/gwt/dom/client/Node;)(node)
		});
		$wnd.Saxon.setErrorHandler = $entry(function(handler) {
			return @client.net.sf.saxon.ce.SaxonceApi::setErrorHandler(Lcom/google/gwt/core/client/JavaScriptObject;)(handler)
		});
		$wnd.Saxon.setLogLevel = $entry(function(level) {
			return @client.net.sf.saxon.ce.LogController::setLogLevel(Ljava/lang/String;)(level)
		});
		$wnd.Saxon.getLogLevel = $entry(function() {
			return @client.net.sf.saxon.ce.LogController::getLogLevel()()
		});
		$wnd.Saxon.newXSLT20Processor = function(doc) {
			var sp = new $wnd.Saxonce.XSLT20Processor(doc);
			sp.setThis(sp);
			return sp;
		};
		$wnd.Saxon.getErrorHandler = $entry(function() {
			return @client.net.sf.saxon.ce.SaxonceApi::getErrorHandler()()
		});
		$wnd.Saxon.run = $entry(function(cmd) {
			return @client.net.sf.saxon.ce.SaxonceApi::runCommand(Lcom/google/gwt/core/client/JavaScriptObject;)(cmd)
		});
		$wnd.Saxon.getVersion = $entry(function() {
			return @client.net.sf.saxon.ce.Version::getProductVersion()()
		});
	}-*/;

	/**
	 * Factory method for JavaScript API to create new XSLT20Processor Converts
	 * this to new Saxonce.XSLT20Processor(doc)
	 */
	public static native JavaScriptObject newXSLT20Processor(
			JavaScriptObject doc) /*-{
		return @client.net.sf.saxon.ce.XSLT20Processor::new(Lcom/google/gwt/core/client/JavaScriptObject;)(doc);
	}-*/;

	private static JavaScriptObject handler = null;

	/**
	 * API call to set the function used by <code>initCallback()</code> to make
	 * a JavaScript callback when a logging event occurs
	 * 
	 * @param handlerFunction
	 *            - An instance of a JavaScript function
	 */
	public static void setErrorHandler(JavaScriptObject handlerFunction) {
		handler = handlerFunction;
	}

	/**
	 * Public API function - but also used internally Return the function object
	 * thats the error handler. This is the external error handler (set by a
	 * hosting editor for example) of if there is none then the error handler
	 * set using <code>setErrorHandler()
	 */
	public static JavaScriptObject getErrorHandler() {
		return handler;
	}

	public static void setAnyExternalErrorHandler() {
		logHandlerExternal = callExternalErrorHandler(
				Version.getProductTitle(), "INIT");
	}

	private static boolean logHandlerExternal = false;

	public static boolean isLogHandlerExternal() {
		return logHandlerExternal;
	}

	private static native boolean callExternalErrorHandler(String message,
			String level) /*-{
		if ($wnd.external) {
			try {
				$wnd.external.saxonErrorHandler(message, level);
				return true;
			} catch (e) {
				return false;
			}
		} else {
			return false;
		}
	}-*/;

	/**
	 * Calls back into the JavaScript calling code to allow logging of errors
	 * and events from within JavaScript
	 * 
	 * @param message
	 *            - The error message
	 * @param errorType
	 *            - The error type - can be any type used by GWT-Logging
	 */
	public static void makeCallback(String message, String errorType,
			String milliseconds) {
		JavaScriptObject currentHandler = getErrorHandler();
		setErrorMessage(message);
		if (currentHandler == null && !isLogHandlerExternal())
			return;

		if (!callbackErrorReported) {
			boolean success = false;

			if (currentHandler != null) {
				JavaScriptObject evt = createEventObject(message, errorType,
						milliseconds);
				success = initCallback(currentHandler, evt);
				logAnyCallbackError(success, "JS");
			}

			if (isLogHandlerExternal()) {
				success = callExternalErrorHandler(message, errorType);
				logAnyCallbackError(success, "Ext");
			}

		}
	}

	public static void logAnyCallbackError(boolean success, String name) {
		if (LogConfiguration.loggingIsEnabled() && !success) {
			callbackErrorReported = true; // prevent recursion
			Logger.getLogger("HandlerCallback").severe(
					"Exception on " + name + " errorHandler callback");
		}
	}

	static boolean callbackErrorReported = false;

	/**
	 * Creates an event object for use in the JavaScript API callback
	 * 
	 * @param message
	 *            The event message
	 * @param errorType
	 *            The event type e.g. FINE
	 * @return the event object
	 */
	private static native JavaScriptObject createEventObject(String message,
			String errorType, String milliseconds) /*-{
		var eventObj = {}
		eventObj.message = message;
		eventObj.level = errorType;
		eventObj.time = milliseconds;
		return eventObj;
	}-*/;

	private static native boolean initCallback(JavaScriptObject handlerFn,
			JavaScriptObject eventObj) /*-{
		try {
			handlerFn.call(this, eventObj);
			return true;
		} catch (e) {
			return false;
		}
	}-*/;

	private static native void setErrorMessage(String msg) /*-{
		if ($wnd.Saxon) {
			$wnd.Saxon.message = msg;
		} else {
			$wnd.SaxonMessage = msg;
		}
	}-*/;

	public static native JavaScriptObject createAsyncDoc(String URI) /*-{
		var docObj = {}
		docObj.asyncUri = URI;
		return docObj;
	}-*/;

	public static native String getAsyncUri(JavaScriptObject obj) /*-{
		if (obj.asyncUri) {
			return obj.asyncUri;
		} else {
			return null;
		}
	}-*/;

	public static native JavaScriptObject runCommand(JavaScriptObject cmd) /*-{
		var proc = $wnd.Saxon.newXSLT20Processor();
		var methodVal = null;
		var sourceVal = null;
		var sourceDoc = null;
		var result = null;
		for ( var p in cmd) {
			if (cmd.hasOwnProperty(p)) {
				var pValue = cmd[p];
				switch (p) {
				case "baseOutputURI":
					proc.setBaseOutputURI(pValue);
					break;
				case "initialMode":
					proc.setInitialMode(pValue);
					break;
				case "initialTemplate":
					proc.setInitialTemplate(pValue);
					break;
				case "stylesheet":
					if (typeof pValue == 'string' || pValue instanceof String) {
						try {
							var s = $wnd.Saxon.requestXML(pValue);
							proc.importStylesheet(s);
						} catch (e) {
							throw "Saxon.run error: on importing stylesheet "
									+ pValue;
						}
					} else {
						proc.importStylesheet(pValue); //assume Document or RequestDocument
					}
					break;
				case "logLevel":
					$wnd.Saxon.setLogLevel(pValue);
					break;
				case "errorHandler":
					$wnd.Saxon.setErrorHandler(pValue);
					break;
				case "method":
					methodVal = pValue;
					break;
				case "success":
					proc.setSuccess(pValue);
					break;
				case "source":
					if (typeof pValue == 'string' || pValue instanceof String) {
						sourceDoc = $wnd.Saxon.requestXML(pValue);
					} else {
						sourceDoc = pValue;
					}
					break;
				case "parameters":
					for ( var x in pValue) {
						if (pValue.hasOwnProperty(x)) {
							proc.setParameter(null, x, pValue[x]);
						}
					}
					break;
				} //switch
			} //if
		} // for

		if (methodVal == "transformToFragment") {
			proc.transformToFragment(sourceDoc, null);
		} else if (methodVal == "transformToHTMLFragment") {
			proc.transformToHTMLFragment(sourceDoc, null);
		} else if (methodVal == "transformToDocument") {
			proc.transformToDocument(sourceDoc);
		} else {
			proc.updateHTMLDocument(sourceDoc, null);
		}
		return proc;
	}-*/;

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
