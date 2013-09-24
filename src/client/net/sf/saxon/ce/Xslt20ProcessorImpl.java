package client.net.sf.saxon.ce;

import client.net.sf.saxon.ce.Controller.APIcommand;
import client.net.sf.saxon.ce.client.HTTPHandler;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper.DocType;
import client.net.sf.saxon.ce.dom.Sanitizer;
import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.lib.JavaScriptAPIException;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.StandardErrorListener;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.JSObjectPattern;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.Rule;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.logging.client.LogConfiguration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Window;
import org.timepedia.exporter.client.ExporterUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import com.google.gwt.xml.client.XMLParser;


/**
 * This class represents the XSLT 2.0 processor, which is the top-level object implemented by Saxon-CE
 * Logging is implemented using GWT Logging - documentation at: 
 * http://code.google.com/webtoolkit/doc/latest/DevGuideLogging.html
 */
public class Xslt20ProcessorImpl implements EntryPoint {

    final Configuration config = new Configuration();
    private boolean registeredForEvents = false;
    private boolean principleEventListener = false;
    Executable stylesheet = null;
    private JavaScriptObject successCallback = null;
    Controller localController = new Controller(config, true);       
    private static Logger logger = Logger.getLogger("XSLT20Processor");
   
    public void onModuleLoad() {
    	if (LogConfiguration.loggingIsEnabled()){
            SaxonceApi.setAnyExternalErrorHandler();
    		LogController.initLogger();
    		LogController.addJavaScriptLogHandler();
    	}
    	
    	logger.log(Level.FINE, "GWT Module Load initated by page: " + Document.get().getTitle());
    	if (LogConfiguration.loggingIsEnabled()) {
    		String href = Window.Location.getHref();
    		if (href != null && href.startsWith("file:")) {
    			logger.warning("The file:// protocol in use may cause 'permission denied' errors in Saxon-CE - unless the browser's 'strict-origin-policy' has been relaxed.");
    		}
    	}
        //register();
        ExporterUtil.exportAll();
        SaxonceApi.register();

        // License code commented out - change 1.1 from decision: saxon-ce opensource     
/*        try {
        	Verifier.loadLicense();
        	if (LogConfiguration.loggingIsEnabled()){
        		Verifier.displayLicenseMessage();
        	}
        		
        } catch (LicenseException le) {
        	handleException(le, "onModuleLoad");
        	return;
        } */
        
        // perform JavaScript callback or use <script> element to initate transform
        JavaScriptObject saxonceLoadCallback = getCallback();
        if (saxonceLoadCallback != null) {
        	logger.log(Level.FINE, "Executing 'onSaxonceLoad' callback...");
        	try {
        	executeCallback(saxonceLoadCallback);
        	} catch(JavaScriptException jse) {
        		handleException(jse, "onModuleLoad");
        	} catch(Exception je) {
        		handleException(je, "onModuleLoad");
        	}
        }
    	// do transform based on <script> element - this might be needed as
        // as a callback - for test-harness for example
    	doTransformation();
    }
    
    private native JavaScriptObject getCallback() /*-{
       if ($wnd.onSaxonLoad && typeof $wnd.onSaxonLoad == 'function') {
       	  return $wnd.onSaxonLoad;
       } else {
       		return null;
       }
     }-*/;
    
    // sets the window object as the owner object
    private native void executeCallback(JavaScriptObject callback) /*-{
         callback.apply($wnd);
    }-*/;
          
    // script nodes found when module loaded
    private static NodeList<Element> scriptsOnLoad;
    
    /**
     * Gets script element list the first time this is called
     * Used by Verifier and Xslt20Processor.doTransformation()
     */
    public static NodeList<Element> getScriptsOnLoad() {
    	if (scriptsOnLoad == null) {
    		scriptsOnLoad = Document.get().getElementsByTagName("s" +
    				"cript");
    	}
    	return scriptsOnLoad;
    }
    
    public JavaScriptObject getSuccess() {
    	return successCallback;
    }
    
    public XSLT20Processor successOwner = null;
    
    public void setSuccess(JavaScriptObject sFunction, XSLT20Processor sOwner) {
    	successCallback = sFunction;
    	successOwner = sOwner;
    }
    
   
    
