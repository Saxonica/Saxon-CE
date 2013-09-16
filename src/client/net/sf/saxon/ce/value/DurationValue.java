package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.functions.Component;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.regex.ARegularExpression;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;

import java.math.BigDecimal;

/**
 * A value of type xs:duration
 */

public class DurationValue extends AtomicValue {

    protected boolean negative = false;
    protected int months = 0;
    protected long seconds = 0;
    protected int microseconds = 0;

    /**
     * Private constructor for internal use
     */

    protected DurationValue() {
    }

    /**
     * Constructor for xs:duration taking the components of the duration. There is no requirement
     * that the values are normalized, for example it is acceptable to specify months=18. The values of
     * the individual components must all be non-negative.
     *
     * @param positive     true if the duration is positive, false if negative. For a negative duration
     *                     the components are all supplied as positive integers (or zero).
     * @param years        the number of years
     * @param months       the number of months
     * @param days         the number of days
     * @param hours        the number of hours
     * @param minutes      the number of minutes
     * @param seconds      the number of seconds
     * @param microseconds the number of microseconds
     * @throws IllegalArgumentException if the size of the duration exceeds implementation-defined
     * limits: specifically, if the total number of months exceeds 2^31, or if the total number
     * of seconds exceeds 2^63.
     */

    public DurationValue(boolean positive, int years, int months, int days,
                         int hours, int minutes, long seconds, int microseconds) {
        negative = !positive;
        if (years < 0 || months < 0 || days < 0 || hours < 0 || minutes < 0 || seconds < 0 || microseconds < 0) {
            throw new IllegalArgumentException("Negative component value");
        }
        if (((double)years)*12 + (double)months > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Duration months limit exceeded");
        }
        if (((double)days)*(24*60*60) + ((double)hours)*(60*60) +
                ((double)minutes)*60 + (double)seconds > Long.MAX_VALUE) {
            throw new IllegalArgumentException("Duration seconds limit exceeded");
        }
        this.months = years*12 + months;
        long h = days * 24 + hours ;
        long m = h * 60 + minutes;
        this.seconds = m * 60 + seconds;
        this.microseconds = microseconds;
        normalizeZeroDuration();
    }

    /**
     * Ensure that a zero duration is considered positive
     */

    protected void normalizeZeroDuration() {
        if (months == 0 && seconds == 0L && microseconds == 0) {
            negative = false;
        }
    }

    /**
     * Static factory method: create a duration value from a supplied string, in
     * ISO 8601 format [-]PnYnMnDTnHnMnS
     *
     * @param s a string in the lexical space of xs:duration
     * @return the constructed xs:duration value, or a {@link ValidationFailure} if the
     *         supplied string is lexically invalid.
     */

    public static ConversionResult makeDuration(CharSequence s) {
        return makeDuration(s, durationPattern1);
    }


    // From XSD 1.1 Part 2:
    //    The expression -?P[0-9]+Y?([0-9]+M)?([0-9]+D)?(T([0-9]+H)?([0-9]+M)?([0-9]+(\.[0-9]+)?S)?)?
    //      matches only strings in which the fields occur in the proper order.
    //    The expression '.*[YMDHS].*' matches only strings in which at least one field occurs.
    //    The expression '.*[^T]' matches only strings in which 'T' is not the final character.

    private static ARegularExpression durationPattern1 =
            ARegularExpression.make("-?P([0-9]+Y)?([0-9]+M)?([0-9]+D)?(T([0-9]+H)?([0-9]+M)?([0-9]+(\\.[0-9]+)?S)?)?");

    private static ARegularExpression durationPattern2 =
            ARegularExpression.make("[YMDHS]");

