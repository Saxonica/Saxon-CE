package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.value.DoubleValue;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * This class implements the XPath substring() function
 */

public class Substring extends SystemFunction {

    public Substring newInstance() {
        return new Substring();
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StringValue sv = (StringValue)argument[0].evaluateItem(context);
        if (sv==null) {
            return StringValue.EMPTY_STRING;
        }
        String str = sv.getStringValue();
        double start = ((DoubleValue)argument[1].evaluateItem(context)).round().getDoubleValue();
        double length;
        if (argument.length==2) {
            length = (double)str.length();
        } else {
            length = ((DoubleValue)argument[2].evaluateItem(context)).round().getDoubleValue();
            if (length < 0) {
            	length = 0;
            }
        }
        FastStringBuffer sb = new FastStringBuffer((int)length);
        int i=0;
        int pos=0;
        while(i<start-1 && pos<str.length()) {
            int c = (int)str.charAt(pos++);
            if (c<55296 || c>56319) i++;    // don't count high surrogates, i.e. D800 to DBFF
        }
        int j=0;
        while (j<length && pos<str.length()) {
            char c = str.charAt(pos++);
            sb.append(c);
            if ((int)c<55296 || (int)c>56319) j++;    // don't count high surrogates, i.e. D800 to DBFF
        }
        StringValue result = new StringValue(sb);
        if (sv.isKnownToContainNoSurrogates()) {
            result.setContainsNoSurrogates();
        }
        return result;
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
