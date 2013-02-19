package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* A FirstItemExpression returns the first item in the sequence returned by a given
* base expression
*/

public final class FirstItemExpression extends SingleItemFilter {

    /**
    * Constructor
    * @param base A sequence expression denoting sequence whose first item is to be returned
    */

    public FirstItemExpression(Expression base) {
        operand = base;
        adoptChildExpression(base);
        computeStaticProperties();
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        return iter.next();
    }


}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
