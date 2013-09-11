package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.*;



/**
 * Implementation of the fn:avg function
 */
public class Average extends Aggregate {

    public Average newInstance() {
        return new Average();
    }

    /**
     * Determine the item type of the value returned by the function
     */

    public ItemType getItemType() {
        ItemType base = Atomizer.getAtomizedItemType(argument[0], false);
        if (base == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return BuiltInAtomicType.DOUBLE;
        } else if (base == BuiltInAtomicType.INTEGER) {
            return BuiltInAtomicType.DECIMAL;
        } else {
            return base;
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = argument[0].iterate(context);
        int count = 0;
        AtomicValue item = (AtomicValue) iter.next();
        if (item == null) {
            // the sequence is empty
            return null;
        }
        count++;
        if (item instanceof UntypedAtomicValue) {
            try {
                item = item.convert(BuiltInAtomicType.DOUBLE, true).asAtomic();
            } catch (XPathException e) {
                e.maybeSetLocation(getSourceLocator());
                throw e;
            }
        }
        if (item instanceof NumericValue) {
            while (true) {
                AtomicValue next = (AtomicValue) iter.next();
                if (next == null) {

                    return ArithmeticExpression.compute(item, Token.DIV, new IntegerValue(count), context);
                }
                count++;
                if (next instanceof UntypedAtomicValue) {
                    next = next.convert(BuiltInAtomicType.DOUBLE, true).asAtomic();
                } else if (!(next instanceof NumericValue)) {
                    badMix(context);
                }
                item = ArithmeticExpression.compute(item, Token.PLUS, next, context);
            }
        } else if (item instanceof DurationValue) {
            while (true) {
                AtomicValue next = (AtomicValue) iter.next();
                if (next == null) {
                    return ((DurationValue)item).multiply(1.0/count);
                }
                count++;
                if (!(next instanceof DurationValue)) {
                    badMix(context);
                }
                item = ((DurationValue)item).add((DurationValue)next);
            }
        } else {
            badMix(context);
            return null;
        }
    }

    private void badMix(XPathContext context) throws XPathException {
        dynamicError("Input to avg() contains invalid or mixed data types", "FORG0006");
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
