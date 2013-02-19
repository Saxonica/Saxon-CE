package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.IntegerValue;

/**
* XSLT 2.0 compare() function
*/

// Supports string comparison using a collation

public class Compare extends CollatingFunction {

    @Override
    public Compare newInstance() {
        return new Compare();
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0==null) {
            return null;
        }

        AtomicValue arg1 = (AtomicValue)argument[1].evaluateItem(context);
        if (arg1==null) {
            return null;
        }

        GenericAtomicComparer collator = getAtomicComparer(2, context);

        int result = collator.compareAtomicValues(arg0, arg1);
        if (result < 0) {
            return IntegerValue.MINUS_ONE;
        } else if (result > 0) {
            return IntegerValue.PLUS_ONE;
        } else {
            return IntegerValue.ZERO;
        }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
