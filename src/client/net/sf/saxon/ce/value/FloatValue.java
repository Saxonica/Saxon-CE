package client.net.sf.saxon.ce.value;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;

import java.math.BigDecimal;

/**
* A numeric (single precision floating point) value
*/

public final class FloatValue extends NumericValue {

    public static final FloatValue ZERO = new FloatValue((float)0.0);
    public static final FloatValue NEGATIVE_ZERO = new FloatValue((float)-0.0);
    public static final FloatValue ONE = new FloatValue((float)1.0);
    public static final FloatValue NaN = new FloatValue(Float.NaN);

    private float value;

    /**
    * Constructor supplying a float
    * @param value the value of the float
    */

    public FloatValue(float value) {
        this.value = value;
        typeLabel = BuiltInAtomicType.FLOAT;
    }

    /**
     * Constructor supplying a float and an AtomicType, for creating
     * a value that belongs to a user-defined subtype of xs:float. It is
     * the caller's responsibility to ensure that the supplied value conforms
     * to the supplied type.
     * @param value the value of the NumericValue
     * @param type the type of the value. This must be a subtype of xs:float, and the
     * value must conform to this type. The method does not check these conditions.
     */

    public FloatValue(float value, BuiltInAtomicType type) {
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
        return BuiltInAtomicType.FLOAT;
    }

    /**
    * Get the value
    */

    public float getFloatValue() {
        return value;
    }

    public double getDoubleValue() {
        return (double)value;
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int)value;
        } else {
            return new Double(getDoubleValue()).hashCode();
        }
    }

    /**
     * Test whether the value is the double/float value NaN
     */

    public boolean isNaN() {
        return Float.isNaN(value);
    }

    /**
     * Get the effective boolean value
     * @return true unless the value is zero or NaN
     */
    public boolean effectiveBooleanValue() {
        return (value!=0.0 && !Float.isNaN(value));
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC ||
                requiredType == BuiltInAtomicType.NUMERIC ||
                requiredType == BuiltInAtomicType.FLOAT) {
            return this;
        } else if (requiredType == BuiltInAtomicType.BOOLEAN) {
            return BooleanValue.get(effectiveBooleanValue());
        } else if (requiredType == BuiltInAtomicType.INTEGER) {
            if (Float.isNaN(value)) {
                ValidationFailure err = new ValidationFailure("Cannot convert float NaN to an integer");
                err.setErrorCode("FOCA0002");
                return err;
            }
            if (Float.isInfinite(value)) {
                ValidationFailure err = new ValidationFailure("Cannot convert float INF to an integer");
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
        } else if (requiredType == BuiltInAtomicType.DOUBLE) {
            return new DoubleValue(value);
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValue());
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else {
            ValidationFailure err = new ValidationFailure("Cannot convert float to " +
                    requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
    * Get the value as a String
    * @return a String representation of the value
    */
    
    /**
     * Convert the double to a string according to the XPath 2.0 rules
     * @return the string value
     */
    public CharSequence getPrimitiveStringValue() {
        // Same code as for DoubleValue, but using the Float type
        if (Float.isNaN(value)) {
            return "NaN";
        } else if (Float.isInfinite(value)) {
            return (value > 0 ? "INF" : "-INF");
        }
        if (isWholeNumber()) {
            // TODO: negative zero
            return ""+(long)value;
        } else {
            double a = Math.abs(value);
            if (a < 1e6) {
                if (a >= 1e-3) {
                    return Float.toString(value);
                } else if (a >= 1e-6) {
                    return BigDecimal.valueOf(value).toPlainString();
                } else {
                    BigDecimal dec = BigDecimal.valueOf(value);
                    return dec.toString();
                }
            } else if (a < 1e7) {
                FastStringBuffer sb = new FastStringBuffer(Float.toString(value * 10));
                sb.setCharAt(sb.length()-1, (char)((int)sb.charAt(sb.length()-1) - 1));
                return sb;
            } else {
                return Float.toString(value);
            }
        }
    }

    /*// previously used DoubleValue
    public CharSequence getPrimitiveStringValue() {
        return new DoubleValue(value).getPrimitiveStringValue();
    }
    */

    /**
    * Negate the value
    */

    public NumericValue negate() {
        return new FloatValue(-value);
    }

    /**
    * Implement the XPath floor() function
    */

    public NumericValue floor() {
        return new FloatValue((float)Math.floor(value));
    }

    /**
    * Implement the XPath ceiling() function
    */

    public NumericValue ceiling() {
        return new FloatValue((float)Math.ceil(value));
    }

    /**
    * Implement the XPath round() function
    */

    public NumericValue round() {
        if (Float.isNaN(value)) {
            return this;
        }
        if (Float.isInfinite(value)) {
            return this;
        }
        if (value==0.0) {
            return this;    // handles the negative zero case
        }
        if (value >= -0.5 && value < 0.0) {
            return new FloatValue((float)-0.0);
        }
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return new FloatValue((float)Math.round(value));
        }

        // if the float is larger than the maximum int, then
        // it can't have any significant digits after the decimal
        // point, so return it unchanged

        return this;
    }


    /**
    * Implement the XPath round-to-half-even() function
    */

    public NumericValue roundHalfToEven(int scale) {
        try {
            return (FloatValue)
                    new DoubleValue((double)value).roundHalfToEven(scale).convertPrimitive(BuiltInAtomicType.FLOAT, true).asAtomic();
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero (including negative zero), +1 if positive, NaN if NaN
     */

    public double signum() {
        if (Float.isNaN(value)) {
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
        return value == Math.floor(value) && !Float.isInfinite(value);
    }

    /**
     * Get the absolute value as defined by the XPath abs() function
     * @return the absolute value
     * @since 9.2
     */

    public NumericValue abs() {
        if (value > 0.0) {
            return this;
        } else {
            return new FloatValue(Math.abs(value));
        }
    }

    public int compareTo(Object other) {
        if (!(other instanceof NumericValue)) {
            throw new ClassCastException("Numeric values are not comparable to " + other.getClass());
        }
        if (other instanceof FloatValue) {
            float otherFloat = ((FloatValue)other).value;
            if (value == otherFloat) return 0;
            if (value < otherFloat) return -1;
            return +1;
        }
        if (other instanceof DoubleValue) {
            return super.compareTo(other);
        }
        try {
            return compareTo(((NumericValue)other).convertPrimitive(BuiltInAtomicType.FLOAT, true).asAtomic());
        } catch (XPathException err) {
            throw new ClassCastException("Operand of comparison cannot be promoted to xs:float");
        }
    }

    /**
     * Compare the value to a long
     * @param other the value to be compared with
     * @return -1 if this is less, 0 if this is equal, +1 if this is greater or if this is NaN
     */

    public int compareTo(long other) {
        float otherFloat = (float)other;
        if (value == otherFloat) return 0;
        if (value < otherFloat) return -1;
        return +1;
    }

    /**
     * Get an object that implements XML Schema comparison semantics
     */

    private Comparable getSchemaComparable() {
        return new Float(value);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
