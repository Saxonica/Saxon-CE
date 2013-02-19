package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;

/**
 * Abstract class for fixed and computed attribute constructor expressions
 */

public abstract class AttributeCreator extends SimpleNodeConstructor {

    private int annotation;
    private int validationAction;
    private int options;

    /**
     * Set the options to be used on the attribute event
     * @param options
     */

    public void setOptions(int options) {
        this.options = options;
    }

    /**
     * Get the options to be used on the attribute event
     * @return the option flags to be used
     */

    public int getOptions() {
        return options;
    }

    /**
     * Set the type annotation fingerprint to be used on the attribute event
     * @param type the fingerprint of the type annotation to be used
     */

    public void setAnnotation(int type) {
        annotation = type;
    }

    /**
     * Get the type annotation fingerprint to be used on the attribute event
     * @return the fingerprint of the type annotation to be used
     */

    public int getAnnotation() {
        return annotation;
    }

    /**
     * Process the value of the node, to create the new node.
     * @param value the string value of the new node
     * @param context the dynamic evaluation context
     * @throws XPathException
     */

    public final void processValue(CharSequence value, XPathContext context) throws XPathException {
        int nameCode = evaluateNameCode(context);
//        if (nameCode == -1) {
//            return null;
//        }
        SequenceReceiver out = context.getReceiver();
        int opt = getOptions();
        int ann = getAnnotation();
        
    	// we may need to change the namespace prefix if the one we chose is
    	// already in use with a different namespace URI: this is done behind the scenes
    	// by the ComplexContentOutputter

        if ((nameCode & NamePool.FP_MASK) == StandardNames.XML_ID) {
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


