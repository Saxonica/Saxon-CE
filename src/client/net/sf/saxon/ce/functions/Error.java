package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.QNameValue;

/**
* Implement XPath function fn:error()
*/

public class Error extends SystemFunction {

    public Error newInstance() {
        return new Error();
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Evaluation of the expression always throws an error
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        QNameValue qname = null;
        if (argument.length > 0) {
            qname = (QNameValue)argument[0].evaluateItem(context);
        }
        if (qname == null) {
            qname = new QNameValue("err", NamespaceConstant.ERR,
                    (argument.length == 1 ? "FOTY0004" : "FOER0000"));
        }
        String description;
        if (argument.length > 1) {
            description = argument[1].evaluateItem(context).getStringValue();
        } else {
            description = "Error signalled by application call on error()";
        }
        XPathException e = new XPathException(description);
        e.setErrorCodeQName(qname.toStructuredQName());
        e.setLocator(getSourceLocator());
        //if (argument.length > 2) {
            // The error object is ignored
        //}
        throw e;
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
