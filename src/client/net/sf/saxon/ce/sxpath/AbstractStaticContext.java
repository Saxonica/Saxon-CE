package client.net.sf.saxon.ce.sxpath;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.EarlyEvaluationContext;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.functions.FunctionLibrary;
import client.net.sf.saxon.ce.functions.FunctionLibraryList;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.trans.DecimalFormatManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.value.DecimalValue;

/**
 * An abstract and configurable implementation of the StaticContext interface,
 * which defines the static context of an XPath expression.
 *
 * <p>This class implements those parts of the functionality of a static context
 * that tend to be common to most implementations: simple-valued properties such
 * as base URI and default element namespace; availability of the standard
 * function library; and support for collations.</p>
*/

public abstract class AbstractStaticContext implements StaticContext {

    private String baseURI = null;
    private Configuration config;
    private FunctionLibraryList libraryList = new FunctionLibraryList();
    private String defaultFunctionNamespace = NamespaceConstant.FN;
    private String defaultElementNamespace = NamespaceConstant.NULL;
    private DecimalFormatManager decimalFormatManager = null;
    private boolean backwardsCompatible = false;
    private DecimalValue xpathLanguageLevel = DecimalValue.TWO;
    private boolean schemaAware = false;
    protected boolean usingDefaultFunctionLibrary;

    /**
     * Set the Configuration.
     * @param config the configuration
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the system configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Say whether this static context is schema-aware
     * @param aware true if this static context is schema-aware
     */

    public void setSchemaAware(boolean aware) {
        schemaAware = aware;
    }

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     *
     * @return the value {@link client.net.sf.saxon.ce.Configuration#XPATH}
     */

    private int getHostLanguage() {
        return Configuration.XPATH;
    }

    /**
     * Construct a dynamic context for early evaluation of constant subexpressions
     */

    public XPathContext makeEarlyEvaluationContext() {
        return new EarlyEvaluationContext(getConfiguration());
    }

    /**
     * Set the base URI in the static context
     * @param baseURI the base URI of the expression
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
    * Get the Base URI, for resolving any relative URI's used
    * in the expression. Used by the document() function, resolve-uri(), etc.
    * @return "" if no base URI has been set
    */

    public String getBaseURI() {
        return baseURI==null ? "" : baseURI;
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context. This method is called by the XPath parser when binding a function call in the
     * XPath expression to an implementation of the function.
     */

    public FunctionLibrary getFunctionLibrary() {
        return libraryList;
    }

    /**
     * Set the function library to be used
     * @param lib the function library
     */

    public void setFunctionLibrary(FunctionLibraryList lib) {
        libraryList = lib;
        usingDefaultFunctionLibrary = false;
    }

    /**
    * Get the name of the default collation.
    * @return the name of the default collation; or the name of the codepoint collation
    * if no default collation has been defined
    */

    public String getDefaultCollationName() {
        return NamespaceConstant.CODEPOINT_COLLATION_URI;
    }

    /**
    * Get the NamePool used for compiling expressions
    */

    public NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
     * Issue a compile-time warning. This method is used during XPath expression compilation to
     * output warning conditions. The default implementation writes the message to the
     * error listener registered with the Configuration.
    */

    public void issueWarning(String s, SourceLocator locator) {
        config.issueWarning(s);
    }

    /**
    * Get the system ID of the container of the expression. Used to construct error messages.
    * @return "" always
    */

    public String getSystemId() {
        return "";
    }


    /**
    * Get the line number of the expression within that container.
    * Used to construct error messages.
    * @return -1 always
    */

    private int getLineNumber() {
        return -1;
    }


    /**
     * Get the default namespace URI for elements and types
     * Return NamespaceConstant.NULL (that is, the zero-length string) for the non-namespace
     * @return the default namespace for elements and type
    */

    public String getDefaultElementNamespace() {
        return defaultElementNamespace;
    }

    /**
     * Set the default namespace for elements and types
     * @param uri the namespace to be used for unprefixed element and type names.
     * The value "" (or NamespaceConstant.NULL) represents the non-namespace
     */

    public void setDefaultElementNamespace(String uri) {
        defaultElementNamespace = uri;
    }

    /**
     * Set the default function namespace
     * @param uri the namespace to be used for unprefixed function names.
     * The value "" (or NamespaceConstant.NULL) represents the non-namespace
     */

    public void setDefaultFunctionNamespace(String uri) {
        defaultFunctionNamespace = uri;
    }

    /**
     * Get the default function namespace.
     * The value "" (or NamespaceConstant.NULL) represents the non-namespace
     * @return the default namesapce for functions
     */

    public String getDefaultFunctionNamespace() {
        return defaultFunctionNamespace;
    }

    /**
     * Set XPath 1.0 backwards compatibility mode on or off
     * @param option true if XPath 1.0 compatibility mode is to be set to true;
     * otherwise false
     */

    public void setBackwardsCompatibilityMode(boolean option) {
        backwardsCompatible = option;
    }

    /**
     * Determine whether Backwards Compatible Mode is used
     * @return true if XPath 1.0 compatibility mode is to be set to true;
     * otherwise false
     */

    public boolean isInBackwardsCompatibleMode() {
        return backwardsCompatible;
    }

    /**
     * Set the DecimalFormatManager used to resolve the names of decimal formats used in calls
     * to the format-number() function.
     * @param manager the decimal format manager for this static context, or null if no named decimal
     *         formats are available in this environment.
     */

    public void setDecimalFormatManager(DecimalFormatManager manager) {
        this.decimalFormatManager = manager;
    }

    /**
     * Get a DecimalFormatManager to resolve the names of decimal formats used in calls
     * to the format-number() function.
     * @return the decimal format manager for this static context, or null if no named decimal
     *         formats are available in this environment.
     * @since 9.2
     */

    public DecimalFormatManager getDecimalFormatManager() {
        return decimalFormatManager;
    }

    public boolean isElementAvailable(String qname) throws XPathException {
        return false;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.