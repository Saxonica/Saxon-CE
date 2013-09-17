package client.net.sf.saxon.ce.js;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;
import com.google.gwt.core.client.JavaScriptObject;


/**
* An XPath item that encapsulates a JavaScript object. Such a value can only be obtained by
* calling an extension function that returns it.
*/

public class JSObjectValue implements Item {

    JavaScriptObject jsObject;

    public JSObjectValue(JavaScriptObject jsObject) {
        this.jsObject = jsObject;
    }

    public JavaScriptObject getJavaScriptObject() {
        return jsObject;
    }

    public String getStringValue() {
        return jsObject.toString();
    }

    public AtomicValue getTypedValue() {
        return new StringValue(getStringValue());
    }

    /**
     * Iterate over the items contained in this value.
     *
     * @return an iterator over the sequence of items
     */
    public UnfailingIterator iterate() {
        return SingletonIterator.makeIterator(this);
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     *
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     *         numbered zero. If n is negative or >= the length of the sequence, returns null.
     */
    public Item itemAt(int n) {
        return (n==0 ? this : null);
    }

    /**
     * Get the length of the sequence
     *
     * @return the number of items in the sequence
     */
    public int getLength() {
        return 1;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

