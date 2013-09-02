package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;
import com.google.gwt.regexp.shared.RegExp;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
* A decimal value
*/

public class DecimalValue extends NumericValue {

    public static final int DIVIDE_PRECISION = 18;

    private BigDecimal value;

    public static final BigDecimal BIG_DECIMAL_ONE_MILLION = BigDecimal.valueOf(1000000);
    public static final BigDecimal BIG_DECIMAL_MAX_INT = BigDecimal.valueOf(Integer.MAX_VALUE);
    public static final BigDecimal BIG_DECIMAL_MIN_INT = BigDecimal.valueOf(Integer.MIN_VALUE);

    public static final DecimalValue ZERO = new DecimalValue(BigDecimal.valueOf(0));
    public static final DecimalValue ONE = new DecimalValue(BigDecimal.valueOf(1));
    public static final DecimalValue TWO = new DecimalValue(BigDecimal.valueOf(2));
    public static final DecimalValue THREE = new DecimalValue(BigDecimal.valueOf(3));

    /**
    * Constructor supplying a BigDecimal
    * @param value the value of the DecimalValue
    */

    public DecimalValue(BigDecimal value) {
    	this.value = stripTrailingZeros(value);
        //this.value = value.stripTrailingZeros();
        //this.value = value; // GWT bug 6110 prevents stripTrailingZeros (2011-03-14)
        typeLabel = BuiltInAtomicType.DECIMAL;
    }
    
    /**
     * 
     * @param value a BigDecimal that may have trailing zeros
     * @return a value that has trailing zeros trimmed
     * Used instead of BigDecimal's own method as this has a
     * GWT bug 6110 reported causing infinite recursion
     */
    
    public static BigDecimal stripTrailingZeros(BigDecimal value) {
    	String str = value.toString();
    	
    	int dotPos  = str.indexOf('.');
    	if (dotPos < 0) {
    		return value;
    	}
    	int expPos = str.indexOf('E');
    	if (expPos > -1) {
    		return value; // e.g. 12.3E+7
    	}
    	int lastZeroPos = -1;
    	char zeroCh = '0';
    	for (int i = str.length() -1; i > -1; i--) {
    		char ch = str.charAt(i);
    		if (ch != zeroCh) break;
    		lastZeroPos = i;
    	}
    	if(lastZeroPos > -1) {
    		if (lastZeroPos - dotPos == 1) lastZeroPos = dotPos;
    		String strippedValue = str.substring(0, lastZeroPos);
    		return new BigDecimal(strippedValue);
    	} else {
    		return value;
    	}
    }

    private static final RegExp decimalPattern = RegExp.compile("(\\-|\\+)?((\\.[0-9]+)|([0-9]+(\\.[0-9]*)?))");

    /**
     * Factory method to construct a DecimalValue from a string
     * @param in the value of the DecimalValue
     * @return the required DecimalValue if the input is valid, or a ValidationFailure encapsulating the error
     * message if not.
     */

    public static ConversionResult makeDecimalValue(CharSequence in) {

        try {
            FastStringBuffer digits = new FastStringBuffer(in.length());
            int scale = 0;
            int state = 0;
            // 0 - in initial whitespace; 1 - after sign
            // 3 - after decimal point; 5 - in final whitespace
            boolean foundDigit = false;
            int len = in.length();
            for (int i=0; i<len; i++) {
                char c = in.charAt(i);
                switch (c) {
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                        if (state != 0) {
                            state = 5;
                        }
                        break;
                    case '+':
                        if (state != 0) {
                            throw new NumberFormatException("unexpected sign");
                        }
                        state = 1;
                        break;
                    case '-':
                        if (state != 0) {
                            throw new NumberFormatException("unexpected sign");
                        }
                        state = 1;
                        digits.append(c);
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        if (state == 0) {
                            state = 1;
                        } else if (state >= 3) {
                            scale++;
                        }
                        if (state == 5) {
                            throw new NumberFormatException("contains embedded whitespace");
                        }
                        digits.append(c);
                        foundDigit = true;
                        break;
                    case '.':
                        if (state == 5) {
                            throw new NumberFormatException("contains embedded whitespace");
                        }
                        if (state >= 3) {
                            throw new NumberFormatException("more than one decimal point");
                        }
                        state = 3;
                        break;
                    default:
                        throw new NumberFormatException("invalid character '" + c + "'");
                }

            }

            if (!foundDigit) {
                throw new NumberFormatException("no digits in value");
            }

            // remove insignificant trailing zeroes
            while (scale > 0) {
                if (digits.charAt(digits.length()-1) == '0') {
                    digits.setLength(digits.length() - 1);
                    scale--;
                } else {
                    break;
                }
            }
            if (digits.length() == 0 || (digits.length() == 1 && digits.charAt(0) == '-')) {
                return DecimalValue.ZERO;
            }
            BigInteger bigInt = new BigInteger(digits.toString());
            BigDecimal bigDec = new BigDecimal(bigInt, scale);
            return new DecimalValue(bigDec);
        } catch (NumberFormatException err) {
            ValidationFailure e = new ValidationFailure(
                    "Cannot convert string " + Err.wrap(Whitespace.trim(in), Err.VALUE) +
                            " to xs:decimal: " + err.getMessage());
            e.setErrorCode("FORG0001");
            return e;
        }                                                                                                            
    }

