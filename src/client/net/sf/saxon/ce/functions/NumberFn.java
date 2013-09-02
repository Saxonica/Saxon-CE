package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * Implements the XPath number() function. 
 */

public class NumberFn extends SystemFunction  {

    public NumberFn newInstance() {
        return new NumberFn();
    }

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault();
        argument[0].setFlattened(true);
        return simplifyArguments(visitor);
    }

    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e2 = super.typeCheck(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        if (argument[0] instanceof NumberFn) {
            // happens through repeated rewriting
            argument[0] = ((NumberFn)argument[0]).argument[0];
        }
        return this;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item arg0 = argument[0].evaluateItem(context);
        if (arg0==null) {
            return DoubleValue.NaN;
        }
        if (arg0 instanceof BooleanValue || arg0 instanceof NumericValue) {
            ConversionResult result = ((AtomicValue)arg0).convert(BuiltInAtomicType.DOUBLE, true);
            if (result instanceof ValidationFailure) {
                return DoubleValue.NaN;
            } else {
                return (AtomicValue)result;
            }
        }
        if (arg0 instanceof StringValue && !(arg0 instanceof AnyURIValue)) {
            CharSequence s = arg0.getStringValue();
            try {
                return new DoubleValue(StringToDouble.stringToNumber(s));
            } catch (NumberFormatException e) {
                return DoubleValue.NaN;
            }
        }
        return DoubleValue.NaN;
    }

    /**
     * Static method to perform the same conversion as the number() function. This is different from the
     * convert(Type.DOUBLE) in that it produces NaN rather than an error for non-numeric operands.
     * @param value the value to be converted
     * @return the result of the conversion
     */

    public static DoubleValue convert(AtomicValue value) {
        try {
            if (value==null) {
                return DoubleValue.NaN;
            }
            if (value instanceof BooleanValue || value instanceof NumericValue) {
                ConversionResult result = value.convert(BuiltInAtomicType.DOUBLE, true);
                if (result instanceof ValidationFailure) {
                    return DoubleValue.NaN;
                } else {
                    return (DoubleValue)result;
                }
            }
            if (value instanceof StringValue && !(value instanceof AnyURIValue)) {
                double d = StringToDouble.stringToNumber(value.getStringValue());
                return new DoubleValue(d);
            }
            return DoubleValue.NaN;
        } catch (NumberFormatException e) {
            return DoubleValue.NaN;
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
