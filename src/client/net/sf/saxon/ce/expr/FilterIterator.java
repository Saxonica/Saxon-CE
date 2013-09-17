package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.FocusIterator;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.NumericValue;
import client.net.sf.saxon.ce.value.StringValue;

/**
* A FilterIterator filters an input sequence using a filter expression. Note that a FilterIterator
* is not used where the filter is a constant number (PositionFilter is used for this purpose instead),
* so this class does no optimizations for numeric predicates.
*/

public class FilterIterator implements SequenceIterator {

    protected FocusIterator base;
    protected Expression filter;
    protected XPathContext filterContext;

    /**
    * Constructor
    * @param base An iteration of the items to be filtered
    * @param filter The expression defining the filter predicate
    * @param context The context in which the expression is being evaluated
    */

    public FilterIterator(SequenceIterator base, Expression filter,
                            XPathContext context) {
        this.filter = filter;
        filterContext = context.newMinorContext();
        this.base = filterContext.setCurrentIterator(base);
    }

    /**
    * Get the next item if there is one
    */

    public Item next() throws XPathException {
        return getNextMatchingItem();
    }

    /**
     * Get the next item in the base sequence that matches the filter predicate
     * if there is such an item, or null if not.
     * @return the next item that matches the predicate
    */

    protected Item getNextMatchingItem() throws XPathException {
        while (true) {
            Item next = base.next();
            if (next == null) {
                return null;
            }
            if (matches()) {
                return next;
            }
        }
    }

    /**
    * Determine whether the context item matches the filter predicate
     * @return true if the context item matches
    */

    protected boolean matches() throws XPathException {

        // This code is carefully designed to avoid reading more items from the
        // iteration of the filter expression than are absolutely essential.

        // The code is almost identical to the code in ExpressionTool#effectiveBooleanValue
        // except for the handling of a numeric result

        SequenceIterator iterator = filter.iterate(filterContext);
        Item first = iterator.next();
        if (first == null) {
            return false;
        }
        if (first instanceof NodeInfo) {
            return true;
        } else {
            if (iterator.next() != null) {
                ExpressionTool.ebvError("sequence of two or more atomic values");
            }
            if (first instanceof BooleanValue) {
                return ((BooleanValue)first).getBooleanValue();
            } else if (first instanceof StringValue) {
                return (first.getStringValue().length() != 0);
            } else if (first instanceof NumericValue) {
                return ((NumericValue)first).compareTo(base.position()) == 0;
            } else {
                ExpressionTool.ebvError("sequence starting with an atomic value other than a boolean, number, or string");
                return false;
            }
        }
    }

    /**
    * Get another iterator to return the same nodes
    */

    public SequenceIterator getAnother() throws XPathException {
        return new FilterIterator(base.getAnother(), filter, filterContext);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