    /**
     * Test whether a string is castable to a decimal value
     * @param in the string to be tested
     * @return true if the string has the correct format for a decimal
     */

    public static boolean castableAsDecimal(CharSequence in) {
        CharSequence trimmed = Whitespace.trimWhitespace(in);
        return decimalPattern.exec(trimmed.toString()) != null;
    }

    /**
    * Constructor supplying a double
    * @param in the value of the DecimalValue
    */

    public DecimalValue(double in) throws XPathException {
        try {
            BigDecimal d = new BigDecimal(in);
            //value = d.stripTrailingZeros();
            value = d; // GWT bug 6110 prevents stripTrailingZeros (2011-03-14)
        } catch (NumberFormatException err) {
            // Must be a special value such as NaN or infinity
            XPathException e = new XPathException(
                    "Cannot convert double " + Err.wrap(in+"", Err.VALUE) + " to decimal");
            e.setErrorCode("FOCA0002");
            throw e;
        }
        typeLabel = BuiltInAtomicType.DECIMAL;
    }

    /**
    * Constructor supplying a long integer
    * @param in the value of the DecimalValue
    */

    public DecimalValue(long in) {
        value = BigDecimal.valueOf(in);
        typeLabel = BuiltInAtomicType.DECIMAL;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.DECIMAL;
    }


    /**
    * Get the value
    */

