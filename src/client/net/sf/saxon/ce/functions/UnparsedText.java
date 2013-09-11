package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.StringValue;


public class UnparsedText extends SystemFunction {

    public UnparsedText(int operation) {
        this.operation = operation;
    }

    public UnparsedText newInstance() {
        return new UnparsedText(operation);
    }

    // TODO: There is now a requirement that the results should be stable

    // TODO: Consider supporting a query parameter ?substitute-character=xFFDE

    String expressionBaseURI = null;

    public static final int UNPARSED_TEXT = 0;
    public static final int UNPARSED_TEXT_AVAILABLE = 1;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }


    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
        // in principle we could pre-evaluate any call of unparsed-text() with
        // constant arguments. But we don't, because the file contents might
        // change before the stylesheet executes.
    }

    public int computeSpecialProperties() {
        return super.computeSpecialProperties() &~ StaticProperty.NON_CREATIVE;
        // Pretend the function is creative to prevent the result going into a global variable,
        // which takes excessive memory. (TODO: But it does ensure stability...)
    }

    /**
     * This method handles evaluation of the function:
     * it returns a StringValue in the case of unparsed-text(), or a BooleanValue
     * in the case of unparsed-text-available(). In the case of unparsed-text-lines()
     * this shouldn't be called, but we deal with it anyway.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        CharSequence content;
        StringValue result;
        try {
            StringValue hrefVal = (StringValue)argument[0].evaluateItem(context);
            if (hrefVal == null) {
                return null;
            }
            String href = hrefVal.getStringValue();

            String encoding = null;
            if (getNumberOfArguments() == 2) {
                encoding = argument[1].evaluateItem(context).getStringValue();
            }
            content = readFile(href, expressionBaseURI, encoding, context);
            result = new StringValue(content);
        } catch (XPathException err) {
            if (operation == UNPARSED_TEXT_AVAILABLE) {
                return BooleanValue.FALSE;
            } else {
                err.maybeSetErrorCode("XTDE1170");
                throw err;
            }
        }
        switch (operation) {
            case UNPARSED_TEXT_AVAILABLE:
                return BooleanValue.TRUE;
            case UNPARSED_TEXT:
                return result;
            default:
                throw new UnsupportedOperationException(operation+"");
        }
    }

    /**
     * Supporting routine to load one external file given a URI (href) and a baseURI
     */

    private CharSequence readFile(String href, String baseURI, String encoding, XPathContext context)
            throws XPathException {

        // Use the URI machinery to validate and resolve the URIs

        URI absoluteURI = getAbsoluteURI(href, baseURI);
        try {
            return XMLDOM.makeHTTPRequest(absoluteURI.toString());
        } catch (Exception e) {
            throw new XPathException(e);
        }

    }

    private URI getAbsoluteURI(String href, String baseURI) throws XPathException {
        URI absoluteURI;
        try {
            absoluteURI = ResolveURI.makeAbsolute(href, baseURI);
        } catch (URI.URISyntaxException err) {
            throw new XPathException(err.getMessage(), "XTDE1170");
        }

        if (absoluteURI.getFragment() != null) {
            throw new XPathException("URI for unparsed-text() must not contain a fragment identifier", "XTDE1170");
        }

        return absoluteURI;
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.