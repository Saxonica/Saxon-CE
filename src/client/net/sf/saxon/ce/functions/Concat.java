package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.event.ComplexContentOutputter;
import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.StringValue;


public class Concat extends SystemFunction {

    public Concat newInstance() {
        return new Concat();
    }

    

    /**
    * Get the required type of the nth argument
    */

    protected SequenceType getRequiredType(int arg) {
        return getDetails().argumentTypes[0];
        // concat() is a special case
    }

    /**
    * Evaluate the function in a string context
    */

    public CharSequence evaluateAsString(XPathContext c) throws XPathException {
        return evaluateItem(c).getStringValueCS();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        int numArgs = argument.length;
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        for (int i=0; i<numArgs; i++) {
            AtomicValue val = (AtomicValue)argument[i].evaluateItem(c);
            if (val!=null) {
                sb.append(val.getStringValueCS());
            }
        }
        return StringValue.makeStringValue(sb.condense());
    }

    /**
     * Process the instruction in push mode. This avoids constructing the concatenated string
     * in memory, instead each argument can be sent straight to the serializer.
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        SequenceReceiver out = context.getReceiver();
        if (out instanceof ComplexContentOutputter) {
            // This optimization is only safe if the output forms part of document or element content
            int numArgs = argument.length;
            // Start and end with an empty string to force space separation from any previous or following outputs
            out.append(StringValue.EMPTY_STRING, 0);
            boolean empty = true;
            for (int i=0; i<numArgs; i++) {
                AtomicValue val = (AtomicValue)argument[i].evaluateItem(context);
                if (val!=null) {
                    out.characters(val.getStringValueCS());
                    empty = false;
                }
            }
            if (!empty) {
                out.append(StringValue.EMPTY_STRING, 0);
            }
        } else {
            out.append(evaluateItem(context), 0);
        }
    }
}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
