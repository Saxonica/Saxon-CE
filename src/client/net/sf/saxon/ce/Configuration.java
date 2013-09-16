package client.net.sf.saxon.ce;

import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper.DocType;
import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.expr.sort.CaseInsensitiveCollator;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.lib.ErrorListener;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.StandardErrorListener;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.DocumentPool;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.DateTimeValue;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Window;

import java.util.Date;
import java.util.logging.Logger;
//import com.google.gwt.xml.client.Document;
//import com.google.gwt.xml.client.XMLParser;
//import com.google.gwt.xml.client.impl.*;


/**
 * This class holds details of user-selected configuration options for a set of transformations
 * and/or queries. When running XSLT, the preferred way of setting configuration options is via
 * the JAXP TransformerFactory interface, but the Configuration object provides a finer
 * level of control. As yet there is no standard API for XQuery, so the only way of setting
 * Configuration information is to use the methods on this class directly.
 * <p/>
 * <p>As well as holding configuration settings, this class acts as a factory for classes
 * providing service in particular areas: error handling, URI resolution, and the like. Some
 * of these services are chosen on the basis of the current platform (Java or .NET), some vary
 * depending whether the environment is schema-aware or not.</p>
 * <p/>
 * <p>The <code>Configuration</code> establishes the scope within which node identity is managed.
 * Every document belongs to a <code>Configuration</code>, and every node has a distinct identity
 * within that <code>Configuration</code>. In consequence, it is not possible for any query or
 * transformation to manipulate multiple documents unless they all belong to the same
 * <code>Configuration</code>.</p>
 * <p/>
 * <p>Since Saxon 8.4, the JavaDoc documentation for Saxon attempts to identify interfaces
 * that are considered stable, and will only be changed in a backwards-incompatible way
 * if there is an overriding reason to do so. These interfaces and methods are labelled
 * with the JavaDoc "since" tag. The value 8.n indicates a method in this category that
 * was introduced in Saxon version 8.n: or in the case of 8.4, that was present in Saxon 8.4
 * and possibly in earlier releases. (In some cases, these methods have been unchanged for
 * a long time.) Methods without a "since" tag, although public, are provided for internal
 * use or for use by advanced users, and are subject to change from one release to the next.
 * The presence of a "since" tag on a class or interface indicates that there are one or more
 * methods in the class that are considered stable; it does not mean that all methods are
 * stable.
 *
 * @since 8.4
 */


public class Configuration {

    private ErrorListener errorListener = new StandardErrorListener();

    private DocumentPool globalDocumentPool = new DocumentPool();
    private int implicitTimezone = DateTimeValue.fromJavaDate(new Date()).getTimezoneInMinutes();

    private DocumentPool sourceDocumentPool = new DocumentPool();
    private Logger logger = Logger.getLogger("Configuration");

    private int nextDocumentNumber = 0;

    /**
     * Create a non-schema-aware configuration object with default settings for all options.
     *
     * @since 8.4
     */

    public Configuration() {
    }

    /**
     * Get the edition code identifying this configuration: "CE" for "client edition"
     */

    public static String getEditionCode() {
        return "CE";
    }

    /**
     * Allocate a unique document number
     * @return a unique document number
     */

    public synchronized int allocateDocumentNumber() {
        return nextDocumentNumber++;
    }



    public DocumentInfo getHostPage() {
        // attempt to initialise this only once - in the Configuration constructor led
        // to NamePool exception
        Document page = Document.get();
        return new HTMLDocumentWrapper(page, page.getURL(), this, DocType.UNKNOWN);
    }

    /**
     * Get the implicit timezone. This is fixed for the life of the Configuration. The current date/time
     * may vary for each transformation, but will always be in this timezone.
     * @return the implicit timezone as an offset in minutes
     */

    public int getImplicitTimezone() {
        return implicitTimezone;
    }

    public static URI getLocation() {
        URI location = null;
        try {
            location = new URI(Window.Location.getHref());
        } catch (Exception err) {
        }
        return location;
    }

    /**
     * Get the collation with a given collation name. If the collation name has
     * not been registered in this CollationMap, the CollationURIResolver registered
     * with the Configuration is called. If this cannot resolve the collation name,
     * it should return null.
     *
     * @param name the collation name (should be an absolute URI)
     * @return the StringCollator with this name if known, or null if not known
     */

    public StringCollator getNamedCollation(String name) {
        if (name.equals(NamespaceConstant.CODEPOINT_COLLATION_URI)) {
            return CodepointCollator.getInstance();
        } else if (name.equals(NamespaceConstant.CASE_INSENSITIVE_COLLATION_URI)) {
            return CaseInsensitiveCollator.getInstance();
        } else {
            return null;
        }
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }

    /**
     * Get the document pool. This is used only for source documents, not for stylesheet modules.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return the source document pool
     */

    public DocumentPool getDocumentPool() {
        return sourceDocumentPool;
    }

    /**
     * Get the global document pool. This is used for documents preloaded during query or stylesheet
     * compilation. The user application can preload documents into the global pool, where they will be found
     * if any query or stylesheet requests the specified document using the doc() or document() function.
     *
     * @return the global document pool
     * @since 9.1
     */

    public DocumentPool getGlobalDocumentPool() {
        return globalDocumentPool;
    }

    /**
     * Make a PipelineConfiguration from the properties of this Configuration
     *
     * @return a new PipelineConfiguration
     * @since 8.4
     */

    public PipelineConfiguration makePipelineConfiguration() {
        PipelineConfiguration pipe = new PipelineConfiguration();
        pipe.setConfiguration(this);
        return pipe;
    }

    /**
     * Issue a warning
     */

    public void issueWarning(String message) {
        logger.warning(message);
    }

    /**
     * Build a document, using specified options for parsing and building.
     *
     * @param url the URL of the document to be fetched and parsed.
     * @throws XPathException if the URL cannot be dereferenced or if parsing fails
     */
    public DocumentInfo buildDocument(final String url) throws XPathException {
        if (url.equals("html:document")) {
            // special case this URI
            return getHostPage();
        }

        String xml;
        try {
            xml = XMLDOM.makeHTTPRequest(url);
        } catch (Exception err) {
            throw new XPathException("HTTPRequest error: " + err.getMessage());
        }
        Document jsDoc;
        try {
            jsDoc = (Document) XMLDOM.parseXML(xml);
            if (jsDoc.getDocumentElement() == null) {
                throw new XPathException("null returned for " + url);
            }
        } catch (Exception ec) {
            throw new XPathException("XML parser error: " + ec.getMessage());
        }
        return new HTMLDocumentWrapper(jsDoc, url, Configuration.this, DocType.NONHTML);
    }


    public DocumentInfo wrapHTMLDocument(com.google.gwt.dom.client.Document doc, String uri) {
        return new HTMLDocumentWrapper(doc, uri, Configuration.this, DocType.UNKNOWN);
    }

    public DocumentInfo wrapXMLDocument(Node doc, String uri) {
        return new HTMLDocumentWrapper(doc, uri, Configuration.this, DocType.NONHTML);
    }

    private static int ieVersion = 0;

    /**
     * Returns -1 if host is not IE or the version number when IE is found
     */
    public static int getIeVersion() {
        if (ieVersion == 0) {
            ieVersion = getNativeIEVersion();
        }
        return ieVersion;
    }

    public static native int getNativeIEVersion() /*-{
        var rv = -1;
        if (navigator.appName == 'Microsoft Internet Explorer') {
            var ua = navigator.userAgent;
            var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
            if (re.exec(ua) != null)
                rv = parseFloat(RegExp.$1);
        }
        return rv;
    }-*/;

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