    /**
     * The entry point for transforms initiate on module load,
     * fetches settings from the <code>application/xslt+xml style</code> element
     */
    public void doTransformation() {
    	Logger logger = Logger.getLogger("Xstl20Processor");
        try {       	
            NodeList<Element> scripts = getScriptsOnLoad();
            String sourceURI = null;
            String styleURI = null;
            String initialMode = null;
            String initialTemplate = null;
            boolean styleElementExists = false;
            for (int i=0; i<scripts.getLength(); i++) {
                String type = scripts.getItem(i).getAttribute("type");
                if (type.equals("application/xslt+xml")) {
                	styleElementExists = true;
                    styleURI = scripts.getItem(i).getAttribute("src");
                    sourceURI = scripts.getItem(i).getAttribute("data-source");
                    initialMode = scripts.getItem(i).getAttribute("data-initial-mode");
                    initialTemplate = scripts.getItem(i).getAttribute("data-initial-template");
                    break;
                }
            }
            
            if (!styleElementExists) {
            	logger.info("Saxon-CE API initialised");
            	return;
            } else if (styleURI == null)
            	{ throw new XPathException("No XSLT stylesheet reference found"); }

            JavaScriptObject sourceDoc = null;
            String absSourceURI = null;
            if (sourceURI != null && sourceURI.length() != 0) {
            	String pageHref = Window.Location.getHref();
                absSourceURI = (new URI(pageHref).resolve(sourceURI)).toString();
                if (pageHref.equals(absSourceURI)) {
                	throw new XPathException("Cannot load XML with same URI as the host page");
                }

                sourceDoc = SaxonceApi.createAsyncDoc(absSourceURI); // config.buildDocument(absSourceURI);

            } else if (initialTemplate == null) {
            	throw new XPathException("No data-source attribute or data-initial-template value found - one is required");
            }

            String absStyleURI = (new URI(Window.Location.getHref()).resolve(styleURI)).toString();
            DocumentInfo styleDoc;
            try {
                styleDoc = config.buildDocument(absStyleURI);
            } catch (XPathException e) {
            	String reportURI = (absSourceURI != null)? absSourceURI : styleURI;
            	throw new XPathException("Failed to load XSLT stylesheet " + reportURI + ": " + e.getMessage());
            }
            config.getDocumentPool().add(styleDoc, absStyleURI);     // where document('') can find it
            Element body = getBodyElement();
            
            localController.setInitialMode(initialMode);
            localController.setInitialTemplate(initialTemplate);
            localController.setApiCommand(APIcommand.UPDATE_HTML);
            localController.setTargetNode(Document.get());          // target node is the document node
            
            renderXML(sourceDoc, styleDoc, body);                   // principal output is to the body element

        } catch (Exception err) {
        	logger.log(Level.SEVERE, err.getMessage());
        }
    }
    
    public static Element getBodyElement() {
    	Element body = Document.get().getElementsByTagName("BODY").getItem(0);
    	if (body == null) {
    		body = Document.get().getElementsByTagName("body").getItem(0);
    	}
    	return body;
    }
       
    public void continueWithSourceDocument(
            final DocumentInfo sourceDoc,
            final String styleURI,
            final String initialMode,
            final String initialTemplate) {
//        RequestBuilder request = new RequestBuilder(RequestBuilder.GET, styleURI);
//        request.setHeader("Cache-Control", "no-cache");
//        request.setCallback(new RequestCallback() {
//            public void onResponseReceived(Request request, Response response) {
//                com.google.gwt.xml.client.Document styleXML = XMLParser.parse(response.getText());
//                DocumentInfo styleDoc = new XMLDocumentWrapper(styleXML, styleURI, config);
//                renderXML(sourceDoc, styleDoc, initialMode, initialTemplate, Document.get().getElementById("target"));
//            }
//            public void onError(Request request, Throwable exception) {
//                Window.alert(exception.getMessage());
//            }
//        });
    }
    
    /**
     * Implementation of XSLT20Processor API - only handles xml dom - must
     * use transformToFragment or updateHTMLDocument for html dom
     * @param sourceDoc
     */
    public void updateHTMLDocument(JavaScriptObject sourceDoc, Document targetDoc, APIcommand cmd) {
		if (targetDoc == null) {
			targetDoc = Document.get();
		}
		localController.setApiCommand(cmd);
		localController.setTargetNode(targetDoc);
    	renderXML(sourceDoc, importedStylesheet, getBodyElement());
    }
    
