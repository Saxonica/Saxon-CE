package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.DateTimeValue;
import client.net.sf.saxon.ce.value.DateValue;
import client.net.sf.saxon.ce.value.TimeValue;


/**
* This class supports the dateTime($date, $time) function
*/

public class DateTimeConstructor extends SystemFunction {

    public DateTimeConstructor newInstance() {
        return new DateTimeConstructor();
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        AtomicValue arg1 = (AtomicValue)argument[1].evaluateItem(context);
        try {
            return DateTimeValue.makeDateTimeValue((DateValue)arg0, (TimeValue)arg1);
        } catch (XPathException e) {
            e.maybeSetLocation(getSourceLocator());
            e.maybeSetContext(context);
            throw e;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.