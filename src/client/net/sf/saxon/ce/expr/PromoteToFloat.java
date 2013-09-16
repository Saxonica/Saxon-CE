package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.NumericValue;
import client.net.sf.saxon.ce.value.UntypedAtomicValue;
import client.net.sf.saxon.ce.value.DoubleValue;
import client.net.sf.saxon.ce.trans.XPathException;


/**
 * Expression that performs numeric promotion to xs:double
 */
public class PromoteToFloat extends NumericPromoter {

    public PromoteToFloat(Expression exp) {
        super(exp);
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     */

	public ItemType getItemType() {
        return AtomicType.FLOAT;
	}


    /**
     * Perform the promotion
     * @param value the numeric or untyped atomic value to be promoted
     * @return the value that results from the promotion
     */

    protected AtomicValue promote(AtomicValue value) throws XPathException {
        if (!(value instanceof NumericValue || value instanceof UntypedAtomicValue)) {
            typeError("Cannot promote non-numeric value to xs:float", "XPTY0004");
        }
        if (value instanceof DoubleValue) {
            typeError("Cannot promote from xs:double to xs:float", "XPTY0004");
        }
        return value.convert(AtomicType.FLOAT).asAtomic();
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.