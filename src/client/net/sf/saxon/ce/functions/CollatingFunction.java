package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;


/**
 * Abstract superclass for all functions that take an optional collation argument
 */

// Supports string comparison using a collation

public abstract class CollatingFunction extends SystemFunction {


    // The collation, if known statically
    protected StringCollator stringCollator = null;
    private StaticContext staticContext = null;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (staticContext == null) {
            staticContext = visitor.getStaticContext();
        }
        if (stringCollator == null) {
            preEvaluateCollation(staticContext);
        }
        super.checkArguments(visitor);
    }

    /**
     * Pre-evaluate the collation argument if its value is known statically
     * @param env the static XPath evaluation context
     */

    private void preEvaluateCollation(StaticContext env) throws XPathException {
        if (getNumberOfArguments() == getDetails().maxArguments) {
            final Expression collationExp = argument[getNumberOfArguments() - 1];
            final Sequence collationVal = (collationExp instanceof Literal ? ((Literal)collationExp).getValue() : null);
            if (collationVal instanceof AtomicValue) {
                // Collation is supplied as a constant
                String collationName = ((AtomicValue)collationVal).getStringValue();
                collationName = resolveCollationURI(collationName);
                stringCollator = env.getConfiguration().getNamedCollation(collationName);
                // if collation is unknown, report the error at run-time
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
        return new GenericAtomicComparer(getCollator(arg, context), context.getImplicitTimezone());
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
                collationName = resolveCollationURI(collationName);
                return context.getConfiguration().getNamedCollation(collationName);
            } else {
                // Fallback - this shouldn't happen
                return CodepointCollator.getInstance();
            }
        }
    }

    private String resolveCollationURI(String collationName) throws XPathException {
        URI collationURI;
        try {
            collationURI = new URI(collationName, true);
            if (!collationURI.isAbsolute()) {
                String expressionBaseURI = staticContext.getBaseURI();
                if (expressionBaseURI == null) {
                    XPathException err = new XPathException("Cannot resolve relative collation URI '" + collationName +
                            "': unknown or invalid base URI");
                    err.setErrorCode("FOCH0002");
                    err.setLocator(this.getSourceLocator());
                    throw err;
                }
                collationURI = new URI(expressionBaseURI).resolve(collationURI.toString());
                return collationURI.toString();
            } else {
                return collationName;
            }
        } catch (URI.URISyntaxException e) {
            XPathException err = new XPathException("Collation name '" + collationName + "' is not a valid URI");
            err.setErrorCode("FOCH0002");
            err.setLocator(this.getSourceLocator());
            throw err;
        }
    }

    protected void doesNotSupportSubstringMatching(XPathContext context) throws XPathException {
        dynamicError("The collation requested for " + getDisplayName() +
                        " does not support substring matching", "FOCH0004");
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
