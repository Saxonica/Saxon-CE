package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;

/**
 * Abstract class for fixed and computed attribute constructor expressions
 */

public abstract class AttributeCreator extends SimpleNodeConstructor {

    /**
     * Process the value of the node, to create the new node.
     * @param value the string value of the new node
     * @param context the dynamic evaluation context
     * @throws XPathException
     */

    public final void processValue(CharSequence value, XPathContext context) throws XPathException {
        StructuredQName nameCode = evaluateNameCode(context);
//        if (nameCode == -1) {
//            return null;
//        }
        SequenceReceiver out = context.getReceiver();

    	// we may need to change the namespace prefix if the one we chose is
    	// already in use with a different namespace URI: this is done behind the scenes
    	// by the ComplexContentOutputter

        if (nameCode.equals(StructuredQName.XML_ID)) {
            value = Whitespace.collapseWhitespace(value);
        }
        try {
            out.attribute(nameCode, value);
        } catch (XPathException err) {
            throw dynamicError(getSourceLocator(), err, context);
        }

        //return null;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


