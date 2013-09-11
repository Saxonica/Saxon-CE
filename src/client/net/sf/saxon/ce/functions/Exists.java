package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.FunctionCall;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.BooleanValue;


/**
 * Implementation of the fn:exists and fn:empty functions
 */
public class Exists extends Aggregate {

    public static final int EXISTS = 2;
    public static final int EMPTY = 3;

    public Exists(int operation) {
        this.operation = operation;
    }

    public Exists newInstance() {
        return new Exists(operation);
    }

    /**
     * Return the negation of the expression
     * @return the negation of the expression
     */

    public Expression negate() {
        FunctionCall fc = SystemFunction.makeSystemFunction(
                (operation == EXISTS ? "empty" : "exists"), getArguments());
        fc.setSourceLocator(getSourceLocator());
        return fc;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the function in a boolean context
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        Item next = argument[0].iterate(c).next();
        return (operation == EXISTS ? next != null : next == null);
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.