    public Node transformToDocument(JavaScriptObject sourceDoc) {
		localController.setTargetNode(XMLDOM.createDocument(localController.getBaseOutputURI()));
		localController.setApiCommand(APIcommand.TRANSFORM_TO_DOCUMENT);
    	Document targetDoc = XMLDOM.createDocument(localController.getBaseOutputURI());
    	return renderXML(sourceDoc, importedStylesheet, targetDoc);
    }
    
    public Node transformToFragment(JavaScriptObject sourceDoc, Document ownerDocument) {
    	Document owner = (ownerDocument == null)? XMLDOM.createDocument(localController.getBaseOutputURI()) : ownerDocument;
		Node targetDocumentFragment = HTMLDocumentWrapper.createDocumentFragment(owner);
		// Set the owner document for result document output fragments:
		localController.setTargetNode(owner); 
		localController.setApiCommand(APIcommand.TRANSFORM_TO_FRAGMENT);
    	return renderXML(sourceDoc, importedStylesheet, targetDocumentFragment);
    }
    
    private DocumentInfo importedStylesheet;
    
    /**
     * Imports new stylesheet and de-registers sinks for specific events from
     * any previous stylesheet for this processor
     * @param doc
     */
    public void importStylesheet(JavaScriptObject doc) {
    	deregisterEventHandlers();
    	try {
    	importedStylesheet = SaxonceApi.getDocSynchronously(doc, config);
    	} catch(XPathException e) {
    		handleException(e, "importStylesheet");
    	}
    }
    
    // fetched either synchronously or asynchronously
    NodeInfo fetchedSourceDoc;
    boolean transformInvoked;
    boolean docFetchRequired;
   
    public Node renderXML(JavaScriptObject inSourceDoc,
                          DocumentInfo styleDoc,
                          com.google.gwt.dom.client.Node target) {
        try {
        	if (styleDoc == null) {
        		throw new Exception("Stylesheet for transform is null");
        	}
        	docFetchRequired = inSourceDoc != null;
            config.setErrorListener(new StandardErrorListener());

            String asyncSourceURI = null;

            // for now - don't use aync when using the JavaScript API calls that return a result
            if (docFetchRequired && 
            		(localController.getApiCommand() == APIcommand.UPDATE_HTML || (successCallback != null))) {
            	asyncSourceURI = SaxonceApi.getAsyncUri(inSourceDoc);
            	if (asyncSourceURI != null && asyncSourceURI.toLowerCase().startsWith("file:")) {
            		asyncSourceURI = null; // force synchronous fetch if using file-system protocol
            	}
            }

            
            // ----------- Start async code -------------
            fetchedSourceDoc = null;
            transformInvoked = false;
            
            if (asyncSourceURI != null) {
	            final String URI = asyncSourceURI;
	            final Node transformTarget = target;

	            logger.log(Level.FINE, "Aynchronous GET for: " + asyncSourceURI);
	            final HTTPHandler hr = new HTTPHandler();

	 	        hr.doGet(asyncSourceURI, new RequestCallback() {
	 	        	
	 		        public void onError(Request request, Throwable exception) {
	 		        	//hr.setErrorMessage(exception.getMessage());
	 		        	String msg = "HTTP Error " + exception.getMessage() + " for URI " + URI;
	 		        	handleException (new RuntimeException(msg), "onError");
	 		        }
	
	 		        public void onResponseReceived(Request request, Response response) {
	 		        	int statusCode = response.getStatusCode();
	 		            if (statusCode == 200) {
	 		              Logger.getLogger("ResponseReceived").fine("GET Ok for: " + URI);
	 		              Node responseNode;
	 		              try {
	 		              responseNode = (Node)XMLDOM.parseXML(response.getText());
	 		              } catch(Exception e) {
	 		            	 handleException (new RuntimeException(e.getMessage()), "onResponseReceived");
	 		            	 return;
	 		              }
	 		              DocumentInfo responseDoc = config.wrapXMLDocument(responseNode, URI);
	 		              // now document is here, we can transform it
	 		              Node result = invokeTransform(responseDoc, transformTarget);
	 		              hr.setResultNode(result); // TODO: This isn't used yet
	 		                // handle OK response from the server 
	 		            } else if (statusCode < 400) {
	 		            	// transient
	 		            } else {
	 		            	String msg = "HTTP Error " + statusCode + " " + response.getStatusText() + " for URI " + URI;
	 		            	handleException (new RuntimeException(msg), "onResponseReceived");
	 		            	//hr.setErrorMessage(statusCode + " " + response.getStatusText());
	 		            }
	 		        } // ends inner method
	 		      }// ends inner class
	 	        ); // ends doGet method call
            }
            // -------------- End async code
                       
            /// we can compile - even while sourcedoc is being fetched asynchronously
            
            if (stylesheet == null) {
            	if (LogConfiguration.loggingIsEnabled()) {
            		LogController.InitializeTraceListener();
            	}
            	logger.log(Level.FINE, "Compiling Stylesheet...");
	            Executable sheet = new Executable(config);
	            sheet.prepare(styleDoc);
	            stylesheet = sheet;
	            logger.log(Level.FINE, "Stylesheet compiled OK");
            }
            
            // for async operation - this is called within the callback - so don't call here            
            if (asyncSourceURI == null && inSourceDoc != null) {
            	int nodeType = (Node.is(inSourceDoc))? ((Node)inSourceDoc).getNodeType() : 0;

            	if (nodeType > 0 && nodeType != Node.DOCUMENT_NODE) {
            		// add a document node wrapper
            		Node sourceNode = (Node)inSourceDoc;
            		Document sourceDoc = sourceNode.getOwnerDocument();
        	        HTMLDocumentWrapper htmlDoc = new HTMLDocumentWrapper(sourceDoc, sourceDoc.getURL(), config, DocType.UNKNOWN);
        	        fetchedSourceDoc = htmlDoc.wrap(sourceNode);
            	} else {
            		fetchedSourceDoc = SaxonceApi.getDocSynchronously(inSourceDoc, config);
            	}
            }

            if (stylesheet.getStripperRules().isStripping()) {
                new Sanitizer(stylesheet.getStripperRules()).sanitize((HTMLDocumentWrapper)fetchedSourceDoc);
            }
            // this method only runs if transformInvoked == false - need to get sourceDoc reference if not invoked

            return invokeTransform(fetchedSourceDoc, target);

            //method ends - allowing onResponceReceived handler to call invokeTransform for async operation
        } catch (Exception e) {
        	handleException(e, "renderXML");       
            return null;
        }
    }
    
