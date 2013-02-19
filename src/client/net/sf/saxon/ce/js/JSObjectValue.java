package client.net.sf.saxon.ce.js;

import client.net.sf.saxon.ce.om.Item;
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

    public CharSequence getStringValueCS() {
        return jsObject.toString();
    }

    public AtomicValue getTypedValue() {
        return new StringValue(getStringValue());
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

