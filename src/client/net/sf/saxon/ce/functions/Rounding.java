package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.NumericValue;

/**
* This class supports the ceiling(), floor(), round(), and round-to-half-even() functions,
 * and also the abs() function
*/

public final class Rounding extends SystemFunction {

    public Rounding(int operation) {
        this.operation = operation;
    }

    public Rounding newInstance() {
        return new Rounding(operation);
    }

    public static final int FLOOR = 0;
    public static final int CEILING = 1;
    public static final int ROUND = 2;
    public static final int HALF_EVEN = 3;
    public static final int ABS = 4;

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue val0 = (AtomicValue)argument[0].evaluateItem(context);
        if (val0 == null) {
            return null;
        }
        NumericValue val = (NumericValue)val0;

        switch (operation) {
            case FLOOR:
                return val.floor();
            case CEILING:
                return val.ceiling();
            case ROUND:
                 return val.round();
            case HALF_EVEN:
                int scale = 0;
                if (argument.length==2) {
                    AtomicValue scaleVal0 = (AtomicValue)argument[1].evaluateItem(context);
                    NumericValue scaleVal = (NumericValue)scaleVal0;
                    scale = (int)scaleVal.intValue();
                }
                return val.roundHalfToEven(scale);
            case ABS:
                return val.abs();
            default:
                throw new UnsupportedOperationException("Unknown rounding function");
        }
    }

    /**
     * Determine the cardinality of the function.
     */

    public int computeCardinality() {
        return argument[0].getCardinality();
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
