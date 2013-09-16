package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.functions.Component;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import java.math.BigDecimal;

/**
 * A value of type xs:time
 */

public final class TimeValue extends CalendarValue implements Comparable<TimeValue> {

    private int hour;
    private int minute;
    private int second;
    private int microsecond;

    private TimeValue() {
    }

    /**
     * Construct a time value given the hour, minute, second, and microsecond components.
     * This constructor performs no validation.
     *
     * @param hour        the hour value, 0-23
     * @param minute      the minutes value, 0-59
     * @param second      the seconds value, 0-59
     * @param microsecond the number of microseconds, 0-999999
     * @param tz          the timezone displacement in minutes from UTC. Supply the value
     *                    {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     */

    public TimeValue(int hour, int minute, int second, int microsecond, int tz) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.microsecond = microsecond;
        setTimezoneInMinutes(tz);
    }

    /**
     * Static factory method: create a time value from a supplied string, in
     * ISO 8601 format
     *
     * @param s the time in the lexical format hh:mm:ss[.ffffff] followed optionally by
     *          timezone in the form [+-]hh:mm or Z
     * @return either a TimeValue corresponding to the xs:time, or a ValidationFailure
     *         if the supplied value was invalid
     */

    private static RegExp timePattern =
            RegExp.compile("([0-9][0-9]):([0-9][0-9]):([0-9][0-9])(\\.[0-9]*)?([-+Z].*)?");

    public static ConversionResult makeTimeValue(CharSequence s) {
        String str = s.toString();
        MatchResult match = timePattern.exec(str);
        if (match == null) {
            return badTime("wrong format", str);
        }
        TimeValue dt = new TimeValue();
        dt.hour = DurationValue.simpleInteger(match.getGroup(1));
        dt.minute = DurationValue.simpleInteger(match.getGroup(2));
        dt.second = DurationValue.simpleInteger(match.getGroup(3));
        String frac = match.getGroup(4);
        if (frac != null && frac.length() > 0) {
            double fractionalSeconds = Double.parseDouble(frac);
            dt.microsecond = (int)(Math.round(fractionalSeconds * 1000000));
        }
        String tz = match.getGroup(5);
        int tzmin = parseTimezone(tz);
        if (tzmin == BAD_TIMEZONE) {
            return badTime("Invalid timezone", str);
        }
        dt.setTimezoneInMinutes(tzmin);
        // Adjust midnight to 00:00 on the following day
        if (dt.hour == 24) {
            if (dt.minute != 0 || dt.second != 0 || dt.microsecond != 0) {
                return badTime("after midnight", str);
            } else {
                dt.hour = 0;
            }
        }
        return dt;
    }


    private static ValidationFailure badTime(String msg, CharSequence value) {
        return new ValidationFailure(
                "Invalid time " + Err.wrap(value, Err.VALUE) + " (" + msg + ")", "FORG0001");
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public AtomicType getItemType() {
        return AtomicType.TIME;
    }

    /**
     * Get the hour component, 0-23
     *
     * @return the hour
     */

    public int getHour() {
        return hour;
    }

    /**
     * Get the minute component, 0-59
     *
     * @return the minute
     */

    public int getMinute() {
        return minute;
    }

    /**
     * Get the second component, 0-59
     *
     * @return the second
     */

    public int getSecond() {
        return second;
    }

    /**
     * Get the microsecond component, 0-999999
     *
     * @return the microseconds
     */

    public int getMicrosecond() {
        return microsecond;
    }


    /**
     * Convert to target data type
     *
     *
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convert(AtomicType requiredType) {
        if (requiredType == AtomicType.ANY_ATOMIC || requiredType == AtomicType.TIME) {
            return this;
        } else if (requiredType == AtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == AtomicType.STRING) {
            return new StringValue(getStringValue());
        } else {
            return new ValidationFailure("Cannot convert gYear to " + requiredType.getDisplayName(), "XPTY0004");
        }
    }

    /**
     * Convert to string
     *
     * @return ISO 8601 representation, in the localized timezone
     *         (the timezone held within the value).
     */

    public CharSequence getPrimitiveStringValue() {

        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.TINY);

        appendTwoDigits(sb, hour);
        sb.append(':');
        appendTwoDigits(sb, minute);
        sb.append(':');
        appendTwoDigits(sb, second);
        if (microsecond != 0) {
            sb.append('.');
            int ms = microsecond;
            int div = 100000;
            while (ms > 0) {
                int d = ms / div;
                sb.append((char)(d + '0'));
                ms = ms % div;
                div /= 10;
            }
        }

        if (hasTimezone()) {
            appendTimezone(sb);
        }

        return sb;

    }

    /**
     * Convert to a DateTime value. The date components represent a reference date, as defined
     * in the spec for comparing times.
     */

    public DateTimeValue toDateTime() {
        return new DateTimeValue(1972, (byte)12, (byte)31, hour, minute, second, microsecond, getTimezoneInMinutes());
    }


    /**
     * Make a copy of this time value,
     * but with a different type label
     *
     */

    public AtomicValue copy() {
        return new TimeValue(hour, minute, second, microsecond, getTimezoneInMinutes());
    }

    /**
     * Return a new time with the same normalized value, but
     * in a different timezone. This is called only for a TimeValue that has an explicit timezone
     *
     * @param timezone the new timezone offset, in minutes
     * @return the time in the new timezone. This will be a new TimeValue unless no change
     *         was required to the original value
     */

    public CalendarValue adjustTimezone(int timezone) {
        DateTimeValue dt = (DateTimeValue)toDateTime().adjustTimezone(timezone);
        return new TimeValue(dt.getHour(), dt.getMinute(), dt.getSecond(),
                dt.getMicrosecond(), dt.getTimezoneInMinutes());
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target.isAssignableFrom(TimeValue.class)) {
//            return this;
//        } else if (target == String.class) {
//            return getStringValue();
//        } else if (target == Object.class) {
//            return getStringValue();
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                throw new XPathException("Conversion of time to " + target.getName() +
//                        " is not supported");
//            }
//            return o;
//        }
//    }
//
    /**
     * Get a component of the value. Returns null if the timezone component is
     * requested and is not present.
     */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
        case Component.HOURS:

            return new IntegerValue(hour);
            case Component.MINUTES:

                return new IntegerValue(minute);
            case Component.SECONDS:
            BigDecimal d = BigDecimal.valueOf(microsecond);
            d = d.divide(DecimalValue.BIG_DECIMAL_ONE_MILLION, 6, BigDecimal.ROUND_HALF_UP);
            d = d.add(BigDecimal.valueOf(second));
            return new DecimalValue(d);
        case Component.WHOLE_SECONDS: //(internal use only)

            return new IntegerValue(second);
            case Component.MICROSECONDS:

                return new IntegerValue(microsecond);
            case Component.TIMEZONE:
            if (hasTimezone()) {
                return DayTimeDurationValue.fromMilliseconds(60000L * getTimezoneInMinutes());
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for time: " + component);
        }
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns null. This is overridden for types that allow ordered comparisons in XPath: numeric, boolean,
     * string, date, time, dateTime, yearMonthDuration, dayTimeDuration, and anyURI.
     * @param ordered true if an ordered comparison is required
     * @param collator collation to be used for strings
     * @param context XPath dynamic evaluation context
     */

//    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
//        return this;
//    }

    /**
     * Compare the value to another dateTime value
     *
     * @param other The other dateTime value
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     *         positive value if this one is the later. For this purpose, dateTime values with an unknown
     *         timezone are considered to be UTC values (the Comparable interface requires
     *         a total ordering).
     * @throws ClassCastException if the other value is not a TimeValue (the parameter
     *                            is declared as Object to satisfy the Comparable interface)
     */

    public int compareTo(TimeValue other) {
        return toDateTime().compareTo(other.toDateTime());
    }

    /**
     * Compare the value to another dateTime value
     *
     * @param other The other dateTime value
     * @param implicitTimezone
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     *         positive value if this one is the later. For this purpose, dateTime values with an unknown
     *         timezone are considered to be UTC values (the Comparable interface requires
     *         a total ordering).
     * @throws ClassCastException if the other value is not a TimeValue (the parameter
     *                            is declared as Object to satisfy the Comparable interface)
     */

    public int compareTo(CalendarValue other, int implicitTimezone) {
        return toDateTime().compareTo(((TimeValue)other).toDateTime(), implicitTimezone);
    }


    public boolean equals(Object other) {
        return other instanceof TimeValue && compareTo((TimeValue)other) == 0;
    }

    public int hashCode() {
        return DateTimeValue.hashCode(
                1951, (byte)10, (byte)11, hour, minute, second, microsecond, getTimezoneInMinutes());
    }

    /**
     * Add a duration to a dateTime
     *
     * @param duration the duration to be added (may be negative)
     * @return the new date
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if the duration is an xs:duration, as distinct from
     *          a subclass thereof
     */

    public TimeValue add(DurationValue duration) throws XPathException {
        if (duration instanceof DayTimeDurationValue) {
            DateTimeValue dt = toDateTime().add(duration);
            return new TimeValue(dt.getHour(), dt.getMinute(), dt.getSecond(),
                    dt.getMicrosecond(), getTimezoneInMinutes());
        } else {
            return (TimeValue)super.add(duration); // throw type error
        }
    }

    /**
     * Determine the difference between two points in time, as a duration
     *
     * @param other   the other point in time
     * @param context XPath dynamic evaluation context
     * @return the duration as an xs:dayTimeDuration
     * @throws XPathException for example if one value is a date and the other is a time
     */

    public DayTimeDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        if (!(other instanceof TimeValue)) {
            throw new XPathException("First operand of '-' is a time, but the second is not", "XPTY0004");
        }
        return super.subtract(other, context);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