    public BigDecimal getDecimalValue() {
        return value;
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        BigDecimal round = value.setScale(0, BigDecimal.ROUND_DOWN);
        long value = round.longValue();
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int)value;
        } else {
            return new Double(getDoubleValue()).hashCode();
        }
    }

    public boolean effectiveBooleanValue() {
        return value.signum() != 0;
    }

    /**
    * Convert to target data type
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC ||
                requiredType == BuiltInAtomicType.NUMERIC ||
                requiredType == BuiltInAtomicType.DECIMAL) {
            return this;
        } else if (requiredType == BuiltInAtomicType.INTEGER) {
            return IntegerValue.decimalToInteger(value);
        } else if (requiredType == BuiltInAtomicType.BOOLEAN) {
            return BooleanValue.get(value.signum()!=0);
        } else if (requiredType == BuiltInAtomicType.DOUBLE) {
            return new DoubleValue(value.doubleValue());
        } else if (requiredType == BuiltInAtomicType.FLOAT) {
            return new DoubleValue(value.floatValue());
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValue());
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue((getStringValue()));
        } else {
            ValidationFailure err = new ValidationFailure("Cannot convert decimal to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

//    public CharSequence getStringValueCS() {
//        return decimalToString(value, new FastStringBuffer(20));
//    }

    /**
    * Get the value as a String
    * @return a String representation of the value
    */

    public CharSequence getPrimitiveStringValue() {
        return decimalToString(value, new FastStringBuffer(FastStringBuffer.TINY));
    }

    /**
     * Convert a decimal value to a string, using the XPath rules for formatting
     * @param value the decimal value to be converted
     * @param fsb the FastStringBuffer to which the value is to be appended
     * @return the supplied FastStringBuffer, suitably populated
     */

    public static FastStringBuffer decimalToString(BigDecimal value, FastStringBuffer fsb) {
        // Can't use the plain BigDecimal#toString() under JDK 1.5 because this produces values like "1E-5".
        // JDK 1.5 offers BigDecimal#toPlainString() which might do the job directly
        int scale = value.scale();
        if (scale == 0) {
            fsb.append(value.toString());
            return fsb;
        } else if (scale < 0) {
            String s = value.abs().unscaledValue().toString();
            if (s.equals("0")) {
                fsb.append('0');
                return fsb;
            }
            //FastStringBuffer sb = new FastStringBuffer(s.length() + (-scale) + 2);
            if (value.signum() < 0) {
                fsb.append('-');
            }
            fsb.append(s);
            for (int i=0; i<(-scale); i++) {
                fsb.append('0');
            }
            return fsb;
        } else {
            String s = value.abs().unscaledValue().toString();
            if (s.equals("0")) {
                fsb.append('0');
                return fsb;
            }
            int len = s.length();
            //FastStringBuffer sb = new FastStringBuffer(len+1);
            if (value.signum() < 0) {
                fsb.append('-');
            }
            if (scale >= len) {
                fsb.append("0.");
                for (int i=len; i<scale; i++) {
                    fsb.append('0');
                }
                fsb.append(s);
            } else {
                fsb.append(s.substring(0, len-scale));
                fsb.append('.');
                fsb.append(s.substring(len-scale));
            }
            return fsb;
        }
    }

    /**
    * Negate the value
    */

    public NumericValue negate() {
        return new DecimalValue(value.negate());
    }

    /**
    * Implement the XPath floor() function
    */

    public NumericValue floor() {
        return new DecimalValue(value.setScale(0, BigDecimal.ROUND_FLOOR));
    }

    /**
    * Implement the XPath ceiling() function
    */

    public NumericValue ceiling() {
        return new DecimalValue(value.setScale(0, BigDecimal.ROUND_CEILING));
    }

    /**
    * Implement the XPath round() function
    */

    public NumericValue round() {
        // The XPath rules say that we should round to the nearest integer, with .5 rounding towards
        // positive infinity. Unfortunately this is not one of the rounding modes that the Java BigDecimal
        // class supports, so we need different rules depending on the value.

        // If the value is positive, we use ROUND_HALF_UP; if it is negative, we use ROUND_HALF_DOWN (here "UP"
        // means "away from zero")

        switch (value.signum()) {
            case -1:
                return new DecimalValue(value.setScale(0, BigDecimal.ROUND_HALF_DOWN));
            case 0:
                return this;
            case +1:
                return new DecimalValue(value.setScale(0, BigDecimal.ROUND_HALF_UP));
            default:
                // can't happen
                return this;
        }

    }

    /**
    * Implement the XPath round-half-to-even() function
    */

    public NumericValue roundHalfToEven(int scale) {
        BigDecimal scaledValue = value.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
        return new DecimalValue(scaledValue);
    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero, +1 if positive, NaN if NaN
     */

    public double signum() {
        return value.signum();
    }

    /**
    * Determine whether the value is a whole number, that is, whether it compares
    * equal to some integer
    */

    public boolean isWholeNumber() {
        return value.scale()==0 ||
               value.compareTo(value.setScale(0, BigDecimal.ROUND_DOWN)) == 0;
    }

    /**
     * Get the absolute value as defined by the XPath abs() function
     * @return the absolute value
     * @since 9.2
     */

    public NumericValue abs() {
        if (value.signum() > 0) {
            return this;
        } else {
            return new DecimalValue(value.negate());
        }
    }

    /**
    * Compare the value to another numeric value
    */

    public int compareTo(Object other) {
        if (other instanceof DecimalValue) {
            // including xs:integer
            return value.compareTo(((DecimalValue)other).value);
        } else if (other instanceof FloatValue) {
            try {
                return ((FloatValue)convertPrimitive(BuiltInAtomicType.FLOAT, true).asAtomic()).compareTo(other);
            } catch (XPathException err) {
                throw new AssertionError("Conversion of decimal to float should never fail");
            }
        } else {
            return super.compareTo(other);
        }
    }

    /**
     * Compare the value to a long
     * @param other the value to be compared with
     * @return -1 if this is less, 0 if this is equal, +1 if this is greater or if this is NaN
     */

    public int compareTo(long other) {
        if (other == 0) {
            return value.signum();
        }
        return value.compareTo(BigDecimal.valueOf(other));
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