    public static native boolean isNonDocNode(JavaScriptObject obj) /*-{
		return (typeof obj.getNodeType == "function" && obj.getNodeType() != 9);
	}-*/;
          
    private List<Mode> registeredEventModes = null;
    private boolean registeredProcessorForNonDomEvents = false;
    
    private void registerNonDOMevents(Controller controller) throws XPathException {
        for (Mode eventMode : registeredEventModes) {
        	ArrayList<Rule> nonDomRules = eventMode.getVirtualRuleSet();
        	if (nonDomRules != null) {
        		if (!registeredProcessorForNonDomEvents){
        			registeredProcessorForNonDomEvents = true;
        			Controller.addNonDomEventProcessor(this);
        		}
        		String eventName = eventMode.getModeName().getLocalName();
        		for (Rule r : nonDomRules) {
        			JavaScriptObject eventTarget = null;
        			eventTarget = ((JSObjectPattern)r.getPattern()).evaluate(controller.newXPathContext());
        			bindTemplateToWindowEvent(eventName, eventTarget);
        		}
        	}
        	
        }

    }
       
    public static native String bindTemplateToWindowEvent(String eventName, JavaScriptObject target) /*-{
		target[eventName] = $entry(function(eventArg) {@client.net.sf.saxon.ce.Xslt20ProcessorImpl::relayNonDomEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, target, eventArg)});
	}-*/;
    
    public static void relayNonDomEvent(String name, JavaScriptObject obj, JavaScriptObject eventArg){
    	JavaScriptObject event = (eventArg == null)? getWindowEvent(): eventArg;
    	Controller.relayNonDomEvent(name, obj, event);
    }
    
    public static native JavaScriptObject getWindowEvent() /*-{
		return $wnd.event;
	}-*/;
    
