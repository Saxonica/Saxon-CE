package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Value;


/**
 * Abstract superclass for all functions that take an optional collation argument
 */

// Supports string comparison using a collation

public abstract class CollatingFunction extends SystemFunction {


    // The collation, if known statically
    protected StringCollator stringCollator = null;
    private String absoluteCollationURI = null;
    private URI expressionBaseURI = null;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (stringCollator == null) {
            StaticContext env = visitor.getStaticContext();
            saveBaseURI(env, false);
            preEvaluateCollation(env);
        }
        super.checkArguments(visitor);
    }

    private void saveBaseURI(StaticContext env, boolean fail) throws XPathException {
        if (expressionBaseURI == null) {
            String base = null;
            try {
                base = env.getBaseURI();
                if (base != null) {
                    expressionBaseURI = new URI(base);
                }
            } catch (URI.URISyntaxException e) {
                // perhaps escaping special characters will fix the problem

                String esc = EscapeURI.iriToUri(base).toString();
                try {
                    expressionBaseURI = new URI(esc);
                } catch (URI.URISyntaxException e2) {
                    // don't fail unless the base URI is actually needed (it usually isn't)
                    expressionBaseURI = null;
                }

                if (expressionBaseURI == null && fail) {
                    XPathException err = new XPathException("The base URI " + Err.wrap(env.getBaseURI(), Err.URI) +
                            " is not a valid URI");
                    err.setLocator(this.getSourceLocator());
                    throw err;
                }
            }
        }
    }

    /**
     * Get the saved static base URI
     * @return the static base URI
     */

    public URI getExpressionBaseURI() {
        return expressionBaseURI;
    }

    /**
     * Get the collation if known statically, as a StringCollator object
     * @return a StringCollator. Return null if the collation is not known statically.
     */

    public StringCollator getStringCollator() {
        return stringCollator;
    }

    /**
     * Get the absolute collation URI if known statically, as a string
     * @return the absolute collation URI, as a string, or null if it is not known statically
     */

    public String getAbsoluteCollationURI() {
        return absoluteCollationURI;
    }

    /**
     * Pre-evaluate the collation argument if its value is known statically
     * @param env the static XPath evaluation context
     */

    private void preEvaluateCollation(StaticContext env) throws XPathException {
        if (getNumberOfArguments() == getDetails().maxArguments) {
            final Expression collationExp = argument[getNumberOfArguments() - 1];
            final Value collationVal = (collationExp instanceof Literal ? ((Literal)collationExp).getValue() : null);
            if (collationVal instanceof AtomicValue) {
                // Collation is supplied as a constant
                String collationName = collationVal.getStringValue();
                URI collationURI;
                try {
                    collationURI = new URI(collationName);
                    if (!collationURI.isAbsolute()) {
                        saveBaseURI(env, true);
                        if (expressionBaseURI == null) {
                            XPathException err = new XPathException("The collation name is a relative URI, but the base URI is unknown");
                            err.setErrorCode("XPST0001");
                            err.setIsStaticError(true);
                            err.setLocator(this.getSourceLocator());
                            throw err;
                        }
                        URI base = expressionBaseURI;
                        collationURI = base.resolve(collationURI.toString());
                        collationName = collationURI.toString();
                    }
                } catch (URI.URISyntaxException e) {
                    XPathException err = new XPathException("Collation name '" + collationName + "' is not a valid URI");
                    err.setErrorCode("FOCH0002");
                    err.setIsStaticError(true);
                    err.setLocator(this.getSourceLocator());
                    throw err;
                }
                StringCollator comp = env.getConfiguration().getNamedCollation(collationName);
                if (comp == null) {
                    XPathException err = new XPathException("Unknown collation " + Err.wrap(collationName, Err.URI));
                    err.setErrorCode("FOCH0002");
                    err.setIsStaticError(true);
                    err.setLocator(this.getSourceLocator());
                    throw err;
                } else {
                    stringCollator = comp;
                }
            } else {
                // collation isn't known until run-time
            }
        } else {
            // Use the default collation
            String uri = env.getDefaultCollationName();
            stringCollator = env.getConfiguration().getNamedCollation(uri);
        }
    }


    /**
     * Get a GenericAtomicComparer that can be used to compare values. This method is called
     * at run time by subclasses to evaluate the parameter containing the collation name.
     * <p/>
     * <p>The difference between this method and {@link #getCollator} is that a
     * GenericAtomicComparer is capable of comparing values of any atomic type, not only
     * strings. It is therefore called by functions such as compare, deep-equal, index-of, and
     * min() and max() where the operands may include a mixture of strings and other types.</p>
     *
     * @param arg     the position of the argument (starting at 0) containing the collation name.
     *                If this argument was not supplied, the default collation is used
     * @param context The dynamic evaluation context.
     * @return a GenericAtomicComparer that can be used to compare atomic values.
     */

    protected GenericAtomicComparer getAtomicComparer(int arg, XPathContext context) throws XPathException {
        // TODO:PERF avoid creating a new object on each call when the collation is specified dynamically
        return new GenericAtomicComparer(getCollator(arg, context), context);
    }

    /**
     * Get a collator suitable for comparing strings. Returns the collator specified in the
     * given function argument if present, otherwise returns the default collator. This method is
     * called by subclasses at run time. It is used (in contrast to {@link #getAtomicComparer})
     * when it is known that the values to be compared are always strings.
     *
     * @param arg     The argument position (counting from zero) that holds the collation
     *                URI if present
     * @param context The dynamic context
     * @return a StringCollator
     */

    protected StringCollator getCollator(int arg, XPathContext context) throws XPathException {

        if (stringCollator != null) {
            // the collation was determined statically
            return stringCollator;
        } else {
            int numargs = argument.length;
            if (numargs > arg) {
                AtomicValue av = (AtomicValue) argument[arg].evaluateItem(context);
                StringValue collationValue = (StringValue) av;
                String collationName = collationValue.getStringValue();
                URI collationURI;
                try {
                    collationURI = new URI(collationName);
                    if (!collationURI.isAbsolute()) {
                        if (expressionBaseURI == null) {
                            XPathException err = new XPathException("Cannot resolve relative collation URI '" + collationName +
                                    "': unknown or invalid base URI");
                            err.setErrorCode("FOCH0002");
                            err.setXPathContext(context);
                            err.setLocator(this.getSourceLocator());
                            throw err;
                        }
                        collationURI = expressionBaseURI.resolve(collationURI.toString());
                        collationName = collationURI.toString();
                    }
                } catch (URI.URISyntaxException e) {
                    XPathException err = new XPathException("Collation name '" + collationName + "' is not a valid URI");
                    err.setErrorCode("FOCH0002");
                    err.setXPathContext(context);
                    err.setLocator(this.getSourceLocator());
                    throw err;
                }
                return context.getConfiguration().getNamedCollation(collationName);
            } else {
                // Fallback - this shouldn't happen
                return CodepointCollator.getInstance();
            }
        }
    }

    protected void doesNotSupportSubstringMatching(XPathContext context) throws XPathException {
        dynamicError("The collation requested for " + getDisplayName() +
                        " does not support substring matching", "FOCH0004", context);
    }

    protected final boolean evalContains(XPathContext context) throws XPathException {
        StringValue arg1 = (StringValue)argument[1].evaluateItem(context);
        if (arg1==null || arg1.isZeroLength()) {
            return true;
        }

        StringValue arg0 = (StringValue)argument[0].evaluateItem(context);
        if (arg0==null || arg0.isZeroLength()) {
            return false;
        }

        String s0 = arg0.getStringValue();
        String s1 = arg1.getStringValue();

        if (stringCollator instanceof CodepointCollator) {
            return doComparison(s0, s1);
        } else {
            doesNotSupportSubstringMatching(context);
            return false;

        }
    }

    protected boolean doComparison(String s0, String s1) {
        return false;
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
