package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;

import java.util.Arrays;

/**
 * A value of type xs:hexBinary
 */

public class HexBinaryValue extends AtomicValue {

    private byte[] binaryValue;


    /**
     * Constructor: create a hexBinary value from a supplied string, in which
     * each octet is represented by a pair of values from 0-9, a-f, A-F
     *
     * @param in character representation of the hexBinary value
     */

    public HexBinaryValue(CharSequence in) throws XPathException {
        CharSequence s = Whitespace.trimWhitespace(in);
        if ((s.length() & 1) != 0) {
            XPathException err = new XPathException("A hexBinary value must contain an even number of characters");
            err.setErrorCode("FORG0001");
            throw err;
        }
        binaryValue = new byte[s.length() / 2];
        for (int i = 0; i < binaryValue.length; i++) {
            binaryValue[i] = (byte)((fromHex(s.charAt(2 * i)) << 4) +
                    (fromHex(s.charAt(2 * i + 1))));
        }
    }

    /**
     * Constructor: create a hexBinary value from a given array of bytes
     *
     * @param value the value as an array of bytes
     */

    public HexBinaryValue(byte[] value) {
        binaryValue = value;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getItemType() {
        return BuiltInAtomicType.HEX_BINARY;
    }

    /**
     * Decode a single hex digit
     *
     * @param c the hex digit
     * @return the numeric value of the hex digit
     * @throws XPathException if it isn't a hex digit
     */

    private int fromHex(char c) throws XPathException {
        int d = "0123456789ABCDEFabcdef".indexOf(c);
        if (d > 15) {
            d = d - 6;
        }
        if (d < 0) {
            XPathException err = new XPathException("Invalid hexadecimal digit");
            err.setErrorCode("FORG0001");
            throw err;
        }
        return d;
    }

    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC || requiredType == BuiltInAtomicType.HEX_BINARY) {
            return this;
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValue());
        } else if (requiredType == BuiltInAtomicType.BASE64_BINARY) {
            return new Base64BinaryValue(binaryValue);
        } else {
            return new ValidationFailure("Cannot convert gYearMonth to " + requiredType.getDisplayName(), "XPTY0004");
        }
    }

    /**
     * Convert to string
     *
     * @return the canonical representation.
     */

    public CharSequence getPrimitiveStringValue() {
        String digits = "0123456789ABCDEF";
        FastStringBuffer sb = new FastStringBuffer(binaryValue.length * 2);
        for (int i = 0; i < binaryValue.length; i++) {
            sb.append(digits.charAt((binaryValue[i] >> 4) & 0xf));
            sb.append(digits.charAt(binaryValue[i] & 0xf));
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
     * @param ordered true if an ordered comparison is required. In this case the result is null if the
     *                type is unordered; in other cases the returned value will be a Comparable.
     * @param collator
     * @param implicitTimezone
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, int implicitTimezone) {
        return (ordered ? null : this);
    }

    /**
     * Test if the two hexBinary or Base64Binaryvalues are equal.
     */

    public boolean equals(Object other) {
        return other instanceof HexBinaryValue && Arrays.equals(binaryValue, ((HexBinaryValue)other).binaryValue);
    }

    public int hashCode() {
        return Base64BinaryValue.byteArrayHashCode(binaryValue);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
