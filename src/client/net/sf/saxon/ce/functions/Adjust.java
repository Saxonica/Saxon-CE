package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.CalendarValue;
import client.net.sf.saxon.ce.value.DayTimeDurationValue;

/**
* This class implements the XPath 2.0 functions
 * adjust-date-to-timezone(), adjust-time-timezone(), and adjust-dateTime-timezone().
*/


public class Adjust extends SystemFunction {

    public Adjust newInstance() {
        return new Adjust();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(context);
        if (av1==null) {
            return null;
        }
        CalendarValue in = (CalendarValue)av1;

        int nargs = argument.length;
        DayTimeDurationValue tz;
        if (nargs==1) {
            return in.adjustTimezone(context.getImplicitTimezone());
        } else {
            AtomicValue av2 = (AtomicValue)argument[1].evaluateItem(context);
            if (av2==null) {
                return in.removeTimezone();
            }
            tz = (DayTimeDurationValue)av2;
            long microseconds = tz.getLengthInMicroseconds();
            if (microseconds%60000000 != 0) {
                dynamicError("Timezone is not an integral number of minutes", "FODT0003");
            }
            int tzminutes = (int)(microseconds / 60000000);
            if (Math.abs(tzminutes) > 14*60) {
                dynamicError("Timezone out of range (-14:00 to +14:00)", "FODT0003");
            }
            return in.adjustTimezone(tzminutes);
        }
    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
