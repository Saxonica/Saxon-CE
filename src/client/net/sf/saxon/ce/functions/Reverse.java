package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceExtent;

/**
* Implement XPath function fn:reverse()
*/

public class Reverse extends SystemFunction {

    public Reverse newInstance() {
        return new Reverse();
    }

    /**
     * Determine the item type of the value returned by the function
     *
     */

    public ItemType getItemType() {
        return argument[0].getItemType();
    }

    public int computeSpecialProperties() {
        int baseProps = argument[0].getSpecialProperties();
        if ((baseProps & StaticProperty.REVERSE_DOCUMENT_ORDER) != 0) {
            return (baseProps &
                (~StaticProperty.REVERSE_DOCUMENT_ORDER)) |
                StaticProperty.ORDERED_NODESET;
        } else if ((baseProps & StaticProperty.ORDERED_NODESET) != 0) {
            return (baseProps &
                (~StaticProperty.ORDERED_NODESET)) |
                StaticProperty.REVERSE_DOCUMENT_ORDER;
        } else {
            return baseProps;
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator forwards = argument[0].iterate(context);
        SequenceExtent extent = SequenceExtent.makeReversed(forwards);
        return extent.iterate();
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
