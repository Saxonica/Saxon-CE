package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.*;

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
        QualifiedNameValue qname = null;
        if (argument.length > 0) {
            qname = (QualifiedNameValue)argument[0].evaluateItem(context);
        }
        if (qname == null) {
            qname = new QNameValue("err", NamespaceConstant.ERR,
                    (argument.length == 1 ? "FOTY0004" : "FOER0000"),
                    BuiltInAtomicType.QNAME);
        }
        String description;
        if (argument.length > 1) {
            description = argument[1].evaluateItem(context).getStringValue();
        } else {
            description = "Error signalled by application call on error()";
        }
        XPathException e = new XPathException(description);
        e.setErrorCodeQName(qname.toStructuredQName());
        e.setXPathContext(context);
        e.setLocator(getSourceLocator());
        if (argument.length > 2) {
            Value errorObject = ((Value)SequenceExtent.makeSequenceExtent(argument[2].iterate(context))).reduce();
            if (errorObject instanceof SingletonItem) {
                Item root = ((SingletonItem)errorObject).getItem();
                if ((root instanceof NodeInfo) && ((NodeInfo)root).getNodeKind() == Type.DOCUMENT) {
//                    XPathEvaluator xpath = new XPathEvaluator();
//                    XPathExpression exp = xpath.createExpression("/error/@module");
//                    NodeInfo moduleAtt = (NodeInfo)exp.evaluateSingle((NodeInfo)root);
//                    String module = (moduleAtt == null ? null : moduleAtt.getStringValue());
//                    exp = xpath.createExpression("/error/@line");
//                    NodeInfo lineAtt = (NodeInfo)exp.evaluateSingle((NodeInfo)root);
//                    int line = (lineAtt == null ? -1 : Integer.parseInt(lineAtt.getStringValue()));
//                    exp = xpath.createExpression("/error/@column");
//                    NodeInfo columnAtt = (NodeInfo)exp.evaluateSingle((NodeInfo)root);
//                    int column = (columnAtt == null ? -1 : Integer.parseInt(columnAtt.getStringValue()));
//                    ExpressionLocation locator = new ExpressionLocation();
//                    locator.setSystemId(module);
//                    locator.setLineNumber(line);
//                    locator.setColumnNumber(column);
//                    e.setLocator(locator);
                }
            }
            e.setErrorObject(errorObject);
        }
        throw e;
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
