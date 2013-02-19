package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NameChecker;
import client.net.sf.saxon.ce.om.QNameException;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.QNameValue;


/**
* This class supports the fn:QName() function (previously named fn:expanded-QName())
*/

public class QNameFn extends SystemFunction {

    public QNameFn newInstance() {
        return new QNameFn();
    }

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        try {
            XPathContext early = visitor.getStaticContext().makeEarlyEvaluationContext();
            final Item item1 = argument[1].evaluateItem(early);
            final String lex = item1.getStringValue();
            final Item item0 = argument[0].evaluateItem(early);
            String uri;
            if (item0 == null) {
                uri = "";
            } else {
                uri = item0.getStringValue();
            }
            final String[] parts = NameChecker.getQNameParts(lex);
            // The QNameValue constructor does not check the prefix
            if (parts[0].length() != 0 && !NameChecker.isValidNCName(parts[0])) {
                XPathException err = new XPathException("Malformed prefix in QName: '" + parts[0] + '\'');
                err.setErrorCode("FOCA0002");
                throw err;
            }
            return Literal.makeLiteral(
                    new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, true));
        } catch (QNameException e) {
            dynamicError(e.getMessage(), "FOCA0002", null);
            return null;
        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            throw err;
        }
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);

        String uri;
        if (arg0 == null) {
            uri = null;
        } else {
            uri = arg0.getStringValue();
        }

        try {
            final String lex = argument[1].evaluateItem(context).getStringValue();
            final String[] parts = NameChecker.getQNameParts(lex);
            // The QNameValue constructor does not check the prefix
            if (parts[0].length() != 0 && !NameChecker.isValidNCName(parts[0])) {
                XPathException err = new XPathException("Malformed prefix in QName: '" + parts[0] + '\'');
                err.setErrorCode("FORG0001");
                throw err;
            }
            return new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, true);
        } catch (QNameException e) {
            dynamicError(e.getMessage(), "FOCA0002", context);
            return null;
        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            throw err;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.