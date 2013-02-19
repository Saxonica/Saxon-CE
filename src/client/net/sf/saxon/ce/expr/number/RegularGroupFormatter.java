package client.net.sf.saxon.ce.expr.number;

import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * A RegularGroupFormatter is a NumericGroupFormatter that inserts a separator
 * at constant intervals through a number: for example, a comma after every three
 * digits counting from the right.
 */

public class RegularGroupFormatter extends NumericGroupFormatter {
    
    private int groupSize;
    private String groupSeparator;

    /**
     * Create a RegularGroupFormatter
     * @param grpSize the grouping size. If zero, no grouping separators are inserted
     * @param grpSep the grouping separator (normally a single character, but may be a surrogate pair)
     */
    
    public RegularGroupFormatter(int grpSize, String grpSep){
        groupSize = grpSize;
        groupSeparator = grpSep;
    }

    @Override
    public String format(FastStringBuffer value) {
        int [] valueEx = StringValue.expand(value); 
        int [] groupSeparatorVal = StringValue.expand(groupSeparator);
        FastStringBuffer temp = new FastStringBuffer(FastStringBuffer.TINY);
        if (groupSize>0) {
            for (int i=valueEx.length-1,j=0; i>=0; i--, j++) {
                if (j!=0 && (j % groupSize) == 0) {
                    temp.prependWideChar(groupSeparatorVal[0]);
                }
                temp.prependWideChar(valueEx[i]);
            }
            return temp.toString();
        } 
        return value.toString();
    }

    /**
     * Get the grouping separator to be used. If more than one is used, return the last.
     * If no grouping separators are used, return null
     *
     * @return the grouping separator
     */
    @Override
    public String getSeparator() {
        return groupSeparator;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

