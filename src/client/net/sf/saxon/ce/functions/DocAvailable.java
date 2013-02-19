package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.ErrorListener;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.DocumentPool;
import client.net.sf.saxon.ce.om.DocumentURI;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.BooleanValue;


/**
 * Implement the fn:doc-available() function
 */

public class DocAvailable extends SystemFunction {

    public DocAvailable newInstance() {
        return new DocAvailable();
    }

    private String expressionBaseURI = null;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }

    /**
     * Get the static base URI of the expression
     */

    public String getStaticBaseURI() {
        return expressionBaseURI;
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
     * Evaluate the expression
     * @param context
     * @return the result of evaluating the expression (a BooleanValue)
     * @throws client.net.sf.saxon.ce.trans.XPathException
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(context);
        if (hrefVal==null) {
            return BooleanValue.FALSE;
        }
        String href = hrefVal.getStringValue();

        // suppress all error messages while attempting to fetch the document
        Controller controller = context.getController();
        ErrorListener old = controller.getErrorListener();
        controller.setErrorListener(ErrorDiscarder.THE_INSTANCE);
        boolean b = docAvailable(href, context);
        controller.setErrorListener(old);
        return BooleanValue.get(b);
    }

    private static class ErrorDiscarder implements ErrorListener {
        public static ErrorDiscarder THE_INSTANCE = new ErrorDiscarder();
        public void error(XPathException exception) {}
    }

    private boolean docAvailable(String href, XPathContext context) throws XPathException {
        try {
            DocumentURI documentKey = DocumentFn.computeDocumentKey(href, expressionBaseURI);
            DocumentPool pool = context.getController().getDocumentPool();
            if (pool.isMarkedUnavailable(documentKey)) {
                return false;
            }
            DocumentInfo doc = pool.find(documentKey);
            if (doc != null) {
                return true;
            }
            Item item = DocumentFn.makeDoc(href, expressionBaseURI, context, getSourceLocator());
            if (item != null) {
                return true;
            } else {
                // The document does not exist; ensure that this remains the case
                pool.markUnavailable(documentKey);
                return false;
            }
        } catch (XPathException e) {
            return false;
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
