package client.net.sf.saxon.ce.value;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;

/**
 * A boolean XPath value
 */

public final class BooleanValue extends AtomicValue implements Comparable {
    private boolean value;

    /**
     * The boolean value TRUE
     */
    public static final BooleanValue TRUE = new BooleanValue(true);
    /**
     * The boolean value FALSE
     */
    public static final BooleanValue FALSE = new BooleanValue(false);

    /**
     * Private Constructor: create a boolean value. Only two instances of this class are
     * ever created, one to represent true and one to represent false.
     * @param value the initial value, true or false
     */

    private BooleanValue(boolean value) {
        this.value = value;
    }

    /**
     * Factory method: get a BooleanValue
     *
     * @param value true or false, to determine which boolean value is
     *     required
     * @return the BooleanValue requested
     */

    public static BooleanValue get(boolean value) {
        return (value ? TRUE : FALSE);
    }

    /**
     * Convert a string to a boolean value, using the XML Schema rules (including
     * whitespace trimming)
     * @param s the input string
     * @return the relevant BooleanValue if validation succeeds; or a ValidationFailure if not.
     */

    public static ConversionResult fromString(CharSequence s) {
        // implementation designed to avoid creating new objects
        s = Whitespace.trimWhitespace(s);
        int len = s.length();
        if (len == 1) {
            char c = s.charAt(0);
            if (c == '1') {
                return TRUE;
            } else if (c == '0') {
                return FALSE;
            }
        } else if (len == 4) {
            if (s.charAt(0) == 't' && s.charAt(1) == 'r' && s.charAt(2) == 'u' && s.charAt(3) == 'e') {
                return TRUE;
            }
        } else if (len == 5) {
            if (s.charAt(0) == 'f' && s.charAt(1) == 'a' && s.charAt(2) == 'l' && s.charAt(3) == 's' && s.charAt(4) == 'e') {
                return FALSE;
            }
        }
        return new ValidationFailure(
                            "The string " + Err.wrap(s, Err.VALUE) + " cannot be cast to a boolean", "FORG0001");
    }

    /**
     * Get the value
     * @return true or false, the actual boolean value of this BooleanValue
     */

    public boolean getBooleanValue() {
        return value;
    }

    /**
     * Get the effective boolean value of this expression
     *
     * @return the boolean value
     */
    public boolean effectiveBooleanValue() {
        return value;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getItemType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC || requiredType == BuiltInAtomicType.BOOLEAN) {
            return this;
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return (value ? StringValue.TRUE : StringValue.FALSE);
        } else if (requiredType == BuiltInAtomicType.NUMERIC || requiredType == BuiltInAtomicType.INTEGER ||
                requiredType == BuiltInAtomicType.DECIMAL) {
            return (value ? IntegerValue.PLUS_ONE : IntegerValue.ZERO);
        } else if (requiredType == BuiltInAtomicType.DOUBLE) {
            return (value ? DoubleValue.ONE : DoubleValue.ZERO);
        } else if (requiredType == BuiltInAtomicType.FLOAT) {
            return (value ? FloatValue.ONE : FloatValue.ZERO);
        } else {
            return new ValidationFailure("Cannot convert boolean to " +
                                     requiredType.getDisplayName(), "XPTY0004");
        }
    }

    /**
     * Convert to string
     * @return "true" or "false"
     */

    public String getPrimitiveStringValue() {
        return (value ? "true" : "false");
    }

    /**
     * Convert to Java object (for passing to external functions)
     *
     * @param target the Java class to which conversion is required
     * @exception XPathException if conversion is not possible or fails
     * @return An object of the specified Java class
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target==Object.class) {
//            return Boolean.valueOf(value);
//        } else if (target.isAssignableFrom(BooleanValue.class)) {
//            return this;
//        } else if (target==boolean.class) {
//            return Boolean.valueOf(value);
//        } else if (target==Boolean.class) {
//            return Boolean.valueOf(value);
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                XPathException err = new XPathException("Conversion of xs:boolean to " + target.getName() +
//                        " is not supported");
//                err.setXPathContext(context);
//                err.setErrorCode(SaxonErrorCode.SXJE0001);
//                throw err;
//            }
//            return o;
//        }
//    }


    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns null. This is overridden for types that allow ordered comparisons in XPath: numeric, boolean,
     * string, date, time, dateTime, yearMonthDuration, dayTimeDuration, and anyURI.
     * @param ordered
     * @param collator
     * @param implicitTimezone
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, int implicitTimezone) {
        return this;
    }

    /**
     * Compare the value to another boolean value
     *
     * @throws ClassCastException if the other value is not a BooleanValue
     *     (the parameter is declared as Object to satisfy the Comparable
     *     interface)
     * @param other The other boolean value
     * @return -1 if this one is the lower, 0 if they are equal, +1 if this
     *     one is the higher. False is considered to be less than true.
     */

    public int compareTo(Object other) {
        if (!(other instanceof BooleanValue)) {
            throw new ClassCastException("Boolean values are not comparable to " + other.getClass());
        }
        if (value == ((BooleanValue)other).value) return 0;
        if (value) return +1;
        return -1;
    }

    /**
     * Determine whether two boolean values are equal
     *
     * @param other the value to be compared to this value
     * @return true if the other value is a boolean value and is equal to this
     *      value
     * @throws ClassCastException if other value is not xs:boolean or derived therefrom
     */
    public boolean equals(Object other) {
        return (other instanceof BooleanValue && value == ((BooleanValue)other).value);
    }

    /**
     * Get a hash code for comparing two BooleanValues
     *
     * @return the hash code
     */
    public int hashCode() {
        return (value ? 0 : 1);
    }

    /**
     * Diagnostic display of this value as a string
     * @return a string representation of this value: "true()" or "false()"
     */
    public String toString() {
        return getStringValue() + "()";
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