    private void registerEventHandlers(Controller controller) throws XPathException {
    	// add an event listener to capture registered event modes
    	if (registeredEventModes != null) {
    		return;
    	}
        Element docElement = (com.google.gwt.user.client.Element)(Object)Document.get();
        registeredEventModes = controller.getRuleManager().getModesInNamespace(NamespaceConstant.IXSL);
        // Restriction: only one event listener per element
        if (registeredEventModes.size() > 0 && !registeredForEvents) {
        	registeredForEvents = true;
        	registerNonDOMevents(controller);
	        if (DOM.getEventListener((com.google.gwt.user.client.Element)docElement) == null) {
	        	principleEventListener = true;
	            DOM.setEventListener((com.google.gwt.user.client.Element) docElement, new EventListener() {
	                public void onBrowserEvent(Event event) {
	
	                    EventTarget eTarget = event.getEventTarget();
	                    Node eventNode;
	                    if (Node.is(eTarget)) {
	                    	eventNode = Node.as(eTarget);                   
	                    } else {
	                    	eventNode = Node.as(getCorrespondingSVGElement(eTarget));
	                        if (eventNode == null) {
	                        	return;
	                        }
	                    }
	                    bubbleApplyTemplates(eventNode, event);
	                }
	            });
	        } else {
	        	// can't register for event so register for relayEvent
	        	Controller.addEventProcessor(this);
	        }
        }
        // Events for all processor instances may register 1 or more event types
        for (Mode eventMode : registeredEventModes) {
        	String eventName = eventMode.getModeName().getLocalName();
        	if (!eventName.startsWith("on")) {
        		logger.warning("Event name: '" + eventName + "' is invalid - names should begin with 'on'");
        	} else {
        		eventName = eventName.substring(2);
        	}
            int eventNo = Event.getTypeInt(eventName);
            DOM.sinkEvents((com.google.gwt.user.client.Element)docElement, 
            		eventNo | DOM.getEventsSunk((com.google.gwt.user.client.Element) docElement));

        }

    }
    
    public void deregisterEventHandlers() {
    	// Don't deregister because this will affect any other instances of the processor
//    	Element docElement = (com.google.gwt.user.client.Element)(Object)Document.get();    	
//    	DOM.sinkEvents((com.google.gwt.user.client.Element)docElement, 0);
//    	registeredEventModes = null;
    }
    
    /**
     * Returns the SVG DOM element associated with an external element that's an SVGElementInstance object
     * E.g. If a <code>use</code> element references a <code>rect</code> element - using xlink:href - then,
     * if the <code>rect</code> object is passed as a parameter, the <code>use</code> element is returned.
     */
    public native JavaScriptObject getCorrespondingSVGElement(JavaScriptObject obj) /*-{
    	if (obj.correspondingElement) {
    		return obj.correspondingElement;
    	}
    	return null;
    }-*/;
    
   
    /**
     * This invokes a transform, but it may be called either on an async callback or directly
     * We need to ensure this method runs once and only once for a single transform request -
     * It's possible for either the main code branch which performs the compile or the async callback
     * which depends on the compile to makes the call first.
     */
    private Node invokeTransform(NodeInfo inDoc, com.google.gwt.dom.client.Node target) {
        // in case stylesheet not ready but doc has been fetched:
    	if (fetchedSourceDoc == null) {
    		fetchedSourceDoc = inDoc;
    	}
    	// check to ensure conditions required for a transform have been met
    	if (transformInvoked || stylesheet == null || 
    			(docFetchRequired && fetchedSourceDoc == null)) {
    		return null;
    	}
    	transformInvoked = true;
    	
    	try {
            final Controller controller = stylesheet.newTransformer();
            localController.setSourceNode(fetchedSourceDoc);
            controller.importControllerSettings(localController);
            logger.log(Level.FINE, "Commencing transform type:" + controller.getApiCommand().toString());
            Node outResult = controller.transform(fetchedSourceDoc, target);
            logger.log(Level.FINE, "Transform complete");
            localController.importResults(controller);
            registerEventHandlers(controller);
            if (successCallback != null) {
            	successOwner.invokeSuccess(successCallback);
            }
            return outResult;
    	} catch(Exception e) {
    		handleException(e, "invokeTransform");
    		return null;
    	}
    }
    
          
    public static void handleException(Exception err, String prefix) {
    	if (err instanceof JavaScriptAPIException){
    		// don't log and re-throw exceptions thrown only for the benefit of the API
    		return; 
    	}
    	String excName;
    	prefix = (prefix == null || prefix.length() == 0)? "" : " in " + prefix + ":";
    	boolean logException = true;
    	
    	if (err instanceof XPathException) {
    		excName = "XPathException";
    		XPathException xe = (XPathException)err;
    		logException = !xe.hasBeenReported();
    	} else {
    		excName = "Exception " + err.getClass().getName();
    	}
    	String message = excName + prefix + " " + err.getMessage();
    	if (logException) {
    		logger.log(Level.SEVERE, message);
    	}
    	err.printStackTrace();
    	if (SaxonceApi.doThrowJsExceptions())
    	{
    		throw new JavaScriptAPIException("[js] " + message); 
    	}
    }
    
