package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * Implement the XPath translate() function
 */

public class Translate extends SystemFunction {

    public Translate newInstance() {
        return new Translate();
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        StringValue sv1 = (StringValue)argument[0].evaluateItem(context);
        if (sv1==null) {
            return StringValue.EMPTY_STRING;
        }

        StringValue sv2 = (StringValue)argument[1].evaluateItem(context);

        StringValue sv3 = (StringValue)argument[2].evaluateItem(context);

        int[] a1 = sv1.expand();
        int[] a2 = sv2.expand();
        int[] a3 = sv3.expand();

        int length1 = a1.length;
        int length2 = a2.length;
        FastStringBuffer sb = new FastStringBuffer(length1);
    inputLoop:
        for (int i=0; i<length1; i++) {
            int ch = a1[i];
            for (int j=0; j<length2; j++) {
                 if (a2[j] == ch) {
                     if (j < a3.length) {
                        sb.appendWideChar(a3[j]);
                     } else {
                         // do nothing, delete the character
                     }
                     continue inputLoop;
                 }
            }
            sb.appendWideChar(ch);
        }

        return StringValue.makeStringValue(sb);
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
