package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.ArrayIterator;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.value.IntegerValue;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * This class supports the function string-to-codepoints()
 */

public class StringToCodepoints extends SystemFunction {

    public StringToCodepoints newInstance() {
        return new StringToCodepoints();
    }

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        Item item = argument[0].evaluateItem(c);
        if (item==null) {
            return EmptyIterator.getInstance();
        }
        int[] chars = ((StringValue)item).expand();
        IntegerValue[] codes = new IntegerValue[chars.length];
        for (int i=0; i<chars.length; i++) {
            codes[i] = new IntegerValue(chars[i]);
        }
        return new ArrayIterator(codes);
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
