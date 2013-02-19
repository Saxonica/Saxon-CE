package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.GroundedIterator;
import client.net.sf.saxon.ce.value.Value;

/**
 * A LastItemExpression returns the last item in the sequence returned by a given
 * base expression. The evaluation strategy is to read the input sequence with a one-item lookahead.
*/

public final class LastItemExpression extends SingleItemFilter {

    /**
    * Constructor
    * @param base A sequence expression denoting sequence whose first item is to be returned
    */

    public LastItemExpression(Expression base) {
        operand = base;
        adoptChildExpression(base);
        computeStaticProperties();
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator forwards = operand.iterate(context);
        if ((forwards.getProperties() & SequenceIterator.GROUNDED) != 0) {
            ValueRepresentation repr = ((GroundedIterator)forwards).materialize();
            Value val = Value.asValue(repr);
            int length = val.getLength();
            return val.itemAt(length - 1);
        } else {
            Item current = null;
            while (true) {
                Item item = forwards.next();
                if (item == null) {
                    return current;
                }
                current = item;
            }
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
