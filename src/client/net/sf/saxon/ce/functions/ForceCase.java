package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;


/**
* This class implements the upper-case() and lower-case() functions
*/

public class ForceCase extends SystemFunction {

    public ForceCase(int operation) {
        this.operation = operation;
    }

    public ForceCase newInstance() {
        return new ForceCase(operation);
    }

    public static final int UPPERCASE = 0;
    public static final int LOWERCASE = 1;

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
        if (sv==null) {
            return StringValue.EMPTY_STRING;
        }

        String s = sv.getStringValue();
        if (operation == UPPERCASE) {
            s = s.toUpperCase();
        } else {
            s = s.toLowerCase();
        }
        return StringValue.makeStringValue(s);
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