    // emulate bubbling up events by iterating through ancestors that have matching rule, starting with the
    // targetNode
    
    private Mode getModeFromEvent(Event event) {
    	Mode result = null;
    	String mode = "on" + event.getType(); // eg. onclick
    	for (Mode m: registeredEventModes){
    		if (m.getModeName().getLocalName().equals(mode)) {
    			result = m;
    			break;
    		}
    	}
    	return result;
    }
    public void bubbleApplyTemplates(Node node, Event event)  {
    	if (principleEventListener) {
    		Controller.relayEvent(node, event); // make a call to this method for other instances
    	}
    	NodeInfo eventNode = ((HTMLDocumentWrapper)config.getHostPage()).wrap(node);
    	SequenceIterator bubbleElements = eventNode.iterateAxis(Axis.ANCESTOR, NodeKindTest.ELEMENT);
    	Controller controller = stylesheet.newTransformer();
    	try {
	        controller.importControllerSettings(localController);
	    	XPathContext ruleContext = controller.newXPathContext();
	    	Mode matchedMode = getModeFromEvent(event);
	    	
	    	if (matchedMode == null) {
	    		return;
	    	}
	    	
	    	// walk up the tree until we find an element with matching rule for the event mode

    		NodeInfo element = eventNode;
	        while (element != null) {
	            Rule matchedRule = matchedMode.getRule(element, ruleContext);
	            if (matchedRule != null && eventPropertyMatch(event, matchedRule)) {
	            	logger.log(Level.FINER, "Bubble Apply-Templates - Mode: " + matchedMode.getModeName().getLocalName() + 
	            			" Element: " + element.getDisplayName());
	            	applyEventTemplates(matchedMode.getModeName().getClarkName(), element, event, null);
	            	if (matchedRule.getIxslPreventDefault()) {
            			event.preventDefault();
	            	}
	            	break;
	            }
	            element = (NodeInfo)bubbleElements.next();
	        }
    	} catch (Exception e) {
    		handleException(e, "bubbleApplyTemplates");
    	}
    }
    
    private static boolean eventPropertyMatch(Event event, Rule matchedRule){
    	String eventProperty = matchedRule.getEventProperty();
    	if (eventProperty == null){
    		return true;
    	}
    	String[] all = eventProperty.split("\\s");
    	String eventPropertyValue = getEventProperty(event, all[0]);
    	if (all.length < 2 || eventPropertyValue == null){   		
    		return true;
    	}
    	boolean matches = false;

    	for (int i = 1; i < all.length; i++){
    		if (eventPropertyValue.equals(all[i]) ){
    			matches = true;
    			break;
    		}
    	}
    	return matches;
    }
      
    private static native String getEventProperty(Event evt, String propName) /*-{
		var prop = evt[propName];
		if (prop != null) {
			return String(prop);
		}
		return null;
    }-*/;
       
    // called by bubbleApplyTemplates which is the registered event handler
    // at the document node level for all specified ixsl modes      
    public void applyEventTemplates(String mode, NodeInfo start, JavaScriptObject event, JavaScriptObject object) {
        try {
        	// for case where this is a non-user event - to prevent error
        	if (start == null) {
        		start = config.getHostPage();
        	}
        	logger.log(Level.FINER, "OnEvent Apply-Templates - Mode: " + mode + " Event: " + event.toString());
            Controller controller = stylesheet.newTransformer();

            controller.importControllerSettings(localController);
            // override any imported initial mode with that for the event
            controller.setInitialTemplate(null);
            controller.setInitialMode(mode); 
            controller.setUserData("Saxon-CE", "current-event", event);
            controller.setUserData("Saxon-CE", "current-object", object);

            controller.transform(start, controller.getTargetNode());
        } catch (Exception err) {
        	handleException(err, "mode: '" + mode +"' event: '" + event.toString());
        }
    }
    
    public Controller getController() {
    	return localController;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


