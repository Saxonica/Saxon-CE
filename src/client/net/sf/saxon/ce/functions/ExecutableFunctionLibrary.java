package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.UserFunction;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;

import java.util.HashMap;
import java.util.Iterator;

/**
 * An ExecutableFunctionLibrary is a function library that contains definitions of functions for use at
 * run-time. Normally functions are bound at compile-time; however there are various situations in which
 * the information is needed dynamically, for example (a) to support the XSLT function-available() call
 * (in the pathological case where the argument is not known statically), (b) to allow functions to be
 * called from saxon:evaluate(), (c) to allow functions to be called from a debugging breakpoint.
 *
 * The objects actually held in the ExecutableFunctionLibrary are UserFunctionCall objects that have been
 * prepared at compile time. These are function calls that do full dynamic type checking: that is, they
 * are prepared on the basis that the static types of the arguments are all "item()*", meaning that full
 * type checking is needed at run-time.
 */

public class ExecutableFunctionLibrary implements FunctionLibrary {

    private transient Configuration config;
    private HashMap<String, UserFunction> functions = new HashMap<String, UserFunction>(20);
    // The key of the hash table is a String that combines the QName of the function with the arity.

    /**
     * Create the ExecutableFunctionLibrary
     * @param config the Saxon configuration
     */

    public ExecutableFunctionLibrary(Configuration config) {
        this.config = config;
    }

    /**
     * Make a key that will uniquely identify a function
     * @param functionName the name of the function
     * @param arity the number of arguments
     * @return the constructed key. This is of the form {uri}local/arity
     */

    private String makeKey(StructuredQName functionName, int arity) {
        String uri = functionName.getNamespaceURI();
        String local = functionName.getLocalName();
        FastStringBuffer sb = new FastStringBuffer(uri.length() + local.length() + 8);
        sb.append('{');
        sb.append(uri);
        sb.append('}');
        sb.append(local);
        sb.append("#" + arity);
        return sb.toString();
    }

    /**
     * Register a function with the function library
     * @param fn the function to be registered
     */

    public void addFunction(UserFunction fn) {
        functions.put(makeKey(fn.getFunctionName(), fn.getNumberOfArguments()), fn);
    }

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
     *                     function-available() function; in this case the method should return true if there is some
     *                     function of this name available for calling.
     * @return if a function of this name and arity is available for calling, then the type signature of the
     * function, as an array of sequence types in which the zeroth entry represents the return type; or a zero-length
     * array if the function exists but the signature is not known; or null if the function does not exist
     */

    public boolean hasFunctionSignature(StructuredQName functionName, int arity) {
        if (arity == -1) {
            for (int i=0; i<=20; i++) {
                boolean b = hasFunctionSignature(functionName, i);
                if (b) return true;
            }
            return false;
        }
        UserFunction uf = functions.get(makeKey(functionName, arity));
        return uf != null;
    }

    /**
     * Bind a function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName  The name of the function to be called
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env the static evaluation context
     * @param container
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws client.net.sf.saxon.ce.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env, Container container)
            throws XPathException {
        UserFunction fn = functions.get(makeKey(functionName, staticArgs.length));
        if (fn == null) {
            return null;
        }
        ExpressionVisitor visitor = ExpressionVisitor.make(env, fn.getExecutable());
        UserFunctionCall fc = new UserFunctionCall();
        fc.setFunctionName(functionName);
        fc.setArguments(staticArgs);
        fc.setFunction(fn);
        fc.checkFunctionCall(fn, visitor);
        fc.setStaticType(fn.getResultType(config.getTypeHierarchy()));
        return fc;
    }

    /**
     * Iterate over all the functions defined in this function library. The objects
     * returned by the iterator are of class {@link UserFunction}
     * @return an iterator delivering the {@link UserFunction} objects representing
     * the user-defined functions in a stylesheet or query
     */

    public Iterator<UserFunction> iterateFunctions() {
        return functions.values().iterator();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.