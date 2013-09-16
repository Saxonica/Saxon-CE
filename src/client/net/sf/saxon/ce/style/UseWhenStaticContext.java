package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.Container;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.functions.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.NamespaceResolver;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.sxpath.AbstractStaticContext;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.AtomicType;

/**
 * This class implements the static context used for evaluating use-when expressions in XSLT 2.0
 * A new instance of this class is created for each use-when expression encountered; there are
 * therefore no issues with reusability. The class provides a Container for the expression as well
 * as the static context information; the Executable contains the single XPath expression only, and
 * is created for the purpose.
 */

public class UseWhenStaticContext extends AbstractStaticContext implements StaticContext, Container {

    private NamespaceResolver namespaceContext;
    private FunctionLibrary functionLibrary;
    private Executable executable;
    private SourceLocator sourceLocator;

    /**
     * Create a static context for evaluating use-when expressions
     * @param config the Saxon configuration
     * @param namespaceContext the namespace context in which the use-when expression appears
     */

    public UseWhenStaticContext(Configuration config, NamespaceResolver namespaceContext, SourceLocator sourceLocator) {
        setConfiguration(config);
        this.namespaceContext = namespaceContext;
        this.sourceLocator = sourceLocator;

        FunctionLibraryList lib = new FunctionLibraryList();
        lib.addFunctionLibrary(SystemFunctionLibrary.getSystemFunctionLibrary(
                StandardFunction.CORE|StandardFunction.USE_WHEN));
        lib.addFunctionLibrary(ConstructorFunctionLibrary.getInstance());
        functionLibrary = lib;
        executable = new Executable(config);
    }

    /**
     * Get the Executable representing the containing XSLT stylesheet
     * @return the Executable
     */

    public Executable getExecutable() {
        return executable;
    }

    public SourceLocator getSourceLocator() {
        return sourceLocator;
    }

    /**
     * Get the System ID of the container of the expression. This is the containing
     * entity (file) and is therefore useful for diagnostics. Use getBaseURI() to get
     * the base URI, which may be different.
     */

    public String getSystemId() {
        return getBaseURI();
    }

    /**
     * Get the granularity of the container.
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 1;
    }

    /**
     * Bind a variable used in this element to the XSLVariable element in which it is declared
     * @param qName the name of the variable
     */

    public Expression bindVariable(StructuredQName qName) throws XPathException {
        XPathException err = new XPathException("Variables cannot be used in a use-when expression");
        err.setErrorCode("XPST0008");
        err.setIsStaticError(true);
        throw err;
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Get a named collation.
     *
     * @param name The name of the required collation. Supply null to get the default collation.
     * @return the collation; or null if the required collation is not found.
     */

    public StringCollator getCollation(String name) {
        return null;
    }

    /**
     * Get the name of the default collation.
     *
     * @return the name of the default collation; or the name of the codepoint collation
     *         if no default collation has been defined
     */

    public String getDefaultCollationName() {
        return NamespaceConstant.CODEPOINT_COLLATION_URI;
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
        return false;
    }

    /**
     * Determine whether a built-in type is available in this context. This method caters for differences
     * between host languages as to which set of types are built in.
     *
     * @param type the supposedly built-in type. This will always be a type in the
     *                    XS or XDT namespace.
     * @return true if this type can be used in this static context
     */

    public boolean isAllowedBuiltInType(AtomicType type) {
        return true;
    }

    /**
     * Get a namespace resolver to resolve the namespaces declared in this static context.
     *
     * @return a namespace resolver.
     */

    public NamespaceResolver getNamespaceResolver() {
        return namespaceContext;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.