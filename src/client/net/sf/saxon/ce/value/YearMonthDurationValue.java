package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.regex.ARegularExpression;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;

import java.math.BigDecimal;

/**
 * A value of type xs:yearMonthDuration
 */

public final class YearMonthDurationValue extends DurationValue implements Comparable {

    private static ARegularExpression YMDdurationPattern =
            ARegularExpression.make("-?P([0-9]+Y)?([0-9]+M)?([0-9]+D)?");


    /**
     * Private constructor for internal use
     */

    private YearMonthDurationValue() {
    }

    /**
     * Static factory: create a duration value from a supplied string, in
     * ISO 8601 format [+|-]PnYnM
     *
     * @param s a string in the lexical space of xs:yearMonthDuration.
     * @return either a YearMonthDurationValue, or a ValidationFailure if the string was
     *         not in the lexical space of xs:yearMonthDuration.
     */

    public static ConversionResult makeYearMonthDurationValue(CharSequence s) {
         ConversionResult d = DurationValue.makeDuration(s, YMDdurationPattern);
         if (d instanceof ValidationFailure) {
             return d;
         }
         DurationValue dv = (DurationValue)d;
         return YearMonthDurationValue.fromMonths((dv.getYears()*12 + dv.getMonths()) * (dv.isNegative() ? -1 : +1));
     }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getItemType() {
        return BuiltInAtomicType.YEAR_MONTH_DURATION;
    }

    /**
     * Convert to string
     *
     * @return ISO 8601 representation.
     */

    public CharSequence getPrimitiveStringValue() {

        // The canonical representation has months in the range 0-11

        int y = getYears();
        int m = getMonths();

        FastStringBuffer sb = new FastStringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append('P');
        if (y != 0) {
            sb.append(y + "Y");
        }
        if (m != 0 || y == 0) {
            sb.append(m + "M");
        }
        return sb;

    }

    /**
     * Get the number of months in the duration
     *
     * @return the number of months in the duration
     */

    public int getLengthInMonths() {
        return (months) * (negative ? -1 : +1);
    }

    /**
     * Construct a duration value as a number of months.
     *
     * @param months the number of months (may be negative)
     * @return the corresponding xs:yearMonthDuration value
     */

    public static YearMonthDurationValue fromMonths(int months) {
        YearMonthDurationValue mdv = new YearMonthDurationValue();
        mdv.negative = (months < 0);
        mdv.months = (months < 0 ? -months : months);
        mdv.seconds = 0;
        mdv.microseconds = 0;
        return mdv;
    }

    /**
     * Multiply duration by a number. Also used when dividing a duration by a number
     */

    public DurationValue multiply(double n) throws XPathException {
        if (Double.isNaN(n)) {
            throw new XPathException("Cannot multiply/divide a duration by NaN", "FOCA0005");
        }
        double m = (double)getLengthInMonths();
        double product = n * m;
        if (Double.isInfinite(product) || product > Integer.MAX_VALUE || product < Integer.MIN_VALUE) {
            throw new XPathException("Overflow when multiplying/dividing a duration by a number", "FODT0002");
        }
        return fromMonths((int)Math.round(product));
    }

    /**
     * Find the ratio between two durations
     *
     * @param other the dividend
     * @return the ratio, as a decimal
     * @throws XPathException
     */

    public DecimalValue divide(DurationValue other) throws XPathException {
        if (other instanceof YearMonthDurationValue) {
            BigDecimal v1 = BigDecimal.valueOf(getLengthInMonths());
            BigDecimal v2 = BigDecimal.valueOf(((YearMonthDurationValue)other).getLengthInMonths());
            if (v2.signum() == 0) {
                XPathException err = new XPathException("Divide by zero (durations)");
                err.setErrorCode("FOAR0001");
                throw err;
            }
            return new DecimalValue(v1.divide(v2, 20, BigDecimal.ROUND_HALF_EVEN));
        } else {
            XPathException err = new XPathException("Cannot divide two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Add two year-month-durations
     */

    public DurationValue add(DurationValue other) throws XPathException {
        if (other instanceof YearMonthDurationValue) {
            return fromMonths(getLengthInMonths() +
                    ((YearMonthDurationValue)other).getLengthInMonths());
        } else {
            XPathException err = new XPathException("Cannot add two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     */

    public DurationValue negate() {
        return fromMonths(-getLengthInMonths());
    }

    /**
     * Compare the value to another duration value
     *
     * @param other The other dateTime value
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     *         positive value if this one is the later. For this purpose, dateTime values with an unknown
     *         timezone are considered to be UTC values (the Comparable interface requires
     *         a total ordering).
     * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
     *                            is declared as Object to satisfy the Comparable interface)
     */

    public int compareTo(Object other) {
        if (other instanceof YearMonthDurationValue) {
            return getLengthInMonths() - ((YearMonthDurationValue)other).getLengthInMonths();
        } else {
            throw new ClassCastException("Cannot compare a yearMonthDuration to an object of class "
                    + other.getClass());
        }
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns the value itself. This is modified for types such as
     * xs:duration which allow ordering comparisons in XML Schema, but not in XPath.
     * @param ordered
     * @param collator
     * @param implicitTimezone
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, int implicitTimezone) {
        return this;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
