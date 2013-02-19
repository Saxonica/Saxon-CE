package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.functions.FunctionLibrary;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.DecimalFormatManager;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;


/**
* An ExpressionContext represents the context for an XPath expression written
* in the stylesheet.
*/

public class ExpressionContext implements StaticContext {

	private StyleElement element;
	private NamePool namePool;
    private NamespaceResolver namespaceResolver = null;

    /**
     * Create a static context for XPath expressions in an XSLT stylesheet
     * @param styleElement the element on which the XPath expression appears
     */

    public ExpressionContext(StyleElement styleElement) {
		element = styleElement;
		namePool = styleElement.getNamePool();
	}

    /**
     * Get the system configuration
     */

    public Configuration getConfiguration() {
        return element.getConfiguration();
    }

    /**
     * Get the executable
     * @return the executable
     */

    public Executable getExecutable() {
        return element.getExecutable();
    }

    /**
     * Construct a dynamic context for early evaluation of constant subexpressions
     */

    public XPathContext makeEarlyEvaluationContext() {
        return new EarlyEvaluationContext(getConfiguration());
    }

    /**
    * Issue a compile-time warning
    */

    public void issueWarning(String s, SourceLocator locator) {
        element.issueWarning(s, locator);
    }

    /**
    * Get the NamePool used for compiling expressions
    */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
    * Get the System ID of the entity containing the expression (used for diagnostics)
    */

    public String getSystemId() {
    	return element.getSystemId();
    }

    /**
    * Get the Base URI of the element containing the expression, for resolving any
    * relative URI's used in the expression.
    * Used by the document() function.
    */

    public String getBaseURI() {
        return element.getBaseURI();
    }

    /**
    * Get the URI for a prefix, using this Element as the context for namespace resolution.
    * The default namespace will not be used when the prefix is empty.
    * @param prefix The prefix
    * @throws XPathException if the prefix is not declared
    */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = element.getURIForPrefix(prefix, false);
        if (uri == null) {
            XPathException err = new XPathException("Undeclared namespace prefix " + Err.wrap(prefix));
            err.setErrorCode("XPST0081");
            err.setIsStaticError(true);
            throw err;
        }
        return uri;
    }

    /**
     * Get a copy of the NamespaceResolver suitable for saving in the executable code
     * @return a NamespaceResolver
    */


    public NamespaceResolver getNamespaceResolver() {
        return element;
    }

    /**
     * Get a DecimalFormatManager to resolve the names of decimal formats used in calls
     * to the format-number() function.
     * @return the decimal format manager for this static context, or null if named decimal
     *         formats are not supported in this environment.
     */

    public DecimalFormatManager getDecimalFormatManager() {
        return element.getPreparedStylesheet().getDecimalFormatManager();
    }

    /**
    * Get a fingerprint for a name, using this as the context for namespace resolution
    * @param qname The name as written, in the form "[prefix:]localname"
    * @param useDefault Defines the action when there is no prefix. If true, use
    * the default namespace URI (as for element names). If false, use no namespace URI
    * (as for attribute names).
    * @return -1 if the name is not already present in the name pool
    */

    public int getFingerprint(String qname, boolean useDefault) throws XPathException {

        String[] parts;
        try {
            parts = NameChecker.getQNameParts(qname);
        } catch (QNameException err) {
            throw new XPathException(err.getMessage());
        }
        String prefix = parts[0];
        if (prefix.length() == 0) {
            String uri = "";

            if (useDefault) {
                uri = getURIForPrefix(prefix);
            }

			return namePool.getFingerprint(uri, qname);

        } else {

            String uri = getURIForPrefix(prefix);
			return namePool.getFingerprint(uri, parts[1]);
        }
    }

    /**
    * Get a StructuredQName for a name, using this as the context for namespace resolution
    * @param qname The name as written, in the form "[prefix:]localname"
    * @param useDefault Defines the action when there is no prefix. If true, use
    * the default namespace URI (as for element names). If false, use no namespace URI
    * (as for attribute names).
    * @return -1 if the name is not already present in the name pool
    */

    public StructuredQName getStructuredQName(String qname, boolean useDefault) throws XPathException {

        String[] parts;
        try {
            parts = NameChecker.getQNameParts(qname);
        } catch (QNameException err) {
            throw new XPathException(err.getMessage());
        }
        String prefix = parts[0];
        if (prefix.length() == 0) {
            String uri = "";

            if (useDefault) {
                uri = getURIForPrefix(prefix);
            }

			return new StructuredQName("", uri, qname);

        } else {

            String uri = getURIForPrefix(prefix);
			return new StructuredQName(prefix, uri, parts[1]);
        }
    }

    private static StructuredQName[] errorVariables = {
            new StructuredQName("err", NamespaceConstant.ERR, "code"),
            new StructuredQName("err", NamespaceConstant.ERR, "description"),
            new StructuredQName("err", NamespaceConstant.ERR, "value"),
            new StructuredQName("err", NamespaceConstant.ERR, "module"),
            new StructuredQName("err", NamespaceConstant.ERR, "line-number"),
            new StructuredQName("err", NamespaceConstant.ERR, "column-number")
    };

    /**
     * Bind a variable to an object that can be used to refer to it
     * @param qName the name of the variable
     * @return a VariableDeclaration object that can be used to identify it in the Bindery,
     * @throws XPathException if the variable has not been declared
    */

    public Expression bindVariable(StructuredQName qName) throws XPathException {
        XSLVariableDeclaration xslVariableDeclaration = element.bindVariable(qName);
        if (xslVariableDeclaration == null) {
            XPathException err = new XPathException("Variable " + qName.getDisplayName() +
                    " has not been declared");
            err.setErrorCode("XPST0008");
            err.setIsStaticError(true);
            throw err;
        }
        VariableReference var = (xslVariableDeclaration.isGlobal()
                                    ? new VariableReference()
                                    : new LocalVariableReference());
        xslVariableDeclaration.registerReference(var);
        return var;
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     */

    public FunctionLibrary getFunctionLibrary() {
        return element.getPrincipalStylesheetModule().getFunctionLibrary();
    }

    /**
    * Determine if an extension element is available
    * @throws XPathException if the name is invalid or the prefix is not declared
    */

    public boolean isElementAvailable(String qname) throws XPathException {
        try {
            String[] parts = NameChecker.getQNameParts(qname);
            String uri;
            if (parts[0].length() == 0) {
                uri = getDefaultElementNamespace();
            } else {
                uri = getURIForPrefix(parts[0]);
            }
            return element.getPreparedStylesheet().getStyleNodeFactory().isElementAvailable(uri, parts[1]);
        } catch (QNameException e) {
            XPathException err = new XPathException("Invalid element name. " + e.getMessage());
            err.setErrorCode("XTDE1440");
            throw err;
        }
    }

    /**
    * Get the default collation. Return null if no default collation has been defined
    */

    public String getDefaultCollationName() {
        return element.getDefaultCollationName();
    }

    /**
     * Get the default XPath namespace for elements and types
     * Return NamespaceConstant.NULL for the non-namespace
    */

    public String getDefaultElementNamespace() {
        return element.getDefaultXPathNamespace();
    }

    /**
     * Get the default function namespace
     */

    public String getDefaultFunctionNamespace() {
        return NamespaceConstant.FN;
    }

    /**
    * Determine whether Backwards Compatible Mode is used
    */

    public boolean isInBackwardsCompatibleMode() {
        return element.xPath10ModeIsEnabled();
    }

    /**
     * Get the containing element in the stylesheet
     * @return the stylesheet element
     */

    public StyleElement getStyleElement() {
        return element;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
