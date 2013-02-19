package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.BooleanValue;

/**
* XPath 2.0 codepoint-equal() function.
* Compares two strings using the unicode codepoint collation. (The function was introduced
 * specifically to allow URI comparison: URIs are promoted to strings when necessary.)
*/

public class CodepointEqual extends SystemFunction {

    public CodepointEqual newInstance() {
        return new CodepointEqual();
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue op1 = (AtomicValue)argument[0].evaluateItem(context);
        if (op1 == null) {
            return null;
        }
        AtomicValue op2 = (AtomicValue)argument[1].evaluateItem(context);
        if (op2 == null) {
            return null;
        }

        return BooleanValue.get(op1.getStringValue().equals(op2.getStringValue()));
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.