package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.Version;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * Implementation of the XSLT system-property() function
 */

public class SystemProperty extends SystemFunction {

    public SystemProperty newInstance() {
        return new SystemProperty();
    }

    private StaticContext staticContext;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (staticContext == null) {
            staticContext = visitor.getStaticContext();
        }
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

            StructuredQName qName;
            CharSequence name = argument[0].evaluateItem(context).getStringValue();
            try {
                qName = StructuredQName.fromLexicalQName(name, "", staticContext.getNamespaceResolver());
            } catch (XPathException err) {
                 dynamicError("Invalid system property name. " + err.getMessage(), "XTDE1390");
                 return null;
            }
        return new StringValue(getProperty(
                qName.getNamespaceURI(), qName.getLocalName()));
    }

    /**
     * Here's the real code:
     *
     * @param uri the namespace URI of the system property name
     * @param local the local part of the system property name
     * @return the value of the corresponding system property
    */

    public static String getProperty(String uri, String local) {
        if (uri.equals(NamespaceConstant.XSLT)) {
            if (local.equals("version")) {
                return Version.getXSLVersionString();
            } else if (local.equals("vendor")) {
                return Version.getProductTitle();
            } else if (local.equals("vendor-url")) {
                return Version.getWebSiteAddress();
            } else if (local.equals("product-name")) {
                return Version.getProductName();
            } else if (local.equals("product-version")) {
                return Version.getProductVariantAndVersion();
            } else if (local.equals("supports-serialization")) {
                return "no";
            } else if (local.equals("supports-backwards-compatibility")) {
                return "yes";
            } else if (local.equals("supports-namespace-axis")) {  // Erratum E14
                return "yes";
            } else if (local.equals("is-schema-aware")) {
                return "no";
            }
            return "";

	    } else {
	    	return "";
	    }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
