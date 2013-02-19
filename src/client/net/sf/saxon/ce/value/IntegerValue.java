package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
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
    /**
     * Array of small integer values
     */
    public static final IntegerValue[] SMALL_INTEGERS = {
        new IntegerValue(0),
        new IntegerValue(1),
        new IntegerValue(2),
        new IntegerValue(3),
        new IntegerValue(4),
        new IntegerValue(5),
        new IntegerValue(6),
        new IntegerValue(7),
        new IntegerValue(8),
        new IntegerValue(9),
        new IntegerValue(10),
        new IntegerValue(11),
        new IntegerValue(12),
        new IntegerValue(13),
        new IntegerValue(14),
        new IntegerValue(15),
        new IntegerValue(16),
        new IntegerValue(17),
        new IntegerValue(18),
        new IntegerValue(19),
        new IntegerValue(20)
    };

    public IntegerValue(int value) {
        super(value);
        typeLabel = BuiltInAtomicType.INTEGER;
    }

    public IntegerValue(BigDecimal value) {
        super(value);
        if (value.scale()!=0 && value.compareTo(value.setScale(0, BigDecimal.ROUND_DOWN)) != 0) {
            throw new IllegalArgumentException("non-integral");
        }
        typeLabel = BuiltInAtomicType.INTEGER;
    }
    
    public static ConversionResult decimalToInteger(BigDecimal value) {
    	int setScaleValue = value.setScale(0, BigDecimal.ROUND_DOWN).intValue();
    	return new IntegerValue(setScaleValue);
    }

    /**
     * Get the value of the integer as an int.
     * @return the value as an int: if the value is too large to fit in an int, only the bottom 32 bits are returned.
     */

    public int getIntValue() {
        return getDecimalValue().intValue();
    }

    /**
     * Static factory method to convert strings to integers.
     * @param s CharSequence representing the string to be converted
     * @return an IntegerValue representing the value of the String, or
     * a ValidationFailure encapsulating an Exception if the value cannot be converted.
     */

    public static ConversionResult stringToInteger(CharSequence s) {

        int len = s.length();
        int start = 0;
        int last = len - 1;
        while (start < len && s.charAt(start) <= 0x20) {
            start++;
        }
        while (last > start && s.charAt(last) <= 0x20) {
            last--;
        }
        if (start > last) {
            return numericError("Cannot convert zero-length string to an integer");
        }
        if (last - start < 16) {
            // for short numbers, we do the conversion ourselves, to avoid throwing unnecessary exceptions
            boolean negative = false;
            long value = 0;
            int i=start;
            if (s.charAt(i) == '+') {
                i++;
            } else if (s.charAt(i) == '-') {
                negative = true;
                i++;
            }
            if (i > last) {
                return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) +
                        " to integer: no digits after the sign");
            }
            while (i <= last) {
                char d = s.charAt(i++);
                if (d >= '0' && d <= '9') {
                    value = 10*value + (d-'0');
                } else {
                    return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) + " to an integer");
                }
            }
            return new IntegerValue(new BigDecimal(negative ? -value : value));
        } else {
            // for longer numbers, rely on library routines
            try {
                CharSequence t = Whitespace.trimWhitespace(s);
                if (t.charAt(0) == '+') {
                    t = t.subSequence(1, t.length());
                }
                return new IntegerValue(new BigDecimal(t.toString()));
            } catch (NumberFormatException err) {
                return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) + " to an integer");
            }
        }
    }

    /**
     * Helper method to handle errors converting a string to a number
     * @param message error message
     * @return a ValidationFailure encapsulating an Exception describing the error
     */
    private static ValidationFailure numericError(String message) {
        ValidationFailure err = new ValidationFailure(message);
        err.setErrorCode("FORG0001");
        return err;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.INTEGER;
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

    /**
     * Take modulo another integer
     *
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if the other integer is zero
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


    /**
     * Factory method: allows Int64Value objects to be reused. Note that
     * a value obtained using this method must not be modified to set a type label, because
     * the value is in general shared.
     * @param value the integer value
     * @return an Int64Value with this integer value
     */

    public static IntegerValue makeIntegerValue(int value) {
        if (value <= 20 && value >= 0) {
            return SMALL_INTEGERS[value];
        } else {
            return new IntegerValue(value);
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
