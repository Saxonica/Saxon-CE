package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This class represents the XPath built-in type xs:integer. It is used for all
 * subtypes of xs:integer, other than user-defined subtypes. Unlike other Saxon editions,
 * IntegerValue is implemented as a subclass of DecimalValue. This is because there is
 * no point in the optimisation whereby small integers are mapped to long, since GWT
 * emulates long using two doubles.
 */

public class IntegerValue extends DecimalValue {

    /**
     * IntegerValue representing the value -1
     */
    public static final IntegerValue MINUS_ONE = new IntegerValue(-1);
    /**
     * IntegerValue representing the value zero
     */
    public static final IntegerValue ZERO = new IntegerValue(0);
    /**
     * IntegerValue representing the value +1
     */
    public static final IntegerValue PLUS_ONE = new IntegerValue(+1);

    /**
     * IntegerValue representing the maximum value for a long
     */
    public static final IntegerValue MAX_LONG = new IntegerValue(new BigDecimal(Long.MAX_VALUE));


    public IntegerValue(int value) {
        super(value);
    }

    public IntegerValue(BigDecimal value) {
        super(value);
        if (value.scale()!=0 && value.compareTo(value.setScale(0, BigDecimal.ROUND_DOWN)) != 0) {
            throw new IllegalArgumentException("Non-integral value " + value);
        }
    }
    
    public static ConversionResult decimalToInteger(BigDecimal value) {
    	int setScaleValue = value.setScale(0, BigDecimal.ROUND_DOWN).intValue();
    	return new IntegerValue(setScaleValue);
    }

    /**
     * Static factory method to convert strings to integers.
     * @param s CharSequence representing the string to be converted
     * @return an IntegerValue representing the value of the String, or
     * a ValidationFailure encapsulating an Exception if the value cannot be converted.
     */

    public static ConversionResult stringToInteger(CharSequence s) {
        try {
            String t = Whitespace.trimWhitespace(s).toString();
            if (t.indexOf('.') >= 0) {
                t = "*"; // force a failure
            }
            return new IntegerValue(new BigDecimal(t));
        } catch (NumberFormatException err) {
            return new ValidationFailure("Cannot convert string '" + s + "' to an integer", "FORG0001");
        }
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public AtomicType getItemType() {
        return AtomicType.INTEGER;
    }

    /**
     * Determine whether the value is a whole number, that is, whether it compares
     * equal to some integer
     *
     * @return always true for this implementation
     */

    public boolean isWholeNumber() {
        return true;
    }

    /**
     * Take modulo another integer
     * @param other the other integer
     * @return the result of the modulo operation (the remainder)
     * @throws XPathException if the other integer is zero
     */

    public IntegerValue mod(IntegerValue other) throws XPathException {
        try {
            return new IntegerValue(getDecimalValue().remainder(other.getDecimalValue()));
        } catch (ArithmeticException err) {
            XPathException e;
            if (BigInteger.valueOf(other.intValue()).signum() == 0) {
                e = new XPathException("Integer modulo zero", "FOAR0001");
            } else {
                e = new XPathException("Integer mod operation failure", err);
            }
            throw e;
        }
    }

    @Override
    public int intValue() throws XPathException {
        if (getDecimalValue().compareTo(BIG_DECIMAL_MIN_INT) < 0 ||
               getDecimalValue().compareTo(BIG_DECIMAL_MAX_INT) > 0 ) {
            throw new XPathException("int out of range");
        } else {
            return getDecimalValue().intValue();
        }
    }

    /**
     * Get the absolute value as defined by the XPath abs() function
     * @return the absolute value
     * @since 9.2
     */

    public NumericValue abs() {
        if (getDecimalValue().signum() > 0) {
            return this;
        } else {
            return negate();
        }
    }

    /**
    * Negate the value
    */

    public NumericValue negate() {
        return new IntegerValue(getDecimalValue().negate());
    }

    /**
    * Implement the XPath floor() function
    */

    public NumericValue floor() {
        return this;
    }

    /**
    * Implement the XPath ceiling() function
    */

    public NumericValue ceiling() {
        return this;
    }


    /**
     * Get the signum of an int
     * @param i the int
     * @return -1 if the integer is negative, 0 if it is zero, +1 if it is positive
     */

    protected static int signum(int i) {
        return (i >> 31) | (-i >>> 31);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
