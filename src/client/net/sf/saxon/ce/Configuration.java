package client.net.sf.saxon.ce;

import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper.DocType;
import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.expr.EarlyEvaluationContext;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.number.Numberer_en;
import client.net.sf.saxon.ce.expr.sort.CaseInsensitiveCollator;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.lib.*;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.DocumentPool;
import client.net.sf.saxon.ce.trans.CompilerInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.DocumentNumberAllocator;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Window;

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

    private boolean timing = false;
    private boolean allowExternalFunctions = true;
    private DocumentNumberAllocator documentNumberAllocator = new DocumentNumberAllocator();
    private DocumentPool globalDocumentPool = new DocumentPool();
    private transient XPathContext conversionContext = null;
    private transient TypeHierarchy typeHierarchy;

    private ParseOptions defaultParseOptions = new ParseOptions();

    private CompilerInfo defaultXsltCompilerInfo = new CompilerInfo();
    private DocumentPool sourceDocumentPool = new DocumentPool();
    private Logger logger = Logger.getLogger("Configuration");


    /**
     * Constant indicating that the processor should take the recovery action
     * when a recoverable error occurs, with no warning message.
     */
    public static final int RECOVER_SILENTLY = 0;
    /**
     * Constant indicating that the processor should produce a warning
     * when a recoverable error occurs, and should then take the recovery
     * action and continue.
     */
    public static final int RECOVER_WITH_WARNINGS = 1;
    /**
     * Constant indicating that when a recoverable error occurs, the
     * processor should not attempt to take the defined recovery action,
     * but should terminate with an error.
     */
    public static final int DO_NOT_RECOVER = 2;

    /**
     * Constant indicating the XML Version 1.0
     */

    public static final int XML10 = 10;

    /**
     * Constant indicating the XML Version 1.1
     */

    public static final int XML11 = 11;

    /**
     * Constant indicating that the host language is XSLT
     */
    public static final int XSLT = 50;

    /**
     * Constant indicating that the host language is XPATH itself - that is, a free-standing XPath environment
     */
    public static final int XPATH = 54;

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

    public DocumentInfo getHostPage() {
        // attempt to initialise this only once - in the Configuration constructor led
        // to NamePool exception
        Document page = Document.get();
        return new HTMLDocumentWrapper(page, page.getURL(), this, DocType.UNKNOWN);
    }

    /**
     * Get an XPathContext object with sufficient capability to perform comparisons and conversions
     *
     * @return a dynamic context for performing conversions
     */

    public XPathContext getConversionContext() {
        if (conversionContext == null) {
            conversionContext = new EarlyEvaluationContext(this);
        }
        return conversionContext;
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

    /**
     * Load a Numberer class for a given language and check it is OK.
     * This method is provided primarily for internal use.
     *
     * @param language the language for which a Numberer is required. May be null,
     *                 indicating default language
     * @param country  the country for which a Numberer is required. May be null,
     *                 indicating default country
     * @return a suitable numberer. If no specific numberer is available
     *         for the language, the default numberer (normally English) is used.
     */

    public Numberer makeNumberer(String language, String country) {
        return new Numberer_en();
    }


    /**
     * Get the default options for XSLT compilation
     *
     * @return the default options for XSLT compilation. The CompilerInfo object will reflect any options
     *         set using other methods available for this Configuration object
     */

    public CompilerInfo getDefaultXsltCompilerInfo() {
        return defaultXsltCompilerInfo;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err.
     * <p/>
     * This method is provided largely for internal use. Progress messages are normally
     * controlled directly from the command line interfaces, and are not normally used when
     * driving Saxon from the Java API.
     *
     * @return true if these messages are to be output.
     */

    public boolean isTiming() {
        return timing;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err.
     * <p/>
     * This method is provided largely for internal use. Progress messages are normally
     * controlled directly from the command line interfaces, and are not normally used when
     *
     * @param timing true if these messages are to be output.
     */

    public void setTiming(boolean timing) {
        this.timing = timing;
    }

    /**
     * Determine whether a warning is to be output when running against a stylesheet labelled
     * as version="1.0". The XSLT specification requires such a warning unless the user disables it.
     *
     * @return true if these messages are to be output.
     * @since 8.4
     */

    public boolean isVersionWarning() {
        return defaultXsltCompilerInfo.isVersionWarning();
    }

    /**
     * Determine whether a warning is to be output when the version attribute of the stylesheet does
     * not match the XSLT processor version. (In the case where the stylesheet version is "1.0",
     * the XSLT specification requires such a warning unless the user disables it.)
     *
     * @param warn true if these warning messages are to be output.
     * @since 8.4
     */

    public void setVersionWarning(boolean warn) {
        defaultXsltCompilerInfo.setVersionWarning(warn);
    }

    /**
     * Determine whether calls to external Java functions are permitted.
     *
     * @return true if such calls are permitted.
     * @since 8.4
     */

    public boolean isAllowExternalFunctions() {
        return allowExternalFunctions;
    }

    /**
     * Determine whether calls to external Java functions are permitted. Allowing
     * external function calls is potentially a security risk if the stylesheet or
     * Query is untrusted, as it allows arbitrary Java methods to be invoked, which can
     * examine or modify the contents of filestore and other resources on the machine
     * where the query/stylesheet is executed.
     * <p/>
     * <p>Setting the value to false disallows all of the following:</p>
     * <p/>
     * <ul>
     * <li>Calls to Java extension functions</li>
     * <li>Use of the XSLT system-property() function to access Java system properties</li>
     * <li>Use of a relative URI in the <code>xsl:result-document</code> instruction</li>
     * <li>Calls to XSLT extension instructions</li>
     * </ul>
     * <p/>
     * <p>Note that this option does not disable use of the <code>doc()</code> function or similar
     * functions to access the filestore of the machine where the transformation or query is running.
     * That should be done using a user-supplied <code>URIResolver</code></p>
     *
     * @param allowExternalFunctions true if external function calls are to be
     *                               permitted.
     * @since 8.4
     */

    public void setAllowExternalFunctions(boolean allowExternalFunctions) {
        this.allowExternalFunctions = allowExternalFunctions;
    }


    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * validation of source documents
     *
     * @return true if DTD validation is requested.
     * @since 8.4
     */

    public boolean isValidation() {
        return defaultParseOptions.getDTDValidationMode() == Validation.STRICT ||
                defaultParseOptions.getDTDValidationMode() == Validation.LAX;
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
     * Determine whether the XML parser for source documents will be asked to perform
     * DTD validation of source documents
     *
     * @param validation true if DTD validation is to be requested.
     * @since 8.4
     */

    public void setValidation(boolean validation) {
        defaultParseOptions.setDTDValidationMode(validation ? Validation.STRICT : Validation.STRIP);
    }

    /**
     * Get the TypeHierarchy: a cache holding type information
     *
     * @return the type hierarchy cache
     */

    public final TypeHierarchy getTypeHierarchy() {
        if (typeHierarchy == null) {
            typeHierarchy = new TypeHierarchy(this);
        }
        return typeHierarchy;
    }

    /**
     * Get the document number allocator.
     * <p/>
     * The document number allocator is used to allocate a unique number to each document built under this
     * configuration. The document number forms the basis of all tests for node identity; it is therefore essential
     * that when two documents are accessed in the same XPath expression, they have distinct document numbers.
     * Normally this is ensured by building them under the same Configuration. Using this method together with
     * {@link #setDocumentNumberAllocator}, however, it is possible to have two different Configurations that share
     * a single DocumentNumberAllocator
     *
     * @return the current DocumentNumberAllocator
     * @since 9.0
     */

    public DocumentNumberAllocator getDocumentNumberAllocator() {
        return documentNumberAllocator;
    }

    /**
     * Set the document number allocator.
     * <p/>
     * The document number allocator is used to allocate a unique number to each document built under this
     * configuration. The document number forms the basis of all tests for node identity; it is therefore essential
     * that when two documents are accessed in the same XPath expression, they have distinct document numbers.
     * Normally this is ensured by building them under the same Configuration. Using this method together with
     * {@link #getDocumentNumberAllocator}, however, it is possible to have two different Configurations that share
     * a single DocumentNumberAllocator</p>
     * <p>This method is for advanced applications only. Misuse of the method can cause problems with node identity.
     * The method should not be used except while initializing a Configuration, and it should be used only to
     * arrange for two different configurations to share the same DocumentNumberAllocators. In this case they
     * should also share the same NamePool.
     *
     * @param allocator the DocumentNumberAllocator to be used
     * @since 9.0
     */

    public void setDocumentNumberAllocator(DocumentNumberAllocator allocator) {
        documentNumberAllocator = allocator;
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
     * Set which kinds of whitespace-only text node should be stripped.
     *
     * @param kind the kind of whitespace-only text node that should be stripped when building
     *             a source tree. One of {@link Whitespace#NONE} (none), {@link Whitespace#ALL} (all),
     *             or {@link Whitespace#IGNORABLE} (element-content whitespace as defined in a DTD or schema)
     */

    public void setStripsWhiteSpace(int kind) {
        defaultParseOptions.setStripSpace(kind);
    }

    /**
     * Set which kinds of whitespace-only text node should be stripped.
     *
     * @return kind the kind of whitespace-only text node that should be stripped when building
     *         a source tree. One of {@link client.net.sf.saxon.ce.value.Whitespace#NONE} (none), {@link Whitespace#ALL} (all),
     *         or {@link Whitespace#IGNORABLE} (element-content whitespace as defined in a DTD or schema)
     */

    public int getStripsWhiteSpace() {
        return defaultParseOptions.getStripSpace();
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

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.