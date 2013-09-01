package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.ArithmeticExpression;
import client.net.sf.saxon.ce.expr.Atomizer;
import client.net.sf.saxon.ce.expr.Token;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;



/**
 * Implementation of the fn:sum function
 */
public class Sum extends Aggregate {

    public Sum newInstance() {
        return new Sum();
    }

    public ItemType getItemType(TypeHierarchy th) {
        ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
        if (base.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            base = BuiltInAtomicType.DOUBLE;
        }
        if (Cardinality.allowsZero(argument[0].getCardinality())) {
            if (argument.length == 1) {
                return Type.getCommonSuperType(base, BuiltInAtomicType.INTEGER, th);
            } else {
                return Type.getCommonSuperType(base, argument[1].getItemType(th), th);
            }
        } else {
            return base;
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue sum = total(argument[0].iterate(context), context, getSourceLocator());
        if (sum != null) {
            return sum;
        } else {
            // the sequence was empty
            if (argument.length == 2) {
                return argument[1].evaluateItem(context);
            } else {
                return IntegerValue.ZERO;
            }
        }
    }

    /**
     * Calculate the total of a sequence.
     * @param iter iterator over the items to be totalled
     * @param context the XPath dynamic context
     * @param location location of the expression in the source for diagnostics
     * @return the total, according to the rules of the XPath sum() function, but returning null
     * if the sequence is empty. (It's then up to the caller to decide what the correct result is
     * for an empty sequence.
    */

    public static AtomicValue total(SequenceIterator iter, XPathContext context, SourceLocator location)
            throws XPathException {
        AtomicValue sum = (AtomicValue)iter.next();
        if (sum == null) {
            // the sequence is empty
            return null;
        }
        if (sum instanceof UntypedAtomicValue) {
            try {
                sum = sum.convert(BuiltInAtomicType.DOUBLE, true).asAtomic();
            } catch (XPathException e) {
                e.maybeSetLocation(location);
                throw e;
            }
        }
        if (sum instanceof NumericValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return sum;
                }
                if (next instanceof UntypedAtomicValue) {
                    next = next.convert(BuiltInAtomicType.DOUBLE, true).asAtomic();
                } else if (!(next instanceof NumericValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                sum = ArithmeticExpression.compute(sum, Token.PLUS, next, context);
                if (sum.isNaN() && sum instanceof DoubleValue) {
                    // take an early bath, once we've got a double NaN it's not going to change
                    return sum;
                }
            }
        } else if (sum instanceof DurationValue) {
            if (!((sum instanceof DayTimeDurationValue) || (sum instanceof YearMonthDurationValue))) {
                XPathException err = new XPathException("Input to sum() contains a duration that is neither a dayTimeDuration nor a yearMonthDuration");
                err.setXPathContext(context);
                err.setErrorCode("FORG0006");
                err.setLocator(location);
                throw err;
            }
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return sum;
                }
                if (!(next instanceof DurationValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                sum = ((DurationValue)sum).add((DurationValue)next);
            }
        } else {
            XPathException err = new XPathException(
                    "Input to sum() contains a value of type " +
                            sum.getPrimitiveType().getDisplayName() +
                            " which is neither numeric, nor a duration");
            err.setXPathContext(context);
            err.setErrorCode("FORG0006");
            err.setLocator(location);
            throw err;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.