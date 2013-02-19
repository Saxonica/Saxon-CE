package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AnyURIValue;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.EmptySequence;

import java.util.HashMap;

/**
 * The SystemFunctionLibrary represents the collection of functions in the fn: namespace. That is, the
 * functions defined in the "Functions and Operators" specification, optionally augmented by the additional
 * functions defined in XSLT.
 */

public class SystemFunctionLibrary implements FunctionLibrary {

    private int functionSet;

    private static HashMap<Integer, SystemFunctionLibrary> THE_INSTANCES =
            new HashMap<Integer, SystemFunctionLibrary>(3);

    /**
     * Factory method to create or get a SystemFunctionLibrary
     * @param functionSet determines the set of functions allowed. One or more of the bit settings
     * {@link StandardFunction#CORE}, {@link StandardFunction#XSLT}, etc
     * @return the appropriate SystemFunctionLibrary
     */

    public static SystemFunctionLibrary getSystemFunctionLibrary(int functionSet) {
        if (THE_INSTANCES.get(functionSet) == null) {
            THE_INSTANCES.put(functionSet, new SystemFunctionLibrary(functionSet));
        }
        return THE_INSTANCES.get(functionSet);
    }

    /**
     * Create a SystemFunctionLibrary
     * @param functionSet determines the set of functions allowed. One or more of the bit settings
     * {@link StandardFunction#CORE}, {@link StandardFunction#XSLT}, etc
     */

