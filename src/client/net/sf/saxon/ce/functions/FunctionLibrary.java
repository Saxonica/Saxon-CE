package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.Container;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * A FunctionLibrary handles the binding of function calls in XPath (or XQuery) expressions.
 * There are a number of implementations of this
 * class to handle different kinds of function: system functions, constructor functions, vendor-defined
 * functions, Java extension functions, stylesheet functions, and so on. There is also an implementation
 * {@link client.net.sf.saxon.ce.functions.FunctionLibraryList} that allows a FunctionLibrary
 * to be constructed by combining other FunctionLibrary objects.
 */

public interface FunctionLibrary {

    /**
     * Test whether a function with a given name and arity is available; if so, return its signature.
     * This supports the function-available() function in XSLT; it is also used to support
     * higher-order functions introduced in XQuery 1.1.
     *
     * <p>This method may be called either at compile time
     * or at run time. If the function library is to be used only in an XQuery or free-standing XPath
     * environment, this method may throw an UnsupportedOperationException.</p>
     * @param functionName the qualified name of the function being called
     * @param arity        The number of arguments. This is set to -1 in the case of the single-argument
     *                     function-available() function; in this case the method should return a zero-lengh array
     *                     if there is some function of this name available for calling.
     * @return if a function of this name and arity is available for calling, then true
     */

    public boolean hasFunctionSignature(StructuredQName functionName, int arity);

    /**
     * Bind a function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName the QName of the function being called
     * @param staticArgs  The expressions supplied statically in arguments to the function call.
     * The length of this array represents the arity of the function. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality()) may
     * be used as part of the binding algorithm. In some cases it may be possible for the function
     * to be pre-evaluated at compile time, for example if these expressions are all constant values.
     * <p>
     * The conventions of the XPath language demand that the results of a function depend only on the
     * values of the expressions supplied as arguments, and not on the form of those expressions. For
     * example, the result of f(4) is expected to be the same as f(2+2). The actual expression is supplied
     * here to enable the binding mechanism to select the most efficient possible implementation (including
     * compile-time pre-evaluation where appropriate).
     * @param env The static context of the function call
     * @param container The container for the newly created Expression
     * @return An expression equivalent to a call on the specified function, if one is found;
     * null if no function was found matching the required name and arity.
     * @throws client.net.sf.saxon.ce.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env, Container container)
            throws XPathException;

}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.