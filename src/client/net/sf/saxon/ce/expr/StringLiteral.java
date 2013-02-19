package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.value.StringValue;

/**
 * Subclass of Literal used specifically for string literals, as this is a common case
 */
public class StringLiteral extends Literal {

    /**
     * Create a StringLiteral that wraps a StringValue
     * @param value the StringValue
     */

    public StringLiteral(StringValue value) {
        super(value);
    }

    /**
     * Create a StringLiteral that wraps any CharSequence (including, of course, a String)
     * @param value the CharSequence to be wrapped
     */

    public StringLiteral(CharSequence value) {
        super(StringValue.makeStringValue(value));
    }

    /**
     * Get the string represented by this StringLiteral
     * @return the underlying string
     */

    public String getStringValue() {
        //noinspection RedundantCast
        return ((StringValue)getValue()).getStringValue();
    }

    private Expression copy() {
        return new StringLiteral((StringValue)getValue());
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
