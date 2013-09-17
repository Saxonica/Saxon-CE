package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.functions.FunctionLibrary;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* An ExpressionContext represents the context for an XPath expression written
* in the stylesheet.
*/

public class ExpressionContext implements StaticContext {

	private StyleElement element;

    /**
     * Create a static context for XPath expressions in an XSLT stylesheet
     * @param styleElement the element on which the XPath expression appears
     */

    public ExpressionContext(StyleElement styleElement) {
		element = styleElement;
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
     * Get a copy of the NamespaceResolver suitable for saving in the executable code
     * @return a NamespaceResolver
    */


    public NamespaceResolver getNamespaceResolver() {
        return new InscopeNamespaceResolver(element);
    }

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