    private SystemFunctionLibrary(int functionSet) {
        this.functionSet = functionSet;
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
     *                     function-available() function; in this case the method should return a zero-length array
     *                     if there is some
     *                     function of this name available for calling.
     * @return if a function of this name and arity is available for calling, then the type signature of the
     * function, as an array of sequence types in which the zeroth entry represents the return type; or a zero-length
     * array if the function exists but the signature is not known; or null if the function does not exist
     */

    public boolean hasFunctionSignature(StructuredQName functionName, int arity) {
        String uri = functionName.getNamespaceURI();
        String local = functionName.getLocalName();
        if (uri.equals(NamespaceConstant.FN)) {
            StandardFunction.Entry entry = StandardFunction.getFunction(local, arity);
            return entry != null &&
                    ((functionSet & entry.applicability) != 0) &&
                    (arity == -1 ||
                    (arity >= entry.minArguments && arity <= entry.maxArguments));
        } else {
            return false;
        }
    }

    /**
     * Bind a function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName the name of the function to be bound
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env
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
        String uri = functionName.getNamespaceURI();
        if (uri.equals(NamespaceConstant.FN)) {
            String local = functionName.getLocalName();
            StandardFunction.Entry entry = StandardFunction.getFunction(local, staticArgs.length);
            if (entry != null && entry.minArguments == 1 && staticArgs.length == 0 &&
                    ("name".equals(local) || "local-name".equals(local) || "namespace-uri".equals(local) ||
                    "generate-id".equals(local) || "string".equals(local) || "number".equals(local) ||
                    "base-uri".equals(local))) {
                Expression[] newArgs = new Expression[]{new ContextItemExpression()};
                return bind(functionName, newArgs, env, container);
            }
            if (entry == null) {
                // Functions that can be evaluated or simplified immediately...
                if ("true".equals(local) && staticArgs.length == 0) {
                    return new Literal(BooleanValue.TRUE);
                } else if ("false".equals(local) && staticArgs.length == 0) {
                    return new Literal(BooleanValue.FALSE);
                } else if ("collection".equals(local) && staticArgs.length <= 1) {
                    if (staticArgs.length == 0) {
                        return new Literal(EmptySequence.getInstance());
                    } else {
                        return new ErrorExpression(
                                new XPathException("No collection URIs are recognized by Saxon-CE", "FODC0002"));
                    }
                } else if ("data".equals(local) && staticArgs.length == 1) {
                    return new Atomizer(staticArgs[0]);
                } else if ("default-collation".equals(local) && staticArgs.length == 0) {
                    return new StringLiteral(env.getDefaultCollationName());
                } else if ("exactly-one".equals(local) && staticArgs.length == 1) {
                    RoleLocator role = new RoleLocator(RoleLocator.FUNCTION, "one-or-more", 1);
                    role.setErrorCode("FORG0005");
                    return CardinalityChecker.makeCardinalityChecker(staticArgs[0], StaticProperty.EXACTLY_ONE, role);     
                } else if ("idref".equals(local) && (staticArgs.length == 1 || staticArgs.length == 2)) {
                    return new Literal(EmptySequence.getInstance());
                } else if ("nilled".equals(local) && staticArgs.length == 1) {
                    return new Literal(BooleanValue.FALSE);
                } else if ("one-or-more".equals(local) && staticArgs.length == 1) {
                    RoleLocator role = new RoleLocator(RoleLocator.FUNCTION, "one-or-more", 1);
                    role.setErrorCode("FORG0004");
                    return CardinalityChecker.makeCardinalityChecker(staticArgs[0], StaticProperty.ALLOWS_ONE_OR_MORE, role);
                } else if ("static-base-uri".equals(local) && staticArgs.length == 0) {
                        String baseURI = env.getBaseURI();
                        return (baseURI == null ? Literal.makeEmptySequence() : new Literal(new AnyURIValue(baseURI)));
                } else if ("trace".equals(local) && staticArgs.length == 2) {
                    return staticArgs[0];
                } else if ("unordered".equals(local) && staticArgs.length == 1) {
                    return staticArgs[0];
                } else if ("unparsed-entity-uri".equals(local) && staticArgs.length == 1) {
                    return new StringLiteral("");
                } else if ("unparsed-entity-public-id".equals(local) && staticArgs.length == 1) {
                    return new StringLiteral("");
                } else if ("zero-or-one".equals(local) && staticArgs.length == 1) {
                    RoleLocator role = new RoleLocator(RoleLocator.FUNCTION, "zero-or-one", 1);
                    role.setErrorCode("FORG0003");
                    return CardinalityChecker.makeCardinalityChecker(staticArgs[0], StaticProperty.ALLOWS_ZERO_OR_ONE, role);
                } else if (StandardFunction.getFunction(local, -1) == null) {
                    XPathException err = new XPathException("Unknown system function " + local + "()");
                    err.setErrorCode("XPST0017");
                    err.setIsStaticError(true);
                    throw err;
                } else {
                    XPathException err = new XPathException("System function " + local + "#"+staticArgs.length +" cannot be called with "
                            + pluralArguments(staticArgs.length));
                    err.setErrorCode("XPST0017");
                    err.setIsStaticError(true);
                    throw err;
                }
            }
            if ((functionSet & entry.applicability) == 0) {
                XPathException err = new XPathException("System function " + local + "#"+staticArgs.length +" is not available with this host language");
                err.setErrorCode("XPST0017");
                err.setIsStaticError(true);
                throw err;
            }
            SystemFunction f = entry.skeleton.newInstance();
            f.setDetails(entry);
            f.setFunctionName(functionName);
            f.setArguments(staticArgs);
            f.setContainer(container);
            checkArgumentCount(staticArgs.length, entry.minArguments, entry.maxArguments, local);           
            return f;
        } else {
            return null;
        }
    }

    /**
    * Check number of arguments. <BR>
    * A convenience routine for use in subclasses.
    * @param numArgs the actual number of arguments (arity)
    * @param min the minimum number of arguments allowed
    * @param max the maximum number of arguments allowed
    * @param local the local name of the function (for diagnostics)
    * @return the actual number of arguments
    * @throws client.net.sf.saxon.ce.trans.XPathException if the number of arguments is out of range
    */

    private int checkArgumentCount(int numArgs, int min, int max, String local) throws XPathException {
        if (min==max && numArgs != min) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have "
                    + min + pluralArguments(min));
        }
        if (numArgs < min) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have at least "
                    + min + pluralArguments(min));
        }
        if (numArgs > max) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have no more than "
                    + max + pluralArguments(max));
        }
        return numArgs;
    }

    /**
     * Utility routine used in constructing error messages
     * @param num the number of arguments
     * @return the string " argument" or "arguments" depending whether num is plural
    */

    private static String pluralArguments(int num) {
        if (num==1) return " argument";
        return " arguments";
    }

}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.