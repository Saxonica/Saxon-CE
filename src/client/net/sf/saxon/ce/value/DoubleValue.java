package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;

import java.math.BigDecimal;

/**
 * A numeric (double precision floating point) value
 */

public final class DoubleValue extends NumericValue {

    public static final DoubleValue ZERO = new DoubleValue(0.0);
    public static final DoubleValue NEGATIVE_ZERO = new DoubleValue(-0.0);
    public static final DoubleValue ONE = new DoubleValue(1.0);
    public static final DoubleValue NaN = new DoubleValue(Double.NaN);

    private double value;

    /**
     * Constructor supplying a double
     *
     * @param value the value of the NumericValue
     */

    public DoubleValue(double value) {
        this.value = value;
        typeLabel = BuiltInAtomicType.DOUBLE;
    }

    /**
     * Constructor supplying a double and an AtomicType, for creating
     * a value that belongs to a user-defined subtype of xs:double. It is
     * the caller's responsibility to ensure that the supplied value conforms
     * to the supplied type.
     *
     * @param value the value of the NumericValue
     * @param type  the type of the value. This must be a subtype of xs:double, and the
     *              value must conform to this type. The methosd does not check these conditions.
     */

    public DoubleValue(double value, BuiltInAtomicType type) {
        this.value = value;
        typeLabel = type;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.DOUBLE;
    }

    /**
     * Return this numeric value as a double
     *
     * @return the value as a double
     */

