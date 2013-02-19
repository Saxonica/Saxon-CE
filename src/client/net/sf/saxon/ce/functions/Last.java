package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.IntegerValue;

/**
* Implement the XPath 2.0 function last()
*/

public class Last extends SystemFunction {

    public Last newInstance() {
        return new Last();
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (contextItemType == null) {
            dynamicError("The context for last() is undefined", "XPDY0002", null);
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        return this;
    }    

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return IntegerValue.makeIntegerValue(c.getLast());
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_LAST;
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
