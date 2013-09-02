package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.functions.codenorm.Normalizer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;

/**
 * Implement the XPath normalize-unicode() function
 */

public class NormalizeUnicode extends SystemFunction {

    public NormalizeUnicode newInstance() {
        return new NormalizeUnicode();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        StringValue sv = (StringValue)argument[0].evaluateItem(c);
        if (sv==null) {
            return StringValue.EMPTY_STRING;
        }

        byte fb = Normalizer.C;
        if (argument.length == 2) {
            String form = Whitespace.trim(argument[1].evaluateAsString(c));
            if (form.equalsIgnoreCase("NFC")) {
                fb = Normalizer.C;
            } else if (form.equalsIgnoreCase("NFD")) {
                fb = Normalizer.D;
            } else if (form.equalsIgnoreCase("NFKC")) {
                fb = Normalizer.KC;
            } else if (form.equalsIgnoreCase("NFKD")) {
                fb = Normalizer.KD;
            } else if (form.length() == 0) {
                return sv;
            } else {
                dynamicError("Normalization form " + form + " is not supported", "FOCH0003", c);
            }
        }

        // fast path for ASCII strings: normalization is a no-op
        boolean allASCII = true;
        CharSequence chars = sv.getStringValue();
        for (int i=chars.length()-1; i>=0; i--) {
            if (chars.charAt(i) > 127) {
                allASCII = false;
                break;
            }
        }
        if (allASCII) {
            return sv;
        }


        Normalizer norm = new Normalizer(fb, c.getConfiguration());
        CharSequence result = norm.normalize(sv.getStringValue());
        return StringValue.makeStringValue(result);
    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