    public double getDoubleValue() {
        return value;
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     *
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int) value;
        } else {
            return new Double(value).hashCode();
        }
    }

    /**
     * Test whether the value is the double/float value NaN
     */

    public boolean isNaN() {
        return Double.isNaN(value);
    }

    /**
     * Get the effective boolean value
     *
     * @return the effective boolean value (true unless the value is zero or NaN)
     */
    public boolean effectiveBooleanValue() {
        return (value != 0.0 && !Double.isNaN(value));
    }

    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @param validate     true if the supplied value must be validated, false if the caller warrants that it is
     *                     valid
     * @return an AtomicValue, a value of the required type
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC ||
                requiredType == BuiltInAtomicType.NUMERIC ||
                requiredType == BuiltInAtomicType.DOUBLE) {
            return this;
        } else if (requiredType == BuiltInAtomicType.BOOLEAN) {
            return BooleanValue.get(effectiveBooleanValue());
        } else if (requiredType == BuiltInAtomicType.INTEGER) {
            if (Double.isNaN(value)) {
                ValidationFailure err = new ValidationFailure("Cannot convert double NaN to an integer");
                err.setErrorCode("FOCA0002");
                return err;
            }
            if (Double.isInfinite(value)) {
                ValidationFailure err = new ValidationFailure("Cannot convert double INF to an integer");
                err.setErrorCode("FOCA0002");
                return err;
            }
            return IntegerValue.decimalToInteger(new BigDecimal(value));
        } else if (requiredType == BuiltInAtomicType.DECIMAL) {
            try {
                return new DecimalValue(value);
            } catch (XPathException e) {
                return new ValidationFailure(e);
            }
        } else if (requiredType == BuiltInAtomicType.FLOAT) {
            return new FloatValue((float) value);
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValue());
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else {
            ValidationFailure err = new ValidationFailure("Cannot convert double to " +
                    requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Convert the double to a string according to the XPath 2.0 rules
     * @return the string value
     */
//    public String getStringValue() {
//        return doubleToString(value).toString(); //, Double.toString(value)).toString();
//    }

    /**
     * Convert the double to a string according to the XPath 2.0 rules
     *
     * @return the string value
     */
    public CharSequence getPrimitiveStringValue() {
        // Note, we were checking for infinity first, and NaN was being displayed as "-INF" by the compiled
        // Javascript, though not in development mode. Re-ordered the tests as a workaround.
        if (Double.isNaN(value)) {
            return "NaN";
        } else if (Double.isInfinite(value)) {
            return (value > 0 ? "INF" : "-INF");
        }
        if (isWholeNumber()) {
            // TODO: negative zero
            return "" + (long) value;
        } else {
            double a = Math.abs(value);
            if (a < 1e6) {
                if (a >= 1e-3) {
                    return Double.toString(value);
                } else if (a >= 1e-6) {
                    return BigDecimal.valueOf(value).toPlainString();
                } else {
                    BigDecimal dec = BigDecimal.valueOf(value);
                    return dec.toString();
                } // see #1545 - code below failed because no E was added (for exponent)
                // but GWT doesn't include the E - it therefore can't be adjusted in the way shown below
            } else if (a < 1e7) {
                // JSNI used because of bug #1545 where GWT developer and production  modes produced different results
                return convertToString(value);
            } else {
                return Double.toString(value);
            }
        }
    }

    public static native String convertToString(double num) /*-{
        var notated = num.toExponential().toString();
        var pos = notated.lastIndexOf('e+');
        if (pos > -1) {
            return notated.substring(0, pos) + 'E' + notated.substring(pos + 2);
        } else {
            return num;
        }

    }-*/;

    /**
     * Negate the value
     */

    public NumericValue negate() {
        return new DoubleValue(-value);
    }

    /**
     * Implement the XPath floor() function
     */

    public NumericValue floor() {
        return new DoubleValue(Math.floor(value));
    }

    /**
     * Implement the XPath ceiling() function
     */

    public NumericValue ceiling() {
        return new DoubleValue(Math.ceil(value));
    }

    /**
     * Implement the XPath round() function
     */

    public NumericValue round() {
        if (Double.isNaN(value)) {
            return this;
        }
        if (Double.isInfinite(value)) {
            return this;
        }
        if (value == 0.0) {
            return this;    // handles the negative zero case
        }
        if (value >= -0.5 && value < 0.0) {
            return new DoubleValue(-0.0);
        }
        if (value > Long.MIN_VALUE && value < Long.MAX_VALUE) {
            return new DoubleValue(Math.round(value));
        }

        // A double holds fewer significant digits than a long. Therefore,
        // if the double is outside the range of a long, it cannot have
        // any signficant digits after the decimal point. So in this
        // case, we return the original value unchanged

        return this;
    }

    /**
     * Implement the XPath round-to-half-even() function
     */

    public NumericValue roundHalfToEven(int scale) {
        if (Double.isNaN(value)) return this;
        if (Double.isInfinite(value)) return this;
        if (value == 0.0) return this;    // handles the negative zero case

        // Convert to a scaled integer, by multiplying by 10^scale

        double factor = Math.pow(10, scale + 1);
        double d = Math.abs(value * factor);

        if (Double.isInfinite(d)) {
            // double arithmetic has overflowed - do it in decimal
            BigDecimal dec = new BigDecimal(value);
            dec = dec.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
            return new DoubleValue(dec.doubleValue());
        }

        // Now apply any rounding needed, using the "round half to even" rule

        double rem = d % 10;
        if (rem > 5) {
            d += (10 - rem);
        } else if (rem < 5) {
            d -= rem;
        } else {
            // round half to even - check the last bit
            if ((d % 20) == 15) {
                d += 5;
            } else {
                d -= 5;
            }
        }

        // Now convert back to the original magnitude

        d /= factor;
        if (value < 0) {
            d = -d;
        }
        return new DoubleValue(d);

    }

    /**
     * Determine whether the value is negative, zero, or positive
     *
     * @return -1 if negative, 0 if zero (including negative zero), +1 if positive, NaN if NaN
     */

    public double signum() {
        if (Double.isNaN(value)) {
            return value;
        }
        if (value > 0) return 1;
        if (value == 0) return 0;
        return -1;
    }

    /**
     * Determine whether the value is a whole number, that is, whether it compares
     * equal to some integer
     */

    public boolean isWholeNumber() {
        return value == Math.floor(value) && !Double.isInfinite(value);
    }

    /**
     * Get the absolute value as defined by the XPath abs() function
     *
     * @return the absolute value
     * @since 9.2
     */

    public NumericValue abs() {
        if (value > 0.0) {
            return this;
        } else {
            return new DoubleValue(Math.abs(value));
        }
    }

    /**
     * Compare the value to a long.
     *
     * @param other the value to be compared with
     * @return -1 if this is less, 0 if this is equal, +1 if this is greater or if this is NaN
     */

    public int compareTo(long other) {
        double otherDouble = (double) other;
        if (value == otherDouble) return 0;
        if (value < otherDouble) return -1;
        return +1;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

