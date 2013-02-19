package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Version;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamespaceResolver;
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

    private NamespaceResolver nsContext;
    private StructuredQName propertyName;
    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
        if (argument[0] instanceof StringLiteral) {
            try {
                propertyName = StructuredQName.fromLexicalQName(
                        ((StringLiteral)argument[0]).getStringValue(),
                        false,
                        visitor.getStaticContext().getNamespaceResolver());
            } catch (XPathException e) {
                String code = e.getErrorCodeLocalPart();
                if (code==null || code.equals("FOCA0002") || code.equals("FONS0004")) {
                    e.setErrorCode("XTDE1390");
                    throw e;
                }
            }
            // Don't actually read the system property yet, it might be different at run-time
        } else {
            // we need to save the namespace context
            nsContext = visitor.getStaticContext().getNamespaceResolver();
        }
    }

    /**
     * preEvaluate: this method performs compile-time evaluation for properties in the XSLT namespace only
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (propertyName != null && NamespaceConstant.XSLT.equals(propertyName.getNamespaceURI())) {
            return new StringLiteral(
                    getProperty(NamespaceConstant.XSLT, propertyName.getLocalName(), visitor.getConfiguration()));
        } else {
           return this;
        }
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        StructuredQName qName = propertyName;
        if (qName == null) {
            CharSequence name = argument[0].evaluateItem(context).getStringValueCS();
            try {
                qName = StructuredQName.fromLexicalQName(name,
                        false,
                        nsContext);
             } catch (XPathException err) {
                 dynamicError("Invalid system property name. " + err.getMessage(), "XTDE1390", context);
                 return null;
            }
        }
        return new StringValue(getProperty(
                qName.getNamespaceURI(), qName.getLocalName(), context.getConfiguration()));
    }

    /**
     * Here's the real code:
     * @param uri the namespace URI of the system property name
     * @param local the local part of the system property name
     * @param config the Saxon configuration
     * @return the value of the corresponding system property 
    */

    public static String getProperty(String uri, String local, Configuration config) {
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
                return "yes";
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
