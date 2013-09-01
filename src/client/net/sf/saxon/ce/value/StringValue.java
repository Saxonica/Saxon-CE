package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.UTF16CharacterSet;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.StringToDouble;
import client.net.sf.saxon.ce.type.ValidationFailure;


/**
 * An atomic value of type xs:string. This class is also used for types derived from xs:string.
 * Subclasses of StringValue are used for xs:untypedAtomic and xs:anyURI values.
 */

public class StringValue extends AtomicValue {

    public static final StringValue EMPTY_STRING = new StringValue("");
    public static final StringValue SINGLE_SPACE = new StringValue(" ");
    public static final StringValue TRUE = new StringValue("true");
    public static final StringValue FALSE = new StringValue("false");

    // We hold the value as a CharSequence (it may be a StringBuffer rather than a string)
    // But the first time this is converted to a string, we keep it as a string

    protected CharSequence value;     // may be zero-length, will never be null
    protected boolean noSurrogates = false;

    /**
     * Protected constructor for use by subtypes
     */

    protected StringValue() {
        value = "";
        typeLabel = BuiltInAtomicType.STRING;
    }

    /**
     * Constructor. Note that although a StringValue may wrap any kind of CharSequence
     * (usually a String, but it can also be, for example, a StringBuffer), the caller
     * is responsible for ensuring that the value is immutable.
     *
     * @param value the String value. Null is taken as equivalent to "".
     */

    public StringValue(CharSequence value) {
        this.value = (value == null ? "" : value);
        typeLabel = BuiltInAtomicType.STRING;
    }

    /**
     * Assert that the string is known to contain no surrogate pairs
     */

    public void setContainsNoSurrogates() {
        noSurrogates = true;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.STRING;
    }

    /**
     * Factory method. Unlike the constructor, this avoids creating a new StringValue in the case
     * of a zero-length string (and potentially other strings, in future)
     *
     * @param value the String value. Null is taken as equivalent to "".
     * @return the corresponding StringValue
     */

    public static StringValue makeStringValue(CharSequence value) {
        if (value == null || value.length() == 0) {
            return StringValue.EMPTY_STRING;
        } else {
            return new StringValue(value);
        }
    }

    /**
     * Get the string value as a String
     */

    public final String getPrimitiveStringValue() {
        return (String) (value = value.toString());
    }

    /**
     * Convert a value to another primitive data type, with control over how validation is
     * handled.
     *
     * @param requiredType type code of the required atomic type. This must not be a namespace-sensitive type.
     * @param validate     true if validation is required. If set to false, the caller guarantees that
     *                     the value is valid for the target data type, and that further validation is therefore not required.
     *                     Note that a validation failure may be reported even if validation was not requested.
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     *         will be a ValidationErrorValue. The caller must check for this condition. No exception is thrown, instead
     *         the exception will be encapsulated within the ErrorValue.
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.STRING || requiredType == BuiltInAtomicType.ANY_ATOMIC) {
            return this;
        }
        return convertStringToBuiltInType(value, requiredType);
    }

    /**
     * Convert a string value to another built-in data type, with control over how validation is
     * handled.
     *
     * @param value        the value to be converted
     * @param requiredType the required atomic type. This must not be a namespace-sensitive type.
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     *         will be a {@link ValidationFailure}. The caller must check for this condition. No exception is thrown, instead
     *         the exception will be encapsulated within the ValidationFailure.
     */

