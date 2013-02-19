package client.net.sf.saxon.ce;

import java.lang.annotation.ElementType;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportJsInitMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

import client.net.sf.saxon.ce.Controller.APIcommand;
import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.JsArrayIterator;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;

@Export(value="XSLT20Processor")
@ExportPackage("Saxonce")
/**
 * JavaScript API for the XSLTProcessor - mirrors the standard
 * XSLTProcessor specification for JavaScript - gwt-exporter is
 * used to convert this class to JavaScript API
 *  - uses details from the mozilla documentation at:
 * https://developer.mozilla.org/en/Using_the_Mozilla_JavaScript_interface_to_XSL_Transformations
 * 
 * This class provides a capability to perform a Saxon-CE transform on an XML/HTML DOM document
 * For HTML, the passed the exisitng DOM document is modified - for XML, a new XML DOM object is
 * created
 */
public class XSLT20Processor implements Exportable {
	
	Xslt20ProcessorImpl processor;
	Controller controller;


	public XSLT20Processor(JavaScriptObject stylesheet) {
		this(); // call constructor
		if (stylesheet != null) {
			processor.importStylesheet(stylesheet);
		}
	}
	
    private JavaScriptObject jsThis = null;
	
    /**
     * Keep instance of the JavaScript wrapped version - to
     * be used when making a callback
     */
	public void setThis(JavaScriptObject jsObj) {
		jsThis = jsObj;
	}
	
	public XSLT20Processor() {
		processor = new Xslt20ProcessorImpl();
		// get an 'inert' controller instance - used only for getting/setting controller properties
		controller = processor.getController();
		SaxonceApi.setProcessorWasJsInitiated();
	}

	/**
	 * Erases any parameter values set using setParameter method
	 */
	public void clearParameters() {
		controller.clearParameters();
	}
	
	/**
	 * Returns the value of the parameter as a String. 
	 * @param namespaceURI the parameter namespace
	 * @param localName the local name of the parameter
	 * @return the string value of the parameter
	 */
	public Object getParameter(String namespaceURI, String localName) {
		String ns = (namespaceURI == null)? "" : namespaceURI;
		try {
			
			StructuredQName qn = new StructuredQName("", ns, localName);
			ValueRepresentation vr = controller.getParameter(qn);
			return IXSLFunction.convertToJavaScript(vr);
		} catch(Exception e) {
			Xslt20ProcessorImpl.handleException(e, "getParameter");
		}
		return null;
	}
	
	/**
	 * 	
	 * @param stylesheet This may be either a document node or an xsl:transform or
	 * xsl:stylesheet element. If a document node then the transform can be a literal
	 * element transform or a full transform
	 */	
	// what happens if the imported node is modified, do we use a
	// cached stylesheet?
	public void importStylesheet(JavaScriptObject stylesheet) {	
			processor.importStylesheet(stylesheet);
	}
	
	public void setSuccess(JavaScriptObject success) {
		processor.setSuccess(success, this);
	}
	
	public JavaScriptObject getSuccess() {
		return processor.getSuccess();
	}
	
	public void invokeSuccess(JavaScriptObject callback) {
		executeSuccessCallback(callback, jsThis);
	}
	
    // sets the window object as the owner object
    public native void executeSuccessCallback(JavaScriptObject callback, JavaScriptObject proc) /*-{
         callback.call($wnd, proc);
    }-*/;
	
	/**
	 * Deletes a parameter value set using the setParameter method
	 * @param namespaceURI the parameter namespace
	 * @param localName the local name of the parameter
	 */
	public void removeParameter(String namespaceURI, String localName) {
		String ns = (namespaceURI == null)? "" : namespaceURI;
		try {
			
			StructuredQName qn = new StructuredQName("", ns, localName);
            controller.removeParameter(qn);
		} catch(Exception e) {
			Xslt20ProcessorImpl.handleException(e, "getParameter");
		}
	}
	
	/**
	 * Restore the XSLTProcessor20 instance to its default state
	 */
	public void reset() {
		controller.reset();
		processor.deregisterEventHandlers();
	}
	
