package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.regex.ARegularExpression;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.StringValue;


/**
* This class implements the matches() function for regular expression matching
*/

public class Matches extends SystemFunction {

    public Matches newInstance() {
        return new Matches();
    }

    /**
     * Evaluate the matches() function to give a Boolean value.
     * @param c  The dynamic evaluation context
     * @return the result as a BooleanValue, or null to indicate the empty sequence
     * @throws XPathException on an error
     */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv0 = (AtomicValue)argument[0].evaluateItem(c);
        if (sv0==null) {
            sv0 = StringValue.EMPTY_STRING;
        }


        AtomicValue pat = (AtomicValue)argument[1].evaluateItem(c);
        if (pat==null) return null;

        CharSequence flags;
        if (argument.length==2) {
            flags = "";
        } else {
            AtomicValue sv2 = (AtomicValue)argument[2].evaluateItem(c);
            if (sv2==null) return null;
            flags = sv2.getStringValue();
        }

        try {
            ARegularExpression re = new ARegularExpression(pat.getStringValue(), flags.toString(), "XP20", null);
            return BooleanValue.get(re.containsMatch(sv0.getStringValue()));

        } catch (XPathException err) {
            XPathException de = new XPathException(err);
            de.maybeSetErrorCode("FORX0002");
            de.setXPathContext(c);
            throw de;
        }
    }


}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.