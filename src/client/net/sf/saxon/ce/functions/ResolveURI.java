package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.AnyURIValue;
import client.net.sf.saxon.ce.value.AtomicValue;


/**
* This class supports the resolve-uri() functions in XPath 2.0
*/

public class ResolveURI extends SystemFunction {

    public ResolveURI newInstance() {
        return new ResolveURI();
    }

    String expressionBaseURI = null;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
            if (expressionBaseURI == null && argument.length == 1) {
                XPathException de = new XPathException("Base URI in static context of resolve-uri() is unknown");
                de.setErrorCode("FONS0005");
                throw de;
            }
        }
    }

    /**
     * Get the static base URI of the expression
     */

    public String getStaticBaseURI() {
        return expressionBaseURI;
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0 == null) {
            return null;
        }
        String relative = arg0.getStringValue();
        String msgBase = "in resolve-uri(), Base URI ";
        String base;
        if (argument.length == 2) {
            base = argument[1].evaluateAsString(context).toString();
        } else {
            base = expressionBaseURI;
            if (expressionBaseURI == null) {
                dynamicError(msgBase + "in static context of resolve-uri() is unknown", "FONS0005", context);
                return null;
            }
        }

        try {
            URI absoluteURI = new URI(base, true);
            if (!absoluteURI.isAbsolute()) {
                URI relativeURI = new URI(relative, true);
                if (relativeURI.isAbsolute()) {
                    return new AnyURIValue(relative);
                }
                dynamicError(msgBase + "in resolve-uri(): Base URI " + Err.wrap(base) + " is not an absolute URI", "FORG0002", context);
                return null;
            }
            URI resolved = makeAbsolute(relative,  base);
            return new AnyURIValue(resolved.toString());
        } catch (URI.URISyntaxException err) {
            dynamicError(msgBase + "Base URI " + Err.wrap(base) + " is invalid: " + err.getMessage(),
                    "FORG0002", context);
            return null;
        }
    }

    /**
    * If a system ID can't be parsed as a URL, try to expand it as a relative
    * URI using the current directory as the base URI.
    */

    public static String tryToExpand(String systemId) {
        return systemId;
//        if (systemId==null) {
//            systemId = "";
//        }
//	    try {
//	        new URL(systemId);
//	        return systemId;   // all is well
//	    } catch (MalformedURLException err) {
//	        String dir;
//	        try {
//	            dir = System.getProperty("user.dir");
//	        } catch (Exception geterr) {
//	            // this doesn't work when running an applet
//	            return systemId;
//	        }
//	        if (!(dir.endsWith("/") || systemId.startsWith("/"))) {
//	            dir = dir + '/';
//	        }
//
//            URI currentDirectoryURI = new File(dir).toURI();
//            URI baseURI = currentDirectoryURI.resolve(systemId);
//            return baseURI.toString();
//
//	    }
	}

    /**
     * Construct an absolute URI from a relative URI and a base URI. The method uses the resolve
     * method of the java.net.URI class, except where the base URI uses the (non-standard) "jar:" scheme,
     * in which case the method used is <code>new URL(baseURL, relativeURL)</code>.
     *
     * <p>Spaces in either URI are converted to %20</p>
     *
     * <p>If no base URI is available, and the relative URI is not an absolute URI, then the current
     * directory is used as a base URI.</p>
     *
     * @param relativeURI the relative URI. Null is permitted provided that the base URI is an absolute URI
     * @param base        the base URI. Null is permitted provided that relativeURI is an absolute URI
     * @return the absolutized URI
     * @throws java.net.URISyntaxException if either of the strings is not a valid URI or
     * if the resolution fails
     */

    public static URI makeAbsolute(String relativeURI, String base) throws URI.URISyntaxException {
        URI absoluteURI;
        // System.err.println("makeAbsolute " + relativeURI + " against base " + base);
        if (relativeURI == null) {
            absoluteURI = new URI(ResolveURI.escapeSpaces(base), true);
            if (!absoluteURI.isAbsolute()) {
                throw new URI.URISyntaxException(base + ": Relative URI not supplied, so base URI must be absolute");
            } else {
                return absoluteURI;
            }
        }
        relativeURI = ResolveURI.escapeSpaces(relativeURI);
        base = ResolveURI.escapeSpaces(base);
        try {
            if (base==null || base.length() == 0) {
                absoluteURI = new URI(relativeURI, true);
                if (!absoluteURI.isAbsolute()) {
                    String expandedBase = ResolveURI.tryToExpand(base);
                    if (!expandedBase.equals(base)) { // prevent infinite recursion
                        return makeAbsolute(relativeURI, expandedBase);
                    }
                }

            } else {
                URI baseURI;
                try {
                    baseURI = new URI(base);
                } catch (URI.URISyntaxException e) {
                    throw new URI.URISyntaxException(base +  ": Invalid base URI: " + e.getMessage());
                }
                if (baseURI.getFragment() != null) {
                    int hash = base.indexOf('#');
                    if (hash >= 0) {
                        base = base.substring(0, hash);
                    }
                    try {
                        baseURI = new URI(base);
                    } catch (URI.URISyntaxException e) {
                        throw new URI.URISyntaxException(base +  ": Invalid base URI: " + e.getMessage());
                    }
                }
                try {
                    new URI(relativeURI, true);   // for validation only
                } catch (URI.URISyntaxException e) {
                    throw new URI.URISyntaxException(base + ": Invalid relative URI: " + e.getMessage());
                }
                absoluteURI = (relativeURI.length() == 0 ? baseURI : baseURI.resolve(relativeURI));
            }
        } catch (IllegalArgumentException err0) {
            // can be thrown by resolve() when given a bad URI
            throw new URI.URISyntaxException(relativeURI + ": Cannot resolve URI against base " + Err.wrap(base));
        }

        return absoluteURI;
    }


    /**
     * Replace spaces by %20
     */

    public static String escapeSpaces(String s) {
        // It's not entirely clear why we have to escape spaces by hand, and not other special characters;
        // it's just that tests with a variety of filenames show that this approach seems to work.
        if (s == null) return s;
        int i = s.indexOf(' ');
        if (i < 0) {
            return s;
        }
        return (i == 0 ? "" : s.substring(0, i))
                + "%20"
                + (i == s.length()-1 ? "" : escapeSpaces(s.substring(i+1)));
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
