package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

/**
 * Class JsArrayIterator, iterates over a sequence of items held in a Javascript array; it also implements Sequence
 * providing access to the underlying value.
*/

public class JsArrayIterator
        implements UnfailingIterator, GroundedIterator, Sequence {

    int index=0;
    int length;
    Item current = null;
    JsArray list = null;
    Configuration config;

    /**
     * Create a JsArrayIterator over a given List
     * @param list the list: all objects in the list must be instances of {@link Item}
     */

    public JsArrayIterator(JsArray list, Configuration config) {
        index = 0;
        this.list = list;
        this.length = list.length();
        this.config = config;
    }

    public Item next() {
        if (index >= length) {
            current = null;
            index = -1;
            length = -1;
            return null;
        }
        Object obj = getObject(index++, list);
        current = IXSLFunction.convertFromJavaScript(obj, config).next();
        return current;
    }
    
    private final native Object getObject(int index, JsArray jsa) /*-{
       return jsa[index];
    }-*/;
    
    public final native JavaScriptObject getUnderlyingArray() /*-{
       return list;
    }-*/;

    public Item current() {
        return current;
    }

    public int getLastPosition() {
        return length;
    }

    public UnfailingIterator getAnother() {
        return new JsArrayIterator(list, config);
    }

    //@Override
    /**
     * Return an iterator over this sequence.
     *
     * @return the required SequenceIterator, positioned at the start of the
     *     sequence
     */
	public UnfailingIterator iterate() {
		return getAnother();
	}

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator. This should involve no computation, and throws no errors.
     *
     * @return the corresponding Value, or null if the value is not known
     */
    public Sequence materialize() {
        return this;
    }

    /**
     * Get the n'th item in the sequence (starting from 0).
     *
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     *         numbered zero. If n is negative or >= the length of the sequence, returns null.
     */
    public Item itemAt(int n) {
        return IXSLFunction.convertFromJavaScript(getObject(n, list), config).next();
    }

    /**
     * Get the length of the sequence
     *
     * @return the number of items in the sequence
     */
    public int getLength() {
        return length;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