    public static ConversionResult convertStringToBuiltInType(CharSequence value, BuiltInAtomicType requiredType) {
        try {
            if (requiredType == BuiltInAtomicType.BOOLEAN) {
                return BooleanValue.fromString(value);
            } else if (requiredType == BuiltInAtomicType.NUMERIC || requiredType == BuiltInAtomicType.DOUBLE) {
                try {
                    double dbl = StringToDouble.stringToNumber(value);
                    return new DoubleValue(dbl);
                } catch (NumberFormatException err) {
                    ValidationFailure ve = new ValidationFailure("Cannot convert string to double: " + value.toString());
                    ve.setErrorCode("FORG0001");
                    return ve;
                }
            } else if (requiredType == BuiltInAtomicType.INTEGER) {
                return IntegerValue.stringToInteger(value);
            } else if (requiredType == BuiltInAtomicType.DECIMAL) {
                return DecimalValue.makeDecimalValue(value);
            } else if (requiredType == BuiltInAtomicType.FLOAT) {
                try {
                    float flt = (float) StringToDouble.stringToNumber(value);
                    return new FloatValue(flt);
                } catch (NumberFormatException err) {
                    ValidationFailure ve = new ValidationFailure("Cannot convert string to float: " + value.toString());
                    ve.setErrorCode("FORG0001");
                    return ve;
                }
            } else if (requiredType == BuiltInAtomicType.DATE) {
                return DateValue.makeDateValue(value);
            } else if (requiredType == BuiltInAtomicType.DATE_TIME) {
                return DateTimeValue.makeDateTimeValue(value);
            } else if (requiredType == BuiltInAtomicType.TIME) {
                return TimeValue.makeTimeValue(value);
            } else if (requiredType == BuiltInAtomicType.G_YEAR) {
                return GYearValue.makeGYearValue(value);
            } else if (requiredType == BuiltInAtomicType.G_YEAR_MONTH) {
                return GYearMonthValue.makeGYearMonthValue(value);
            } else if (requiredType == BuiltInAtomicType.G_MONTH) {
                return GMonthValue.makeGMonthValue(value);
            } else if (requiredType == BuiltInAtomicType.G_MONTH_DAY) {
                return GMonthDayValue.makeGMonthDayValue(value);
            } else if (requiredType == BuiltInAtomicType.G_DAY) {
                return GDayValue.makeGDayValue(value);
            } else if (requiredType == BuiltInAtomicType.DURATION) {
                return DurationValue.makeDuration(value);
            } else if (requiredType == BuiltInAtomicType.YEAR_MONTH_DURATION) {
                return YearMonthDurationValue.makeYearMonthDurationValue(value);
            } else if (requiredType == BuiltInAtomicType.DAY_TIME_DURATION) {
                return DayTimeDurationValue.makeDayTimeDurationValue(value);
            } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC || requiredType == BuiltInAtomicType.ANY_ATOMIC) {
                return new UntypedAtomicValue(value);
            } else if (requiredType == BuiltInAtomicType.STRING) {
                return makeStringValue(value);
            } else if (requiredType == BuiltInAtomicType.ANY_URI) {
                return new AnyURIValue(value);
            } else if (requiredType == BuiltInAtomicType.HEX_BINARY) {
                return new HexBinaryValue(value);
            } else if (requiredType == BuiltInAtomicType.BASE64_BINARY) {
                return new Base64BinaryValue(value);
            } else {
                ValidationFailure ve = new ValidationFailure("Cannot convert string to type " +
                        Err.wrap(requiredType.getDisplayName()));
                ve.setErrorCode("XPTY0004");
                return ve;
            }
        } catch (XPathException err) {
            err.maybeSetErrorCode("FORG0001");
            ValidationFailure vf = new ValidationFailure(err.getMessage());
            vf.setErrorCodeQName(err.getErrorCodeQName());
            if (vf.getErrorCodeQName() == null) {
                vf.setErrorCode("FORG0001");
            }
            return vf;
        }
    }


    /**
     * Get the length of this string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character
     *
     * @return the length of the string in Unicode code points
     */

    public int getStringLength() {
        if (noSurrogates) {
            return value.length();
        } else {
            int len = getStringLength(value);
            if (len == value.length()) {
                noSurrogates = true;
            }
            return len;
        }
    }

    /**
     * Get the length of a string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character.
     *
     * @param s The string whose length is required
     * @return the length of the string in Unicode code points
     */

    public static int getStringLength(CharSequence s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = (int) s.charAt(i);
            if (c < 55296 || c > 56319) n++;    // don't count high surrogates, i.e. D800 to DBFF
        }
        return n;
    }


    /**
     * Determine whether the string is a zero-length string. This may
     * be more efficient than testing whether the length is equal to zero
     *
     * @return true if the string is zero length
     */

    public boolean isZeroLength() {
        return value.length() == 0;
    }

    /**
     * Determine whether the string contains surrogate pairs
     *
     * @return true if the string contains any non-BMP characters
     */

    public boolean containsSurrogatePairs() {
        //noinspection SimplifiableConditionalExpression
        return (noSurrogates ? false : getStringLength() != value.length());
    }

    /**
     * Ask whether the string is known to contain no surrogate pairs.
     *
     * @return true if it is known to contain no surrogates, false if the answer is not known
     */

    public boolean isKnownToContainNoSurrogates() {
        return noSurrogates;
    }

    /**
     * Expand a string containing surrogate pairs into an array of 32-bit characters
     *
     * @return an array of integers representing the Unicode code points
     */

    public int[] expand() {
        return expand(value);
    }


    /**
     * Expand a string containing surrogate pairs into an array of 32-bit characters
     *
     * @param s the string to be expanded
     * @return an array of integers representing the Unicode code points
     */

    public static int[] expand(CharSequence s) {
        int[] array = new int[getStringLength(s)];
        int o = 0;
        for (int i = 0; i < s.length(); i++) {
            int charval;
            int c = s.charAt(i);
            if (c >= 55296 && c <= 56319) {
                // we'll trust the data to be sound
                charval = ((c - 55296) * 1024) + ((int) s.charAt(i + 1) - 56320) + 65536;
                i++;
            } else {
                charval = c;
            }
            array[o++] = charval;
        }
        return array;
    }

    /**
     * Contract an array of integers containing Unicode codepoints into a Java string
     *
     * @param codes an array of integers representing the Unicode code points
     * @param used  the number of items in the array that are actually used
     * @return the constructed string
     */

    public static CharSequence contract(int[] codes, int used) {
        FastStringBuffer sb = new FastStringBuffer(codes.length);
        for (int i = 0; i < used; i++) {
            if (codes[i] < 65536) {
                sb.append((char) codes[i]);
            } else {  // output a surrogate pair
                sb.append(UTF16CharacterSet.highSurrogate(codes[i]));
                sb.append(UTF16CharacterSet.lowSurrogate(codes[i]));
            }
        }
        return sb;
    }


    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     *
     * @param ordered  true if an ordered comparison is required. In this case the result is null if the
     *                 type is unordered; in other cases the returned value will be a Comparable.
     * @param collator Collation to be used for comparing strings
     * @param context  the XPath dynamic evaluation context, used in cases where the comparison is context
     *                 sensitive
     * @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     *         with respect to this atomic value. If ordered is specified, the result will either be null if
     *         no ordering is defined, or will be a Comparable
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return collator.getCollationKey(value.toString());
    }

    /**
     * Determine if two AtomicValues are equal, according to XPath rules. (This method
     * is not used for string comparisons, which are always under the control of a collation.
     * If we get here, it's because there's a type error in the comparison.)
     *
     * @throws ClassCastException always
     */

    public boolean equals(Object other) {
        throw new ClassCastException("equals on StringValue is not allowed");
    }

    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Test whether this StringValue is equal to another under the rules of the codepoint collation
     *
     * @param other the value to be compared with this value
     * @return true if the strings are equal on a codepoint-by-codepoint basis
     */

    public boolean codepointEquals(StringValue other) {
        // avoid conversion of CharSequence to String if values are different lengths
        return value.length() == other.value.length() &&
                value.toString().equals(other.value.toString());
        // It might be better to do character-by-character comparison in all cases; or it might not.
        // We do it this way in the hope that string comparison compiles to native code.
    }

    /**
     * Get the effective boolean value of a string
     *
     * @return true if the string has length greater than zero
     */

    public boolean effectiveBooleanValue() {
        return value.length() > 0;
    }


    public String toString() {
        return "\"" + value + '\"';
    }

    public static boolean isValidLanguageCode(CharSequence val) {
        String regex = "[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*";
        // See erratum E2-25 to XML Schema Part 2.
        return (val.toString().matches(regex));
    }

    /**
     * Produce a diagnostic representation of the contents of the string
     *
     * @param s the string
     * @return a string in which non-Ascii-printable characters are replaced by \ uXXXX escapes
     */

    public static String diagnosticDisplay(String s) {
        FastStringBuffer fsb = new FastStringBuffer(s.length());
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c <= 0x7e) {
                fsb.append(c);
            } else {
                fsb.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) {
                    fsb.append("0123456789ABCDEF".charAt((c >> shift) & 0xF));
                }
            }
        }
        return fsb.toString();
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

