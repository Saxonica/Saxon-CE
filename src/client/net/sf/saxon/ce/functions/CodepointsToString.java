package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NameChecker;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.UTF16CharacterSet;
import client.net.sf.saxon.ce.value.NumericValue;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * This class supports the function codepoints-to-string
 */

public class CodepointsToString extends SystemFunction {

    public CodepointsToString newInstance() {
        return new CodepointsToString();
    }

    /**
    * Evaluate
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        SequenceIterator si = argument[0].iterate(c);
        while (true) {
            NumericValue nextInt = (NumericValue)si.next();
            if (nextInt == null) {
                break;
            }
            long next = nextInt.intValue();
            if (next < 0 || next > Integer.MAX_VALUE || !NameChecker.isValidChar((int)next)) {
                XPathException e = new XPathException("Invalid XML character [x " + Integer.toHexString((int)next) + ']');
                e.setErrorCode("FOCH0001");
                e.setXPathContext(c);
                throw e;
            }
            if (next<65536) {
                sb.append((char)next);
            } else {  // output a surrogate pair
                sb.append(UTF16CharacterSet.highSurrogate((int)next));
                sb.append(UTF16CharacterSet.lowSurrogate((int)next));
            }
        }
        return StringValue.makeStringValue(sb.condense());
    }


}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
