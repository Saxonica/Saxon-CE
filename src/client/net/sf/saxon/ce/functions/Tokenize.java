package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.regex.ARegularExpression;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.value.AtomicValue;


/**
* This class implements the tokenize() function for regular expression matching. This returns a
* sequence of strings representing the unmatched substrings: the separators which match the
* regular expression are not returned.
*/

public class Tokenize extends SystemFunction  {

    public Tokenize newInstance() {
        return new Tokenize();
    }

    /**
    * Iterate over the results of the function
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
        if (sv==null) {
            return EmptyIterator.getInstance();
        }
        String input = sv.getStringValue();
        if (input.length() == 0) {
            return EmptyIterator.getInstance();
        }

        sv = (AtomicValue)argument[1].evaluateItem(c);
        CharSequence pattern = sv.getStringValue();

        CharSequence flags;
        if (argument.length==2) {
            flags = "";
        } else {
            sv = (AtomicValue)argument[2].evaluateItem(c);
            flags = sv.getStringValue();
        }

        try {
            ARegularExpression re = new ARegularExpression(pattern, flags.toString(), "XP20", null);

            // check that it's not a pattern that matches ""
            if (re.matches("")) {
                dynamicError("The regular expression in tokenize() must not be one that matches a zero-length string", "FORX0003");
            }

            return re.tokenize(input);

        } catch (XPathException err) {
            err.setErrorCode("FORX0002");
            err.maybeSetLocation(this.getSourceLocator());
            throw err;
        }




    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.