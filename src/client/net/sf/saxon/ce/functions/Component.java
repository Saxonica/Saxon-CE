package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
 * This class supports the get_X_from_Y functions defined in XPath 2.0
 */

public class Component extends SystemFunction {


    public Component(int operation) {
        this.operation = operation;
    }

    public Component newInstance() {
        return new Component(operation);
    }

    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOURS = 4;
    public static final int MINUTES = 5;
    public static final int SECONDS = 6;
    public static final int TIMEZONE = 7;
    public static final int LOCALNAME = 8;
    public static final int NAMESPACE = 9;
    public static final int PREFIX = 10;
    public static final int MICROSECONDS = 11;   // internal use only
    public static final int WHOLE_SECONDS = 12;  // internal use only
    public static final int YEAR_ALLOWING_ZERO = 13;  // internal use only

    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg = (AtomicValue)argument[0].evaluateItem(context);

        if (arg == null) {
            return null;
        }
        return arg.getComponent(operation);

    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.