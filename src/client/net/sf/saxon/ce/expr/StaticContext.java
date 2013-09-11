package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.functions.FunctionLibrary;
import client.net.sf.saxon.ce.om.NamespaceResolver;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* A StaticContext contains the information needed while an expression or pattern
* is being parsed. The information is also sometimes needed at run-time.
*/

public interface StaticContext {

    /**
     * Get the system configuration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration();


    /**
     * Construct a dynamic context for early evaluation of constant subexpressions.
     * @return a newly constructed dynamic context
     */

    public XPathContext makeEarlyEvaluationContext();

    /**
     * Get the System ID of the container of the expression. This is the containing
     * entity (file) and is therefore useful for diagnostics. Use getBaseURI() to get
     * the base URI, which may be different.
     * @return the system ID
     */

    public String getSystemId();

    /**
     * Get the Base URI of the stylesheet element, for resolving any relative URI's used
     * in the expression.
     * Used by the document(), doc(), resolve-uri(), and base-uri() functions.
     * May return null if the base URI is not known.
     * @return the static base URI, or null if not known
    */

    public String getBaseURI();

    /**
     * Bind a variable used in this element to the XSLVariable element in which it is declared
     * @param qName The name of the variable
     * @return an expression representing the variable reference, This will often be
     * a {@link VariableReference}, suitably initialized to refer to the corresponding variable declaration,
     * but in general it can be any expression.
    */

    public Expression bindVariable(StructuredQName qName) throws XPathException;

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     * @return the function library
     */

    public FunctionLibrary getFunctionLibrary();

    /**
    * Get the name of the default collation.
    * @return the name of the default collation; or the name of the codepoint collation
    * if no default collation has been defined
    */

    public String getDefaultCollationName();

    /**
     * Get the default XPath namespace for elements and types
     * @return the default namespace, or NamespaceConstant.NULL for the non-namespace
     */

    public String getDefaultElementNamespace();

    /**
     * Get the default function namespace
     * @return the default namespace for function names
     */

    public String getDefaultFunctionNamespace();

    /**
     * Determine whether backwards compatibility mode is used
     * @return true if 1.0 compaibility mode is in force.
    */

    public boolean isInBackwardsCompatibleMode();

    /**
     * Get a namespace resolver to resolve the namespaces declared in this static context.
     * @return a namespace resolver.
     */

    public NamespaceResolver getNamespaceResolver();


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