    protected static ConversionResult makeDuration(CharSequence s, ARegularExpression constrainingPattern) {

        s = Whitespace.trimWhitespace(s);
        if (!constrainingPattern.matches(s)) {
            badDuration("Incorrect format", s);
        }
        if (!durationPattern2.containsMatch(s)) {
            badDuration("No components present", s);
        }
        if (s.charAt(s.length()-1) == 'T') {
            badDuration("No component present after 'T'", s);
        }
        boolean negative = s.charAt(0) == '-';
        boolean inTimePart = false;
        int positionOfDot = -1;
        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int minute = 0;
        int second = 0;
        int micro = 0;
        int part = 0;
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
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
                    part = part*10 + (c - '0');
                    break;
                case 'T':
                    inTimePart = true;
                    break;
                case 'Y':
                    year = part;
                    part = 0;
                    break;
                case 'M':
                    if (inTimePart) {
                        minute = part;
                    } else {
                        month = part;
                    }
                    part = 0;
                    break;
                case 'D':
                    day = part;
                    part = 0;
                    break;
                case 'H':
                    hour = part;
                    part = 0;
                    break;
                case 'S':
                    if (positionOfDot >= 0) {
                        String fraction = (s.subSequence(positionOfDot+1, i).toString() + "000000").substring(0, 6);
                        micro = Integer.parseInt(fraction);
                    } else {
                        second = part;
                    }
                    part = 0;
                    break;
                case '.':
                    second = part;
                    part = 0;
                    positionOfDot = i;
                    break;
                default:
                    // no action
            }

        }

        try {
            return new DurationValue(
                    !negative, year, month, day, hour, minute, second, micro);
        } catch (IllegalArgumentException err) {
            // catch values that exceed limits
            return new ValidationFailure(err.getMessage());
        }
    }


    protected static ValidationFailure badDuration(String msg, CharSequence s) {
        return new ValidationFailure("Invalid duration value '" + s + "' (" + msg + ')', "FORG0001");
    }

    /**
     * Parse a simple unsigned integer
     *
     * @param s the string containing the sequence of digits. No sign or whitespace is allowed.
     * @return the integer. Return -1 if the string is not a sequence of digits or exceeds 2^31
     */

    protected static int simpleInteger(String s) {
        long result = 0;
        if (s == null) {
            return -1;
        }
        int len = s.length();
        if (len == 0) {
            return -1;
        }
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                result = result * 10 + (c - '0');
                if (result > Integer.MAX_VALUE) {
                    return -1;
                }
            } else {
                return -1;
            }
        }
        return (int)result;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public AtomicType getItemType() {
        return AtomicType.DURATION;
    }

    /**
     * Convert to target data type
     *
     *
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or a {@link ValidationFailure} if
     *         the value cannot be converted.
     */

    public ConversionResult convert(AtomicType requiredType) {
        if (requiredType == AtomicType.ANY_ATOMIC || requiredType == AtomicType.DURATION) {
            return this;
        } else if (requiredType == AtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == AtomicType.STRING) {
            return new StringValue(getStringValue());
        } else if (requiredType == AtomicType.YEAR_MONTH_DURATION) {
            return YearMonthDurationValue.fromMonths(months * (negative ? -1 : +1));
        } else if (requiredType == AtomicType.DAY_TIME_DURATION) {
            return new DayTimeDurationValue((negative ? -1 : +1), 0, 0, 0, seconds, microseconds);
        } else {
            return new ValidationFailure("Cannot convert duration to " +
                    requiredType.getDisplayName(), "XPTY0004");
        }
    }

    /**
     * Ask whether the duration is negative (less than zero)
     * @return true if negative
     */

    public boolean isNegative() {
        return negative;
    }

    /**
     * Get the year component
     *
     * @return the number of years in the normalized duration; always positive
     */

    public int getYears() {
        return months / 12;
    }

    /**
     * Get the months component
     *
     * @return the number of months in the normalized duration; always positive
     */

    public int getMonths() {
        return months % 12;
    }

    /**
     * Get the days component
     *
     * @return the number of days in the normalized duration; always positive
     */

    public int getDays() {
//        System.err.println("seconds = " + seconds);
//        System.err.println("minutes = " + seconds / 60L);
//        System.err.println("hours = " + seconds / (60L*60L));
//        System.err.println("days = " + seconds / (24L*60L*60L));
//        System.err.println("days (int) = " + (int)(seconds / (24L*60L*60L)));
        return (int)(seconds / (24L*60L*60L));
    }

    /**
     * Get the hours component
     *
     * @return the number of hours in the normalized duration; always positive
     */

    public int getHours() {
        return (int)(seconds % (24L*60L*60L) / (60L*60L));
    }

    /**
     * Get the minutes component
     *
     * @return the number of minutes in the normalized duration; always positive
     */

    public int getMinutes() {
        return (int)(seconds % (60L*60L) / 60L);
    }

    /**
     * Get the seconds component
     *
     * @return the number of whole seconds in the normalized duration; always positive
     */

    public int getSeconds() {
        return (int)(seconds % 60L);
    }

    /**
     * Get the microseconds component
     *
     * @return the number of microseconds in the normalized duration; always positive
     */

    public int getMicroseconds() {
        return microseconds;
    }

    /**
     * Convert to string
     *
     * @return ISO 8601 representation.
     */

    public CharSequence getPrimitiveStringValue() {

        // Note, Schema does not define a canonical representation. We omit all zero components, unless
        // the duration is zero-length, in which case we output PT0S.

        if (months == 0 && seconds == 0L && microseconds == 0) {
            return "PT0S";
        }

        FastStringBuffer sb = new FastStringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        int years = getYears();
        int months = getMonths();
        int days = getDays();
        int hours = getHours();
        int minutes = getMinutes();
        int seconds = getSeconds();

        sb.append("P");
        if (years != 0) {
            sb.append(years + "Y");
        }
        if (months != 0) {
            sb.append(months + "M");
        }
        if (days != 0) {
            sb.append(days + "D");
        }
        if (hours != 0 || minutes != 0 || seconds != 0 || microseconds != 0) {
            sb.append("T");
        }
        if (hours != 0) {
            sb.append(hours + "H");
        }
        if (minutes != 0) {
            sb.append(minutes + "M");
        }
        if (seconds != 0 || microseconds != 0) {
            if (seconds != 0 && microseconds == 0) {
                sb.append(seconds + "S");
            } else {
                long ms = (seconds * 1000000) + microseconds;
                String mss = ms + "";
                if (seconds == 0) {
                    mss = "0000000" + mss;
                    mss = mss.substring(mss.length() - 7);
                }
                sb.append(mss.substring(0, mss.length() - 6));
                sb.append('.');
                int lastSigDigit = mss.length() - 1;
                while (mss.charAt(lastSigDigit) == '0') {
                    lastSigDigit--;
                }
                sb.append(mss.substring(mss.length() - 6, lastSigDigit + 1));
                sb.append('S');
            }
        }

        return sb;

    }

    /**
     * Get length of duration in seconds, assuming an average length of month. (Note, this defines a total
     * ordering on durations which is different from the partial order defined in XML Schema; XPath 2.0
     * currently avoids defining an ordering at all. But the ordering here is consistent with the ordering
     * of the two duration subtypes in XPath 2.0.)
     *
     * @return the duration in seconds, as a double
     */

    public double getLengthInSeconds() {
        double a = months * (365.242199 / 12.0) * 24 * 60 * 60 + seconds + ((double)microseconds / 1000000);
        return (negative ? -a : a);
    }

    /**
     * Get a component of the normalized value
     */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
        case Component.YEAR:
            int value5 = (negative ? -getYears() : getYears());

            return new IntegerValue(value5);
            case Component.MONTH:
                int value4 = (negative ? -getMonths() : getMonths());

                return new IntegerValue(value4);
            case Component.DAY:
                int value3 = (negative ? -getDays() : getDays());

                return new IntegerValue(value3);
            case Component.HOURS:
                int value2 = (negative ? -getHours() : getHours());

                return new IntegerValue(value2);
            case Component.MINUTES:
                int value1 = (negative ? -getMinutes() : getMinutes());

                return new IntegerValue(value1);
            case Component.SECONDS:
            FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.TINY);
            String ms = ("000000" + microseconds);
            ms = ms.substring(ms.length() - 6);
            sb.append((negative ? "-" : "") + getSeconds() + '.' + ms);
            return (AtomicValue)DecimalValue.makeDecimalValue(sb);
        case Component.WHOLE_SECONDS:
            return new IntegerValue(new BigDecimal(negative ? -seconds : seconds));
        case Component.MICROSECONDS:
            int value = (negative ? -microseconds : microseconds);

            return new IntegerValue(value);
            default:
            throw new IllegalArgumentException("Unknown component for duration: " + component);
        }
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
     * @param collator not used when comparing durations
     * @param implicitTimezone not used when comparing durations
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, int implicitTimezone) {
        return (ordered ? null : this);
    }

    /**
     * Test if the two durations are of equal length.
     *
     * @throws ClassCastException if the other value is not an xs:duration or subtype thereof
     */

    public boolean equals(Object other) {
        if (other instanceof DurationValue) {
            DurationValue d1 = this;
            DurationValue d2 = (DurationValue)other;

            return d1.negative == d2.negative &&
                    d1.months == d2.months &&
                    d1.seconds == d2.seconds &&
                    d1.microseconds == d2.microseconds;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return new Double(getLengthInSeconds()).hashCode();
    }

    /**
     * Add two durations
     *
     * @param other the duration to be added to this one
     * @return the sum of the two durations
     * @throws XPathException if, for example the durations are not both yearMonthDurations or dayTimeDurations
     */

    public DurationValue add(DurationValue other) throws XPathException {
        throw new XPathException("Only subtypes of xs:duration can be added", "XPTY0004");
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     *
     * @return the original duration with its sign reversed, retaining its type
     */

    public DurationValue negate() {
        return new DurationValue(negative, 0, months, 0, 0, 0, seconds, microseconds);
    }

    /**
     * Multiply a duration by a number
     *
     * @param factor the number to multiply by
     * @return the result of the multiplication
     * @throws XPathException for example if the type is wrong or overflow results
     */

    public DurationValue multiply(double factor) throws XPathException {
        throw new XPathException("Only subtypes of xs:duration can be multiplied by a number", "XPTY0004");
    }

    /**
     * Divide a duration by a another duration
     *
     * @param other the duration to divide by
     * @return the result of the division
     * @throws XPathException for example if the type is wrong or overflow results
     */

    public DecimalValue divide(DurationValue other) throws XPathException {
        throw new XPathException("Only subtypes of xs:duration can be divided by another duration", "XPTY0004");
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