	/**
	 * Set the value of a parameter - the value must be convertible to an xdmValue type
	 * @param namespaceURI the namespace of the parameter
	 * @param localName the local name of the parameter
	 * @param value the value, as a boolean/string/number or DOM node sequence to assign to the parameter
	 * @return
	 */
	// extension required to set any sequence of items (xdmValue) as a parameter as per XSLT2.0 spec
	// but emphasis initially on string, boolean and integer	
	public void setParameter(String namespaceURI, String localName, Object value) {

		String ns = (namespaceURI == null)? "" : namespaceURI;

		try {
			StructuredQName qn = new StructuredQName("", ns, localName);
			controller.setParameter(qn, getValue(value));
		} catch (Exception e) {
			Xslt20ProcessorImpl.handleException(e, "setParameter");
		}
	}
	
	private ValueRepresentation getValue(Object jso) throws XPathException {
			SequenceIterator iterator = IXSLFunction.convertFromJavaScript(jso, processor.config);
			if (iterator instanceof JsArrayIterator) {
				return (ValueRepresentation)iterator;
			} else {
				return iterator.next();
			}
	}
	

    /**
     * updates the HTML document object passed as the target - note that any event handling
     * target the host HTML page's DOM, which may not be the target document
     * 
     * @param sourceObject the XML source document - may only be null if an initialTemplate
     *        name has been set
     * @param targetDocument the target HTML or XHTML document object -straight XML is not
     *        supported because updates are either via result-document HTML references or,
     *        for the principle output, direct to the HTML body element
     */
	// for XSLT 2.0 sourceNode may be null
	public void updateHTMLDocument(JavaScriptObject sourceObject, Document targetDocument) {
		processor.updateHTMLDocument(sourceObject, targetDocument, APIcommand.UPDATE_HTML);
	}
	
	public void transformToHTMLFragment(JavaScriptObject sourceObject, Document targetDocument) {
		processor.updateHTMLDocument(sourceObject, targetDocument, APIcommand.TRANSFORM_TO_HTML_FRAGMENT);
	}
	
	/**
	 * 	
	 * @param sourceNode The source node to transform - may be null provided that initial
	 * template was set using setTemplate() - this is not standard XsltProcessor behaviour
	 * because XSLT 1.0 does not support the setting of the initial template
	 * @return The return type is either XMLDocument, HTMLDocument or XMLDocument with a
	 * single root element containing a text node depending on whether xsl:output is set
	 * to be XML, HTML or Text respectively
	 */		
	// for XSLT 2.0 sourceNode may be null
	public Document transformToDocument(JavaScriptObject sourceNode) {
		return (Document)processor.transformToDocument(sourceNode);
	}
	
	/**
	 * Returns a DocumentFragment object. Will only produce HTML DOM objects if the owner document passed
	 * as an argument is an HTMLDocument. Any result documents output from this method will also be
	 * DocumentFragment objects
	 * @sourceNode A Document object or a Saxonce 'placeholder' object - used for async operations, or
	 * null if initialTemplate is set
	 * @ownerDocument Sets The owner document for all DocumentFragments returned
	 * @return return a DOM DocumentFragment node with the owner document the same as that supplied
	 * as a parameter
	 */
	// for XSLT 2.0 sourceNode may be null
	public Node transformToFragment(JavaScriptObject sourceNode, Document  ownerDocument) {
		return processor.transformToFragment(sourceNode, ownerDocument);
	}
	
	/*
	 * Extensions for XSLT 2.0
	 */
	
	public String getInitialMode() {
		return controller.getInitialMode();
	}
	
	public String getInitialTemplate() {
		return controller.getInitialTemplate();
	}
	
	public void setInitialMode(String mode) {
		try {
		controller.setInitialMode(mode);
 		} catch (Exception e) {
			Xslt20ProcessorImpl.handleException(e, "setInitialMode");
		}
	}
	
	public void setInitialTemplate(String template) {
		try {
		controller.setInitialTemplate(template);
 		} catch (Exception e) {
			Xslt20ProcessorImpl.handleException(e, "setInitialTemplate");
		}
	}
	
	
	public void setBaseOutputURI(String URI) {
		controller.setBaseOutputURI(URI);
	}
	
	/**
	 * Return result-documents as a JS map of URI/dom name/value pairs
	 * Note that the base-output-uri setting is use to resolve relative uris
	 * @return
	 */
	public JavaScriptObject getResultDocuments() {
		return controller.getResultDocURIArray();
	}
	
	public JavaScriptObject getResultDocument(String URI) {
		return controller.getResultDocument(URI);
	}
		
	public void setCollation() {
		// TODO: this could also be tried out using Saxon feature keys
	}

}
