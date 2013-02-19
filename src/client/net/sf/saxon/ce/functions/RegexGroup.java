package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.regex.RegexIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.NumericValue;
import client.net.sf.saxon.ce.value.StringValue;


public class RegexGroup extends SystemFunction {

    public RegexGroup newInstance() {
        return new RegexGroup();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue gp0 = (AtomicValue)argument[0].evaluateItem(c);
        NumericValue gp = (NumericValue)gp0;
        RegexIterator iter = c.getCurrentRegexIterator();
        if (iter == null) {
            return StringValue.EMPTY_STRING;
        }
        String s = iter.getRegexGroup(gp.intValue());
        if (s == null) {
            return StringValue.EMPTY_STRING;
        }
        return StringValue.makeStringValue(s);
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
       return StaticProperty.DEPENDS_ON_REGEX_GROUP;
    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
