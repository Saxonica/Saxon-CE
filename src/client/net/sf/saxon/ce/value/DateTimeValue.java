package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.Controller;
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
import java.math.BigInteger;
import java.util.Date;


/**
 * A value of type DateTime
 */

public final class DateTimeValue extends CalendarValue implements Comparable {

    private int year;       // the year as written, +1 for BC years
    private int month;     // the month as written, range 1-12
    private int day;       // the day as written, range 1-31
    private int hour;      // the hour as written (except for midnight), range 0-23
    private int minute;   // the minutes as written, range 0-59
    private int second;   // the seconds as written, range 0-59 (no leap seconds)
    private int microsecond;

    /**
     * Private default constructor
     */

    private DateTimeValue() {
    }

    /**
     * Get the dateTime value representing the nominal
     * date/time of this transformation run. Two calls within the same
     * query or transformation will always return the same answer.
     *
     * @param context the XPath dynamic context. May be null, in which case
     * the current date and time are taken directly from the system clock
     * @return the current xs:dateTime
     */

    public static DateTimeValue getCurrentDateTime(XPathContext context) {
        Controller c;
        if (context == null || (c = context.getController()) == null) {
            // non-XSLT/XQuery environment
            return DateTimeValue.fromJavaDate(new Date());
        } else {
            return c.getCurrentDateTime();
        }
    }

    /**
     * Factory method: create a dateTime value given a Java Date object. The returned dateTime
     * value will always have a timezone, which will always be UTC.
     *
     * @param suppliedDate holds the date and time
     * @return the corresponding xs:dateTime value
     */

    public static DateTimeValue fromJavaDate(Date suppliedDate){
        try {
            long millis = suppliedDate.getTime();
            return EPOCH.add(DayTimeDurationValue.fromMilliseconds(millis));
        } catch (XPathException e) {
            return EPOCH;
        }
    }

    /**
     * Fixed date/time used by Java (and Unix) as the origin of the universe: 1970-01-01
     */

    public static final DateTimeValue EPOCH =
            new DateTimeValue(1970, 1, 1, 0, 0, 0, 0, 0);

    /**
     * Factory method: create a dateTime value given a date and a time.
     *
     * @param date the date
     * @param time the time
     * @return the dateTime with the given components. If either component is null, returns null
     * @throws XPathException if the timezones are both present and inconsistent
     */

    public static DateTimeValue makeDateTimeValue(DateValue date, TimeValue time) throws XPathException {
        if (date == null || time == null) {
            return null;
        }
        int tz1 = date.getTimezoneInMinutes();
        int tz2 = time.getTimezoneInMinutes();
        if (tz1 != NO_TIMEZONE && tz2 != NO_TIMEZONE && tz1 != tz2) {
            throw new XPathException("Supplied date and time are in different timezones", "FORG0008");
        }

        DateTimeValue v = date.toDateTime();
        v.hour = time.getHour();
        v.minute = time.getMinute();
        v.second = time.getSecond();
        v.microsecond = time.getMicrosecond();
        v.setTimezoneInMinutes(Math.max(tz1, tz2));
        return v;
    }

    private static RegExp dateTimePattern =
            RegExp.compile("^\\-?([0-9][0-9][0-9][0-9][0-9]*)-([0-9][0-9])-([0-9][0-9])T([0-2][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]*)?([-+Z].*)?$");

    public static ConversionResult makeDateTimeValue(CharSequence s) {
        String str = s.toString();
        MatchResult match = dateTimePattern.exec(str);
        if (match == null) {
            return badDate("wrong format", str);
        }
        DateTimeValue dt = new DateTimeValue();
        dt.year = DurationValue.simpleInteger(match.getGroup(1));
        if (str.startsWith("-")) {
        	dt.year = dt.year - 1; // no year zero in lexical space for XSD 1.0 - so -1 becomes 0 and -2 becomes -1 etc.
            dt.year = -dt.year;
        }
        dt.month = DurationValue.simpleInteger(match.getGroup(2));
        dt.day = DurationValue.simpleInteger(match.getGroup(3));
        dt.hour = DurationValue.simpleInteger(match.getGroup(4));
        dt.minute = DurationValue.simpleInteger(match.getGroup(5));
        dt.second = DurationValue.simpleInteger(match.getGroup(6));
        String frac = match.getGroup(7);
        if (frac != null && frac.length() > 0) {
            double fractionalSeconds = Double.parseDouble(frac);
            dt.microsecond = (int)(Math.round(fractionalSeconds * 1000000));
        }
        String tz = match.getGroup(8);
        int tzmin = parseTimezone(tz);
        if (tzmin == BAD_TIMEZONE) {
            return badDate("Invalid timezone", str);
        }
        dt.setTimezoneInMinutes(tzmin);
        if (dt.year == 0) {
            return badDate("year zero", str);
        }
        // Check that this is a valid calendar date
        if (!DateValue.isValidDate(dt.year, dt.month, dt.day)) {
            return badDate("Non-existent date", s);
        }
        // Adjust midnight to 00:00 on the following day
        if (dt.hour == 24) {
            if (dt.minute != 0 || dt.second != 0 || dt.microsecond != 0) {
                return badDate("after midnight", str);
            } else {
                dt.hour = 0;
                DateValue tomorrow = DateValue.tomorrow(dt.year, dt.month, dt.day);
                dt.year = tomorrow.getYear();
                dt.month = tomorrow.getMonth();
                dt.day = tomorrow.getDay();
            }
        }
        return dt;
    }


    private static ValidationFailure badDate(String msg, CharSequence value) {
        return new ValidationFailure(
                "Invalid dateTime value " + Err.wrap(value, Err.VALUE) + " (" + msg + ")", "FORG0001");
    }

    /**
     * Constructor: construct a DateTimeValue from its components.
     * This constructor performs no validation.
     *
     * @param year        The year as held internally (note that the year before 1AD is 0)
     * @param month       The month, 1-12
     * @param day         The day 1-31
     * @param hour        the hour value, 0-23
     * @param minute      the minutes value, 0-59
     * @param second      the seconds value, 0-59
     * @param microsecond the number of microseconds, 0-999999
     * @param tz          the timezone displacement in minutes from UTC. Supply the value
     *                    {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     */

    public DateTimeValue(int year, int month, int day,
                         int hour, int minute, int second, int microsecond, int tz) {
        
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.microsecond = microsecond;
        setTimezoneInMinutes(tz);
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public AtomicType getItemType() {
        return AtomicType.DATE_TIME;
    }

    /**
     * Get the year component, in its internal form (which allows a year zero)
     *
     * @return the year component
     */

    public int getYear() {
        return year;
    }

    /**
     * Get the month component, 1-12
     *
     * @return the month component
     */

    public int getMonth() {
        return month;
    }

    /**
     * Get the day component, 1-31
     *
     * @return the day component
     */

    public int getDay() {
        return day;
    }

    /**
     * Get the hour component, 0-23
     *
     * @return the hour component (never 24, even if the input was specified as 24:00:00)
     */

    public int getHour() {
        return hour;
    }

    /**
     * Get the minute component, 0-59
     *
     * @return the minute component
     */

    public int getMinute() {
        return minute;
    }

    /**
     * Get the second component, 0-59
     *
     * @return the second component, excluding fractional seconds
     */

    public int getSecond() {
        return second;
    }

    /**
     * Get the microsecond component, 0-999999
     *
     * @return the microsecond component
     */

    public int getMicrosecond() {
        return microsecond;
    }

    /**
     * Convert the value to a DateTime, retaining all the components that are actually present, and
     * substituting conventional values for components that are missing. (This method does nothing in
     * the case of xs:dateTime, but is there to implement a method in the {@link CalendarValue} interface).
     *
     * @return the value as an xs:dateTime
     */

    public DateTimeValue toDateTime() {
        return this;
    }

    /**
     * Normalize the date and time to be in timezone Z.
     *
     * @param cc used to supply the implicit timezone, used when the value has
     *           no explicit timezone
     * @return in general, a new DateTimeValue in timezone Z, representing the same instant in time.
     *         Returns the original DateTimeValue if this is already in timezone Z.
     */

    public DateTimeValue normalize(XPathContext cc) {
        if (hasTimezone()) {
            return (DateTimeValue)adjustTimezone(0);
        } else {
            DateTimeValue dt = (DateTimeValue) copy();
            dt.setTimezoneInMinutes(cc.getImplicitTimezone());
            return (DateTimeValue)dt.adjustTimezone(0);
        }
    }

    /**
     * Get the Julian instant: a decimal value whose integer part is the Julian day number
     * multiplied by the number of seconds per day,
     * and whose fractional part is the fraction of the second.
     * This method operates on the local time, ignoring the timezone. The caller should call normalize()
     * before calling this method to get a normalized time.
     *
     * @return the Julian instant corresponding to this xs:dateTime value
     */

    public BigDecimal toJulianInstant() {
        int julianDay = DateValue.getJulianDayNumber(year, month, day);
        long julianSecond = julianDay * (24L * 60L * 60L);
        julianSecond += (((hour * 60L + minute) * 60L) + second);
        BigDecimal j = BigDecimal.valueOf(julianSecond);
        if (microsecond == 0) {
            return j;
        } else {
            return j.add(BigDecimal.valueOf(microsecond).divide(DecimalValue.BIG_DECIMAL_ONE_MILLION, 6, BigDecimal.ROUND_HALF_EVEN));
        }
    }

    /**
     * Get the DateTimeValue corresponding to a given Julian instant
     *
     * @param instant the Julian instant: a decimal value whose integer part is the Julian day number
     *                multiplied by the number of seconds per day, and whose fractional part is the fraction of the second.
     * @return the xs:dateTime value corresponding to the Julian instant. This will always be in timezone Z.
     */

    public static DateTimeValue fromJulianInstant(BigDecimal instant) {
        BigInteger julianSecond = instant.toBigInteger();
        BigDecimal microseconds = instant.subtract(new BigDecimal(julianSecond)).multiply(DecimalValue.BIG_DECIMAL_ONE_MILLION);
        long js = julianSecond.longValue();
        long jd = js / (24L * 60L * 60L);
        DateValue date = DateValue.dateFromJulianDayNumber((int)jd);
        js = js % (24L * 60L * 60L);
        byte hour = (byte)(js / (60L * 60L));
        js = js % (60L * 60L);
        byte minute = (byte)(js / (60L));
        js = js % (60L);
        return new DateTimeValue(date.getYear(), date.getMonth(), date.getDay(),
                hour, minute, (byte)js, microseconds.intValue(),0);
    }

    /**
     * Convert to target data type
     *
     *
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convert(AtomicType requiredType) {
        if (requiredType == AtomicType.ANY_ATOMIC || requiredType == AtomicType.DATE_TIME) {
            return this;
        } else if (requiredType == AtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == AtomicType.STRING) {
            return new StringValue(getStringValue());
        } else if (requiredType == AtomicType.DATE) {
            return new DateValue(year, month, day, getTimezoneInMinutes());
        } else if (requiredType == AtomicType.TIME) {
            return new TimeValue(hour, minute, second, microsecond, getTimezoneInMinutes());
        } else if (requiredType == AtomicType.G_YEAR) {
            return new GYearValue(year, getTimezoneInMinutes());
        } else if (requiredType == AtomicType.G_YEAR_MONTH) {
            return new GYearMonthValue(year, month, getTimezoneInMinutes());
        } else if (requiredType == AtomicType.G_MONTH) {
            return new GMonthValue(month, getTimezoneInMinutes());
        } else if (requiredType == AtomicType.G_MONTH_DAY) {
            return new GMonthDayValue(month, day, getTimezoneInMinutes());
        } else if (requiredType == AtomicType.G_DAY) {
            return new GDayValue(day, getTimezoneInMinutes());
        } else {
            return new ValidationFailure("Cannot convert dateTime to " +
                    requiredType.getDisplayName(), "XPTY0004");
        }
    }

    /**
     * Convert to string
     *
     * @return ISO 8601 representation. The value returned is the localized representation,
     *         that is it uses the timezone contained within the value itself.
     */

    public CharSequence getPrimitiveStringValue() {

        FastStringBuffer sb = new FastStringBuffer(30);
        int yr = year;
        if (year <= 0) {
            yr = -yr + 1;    // no year zero in lexical space for XSD 1.0, so zero becomes -1 in string representation
            if(yr!=0){
                sb.append('-');
            }
        }
        appendString(sb, yr, (yr > 9999 ? (yr + "").length() : 4));
        sb.append('-');
        appendTwoDigits(sb, month);
        sb.append('-');
        appendTwoDigits(sb, day);
        sb.append('T');
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
     * Make a copy of this date, time, or dateTime value, but with a new type label
     *
     */

    public AtomicValue copy() {
        return new DateTimeValue(year, month, day,
                hour, minute, second, microsecond, getTimezoneInMinutes());
    }

    /**
     * Return a new dateTime with the same normalized value, but
     * in a different timezone.
     *
     * @param timezone the new timezone offset, in minutes
     * @return the date/time in the new timezone. This will be a new DateTimeValue unless no change
     *         was required to the original value
     */

    public CalendarValue adjustTimezone(int timezone) {
        if (!hasTimezone()) {
            CalendarValue in = (CalendarValue) copy();
            in.setTimezoneInMinutes(timezone);
            return in;
        }
        int oldtz = getTimezoneInMinutes();
        if (oldtz == timezone) {
            return this;
        }
        int tz = timezone - oldtz;
        int h = hour;
        int mi = minute;
        mi += tz;
        if (mi < 0 || mi > 59) {
            h += Math.floor(mi / 60.0);
            mi = (mi + 60 * 24) % 60;
        }

        if (h >= 0 && h < 24) {
            return new DateTimeValue(year, month, day, (byte)h, (byte)mi, second, microsecond, timezone);
        }

        // Following code is designed to handle the corner case of adjusting from -14:00 to +14:00 or
        // vice versa, which can cause a change of two days in the date
        DateTimeValue dt = this;
        while (h < 0) {
            h += 24;
            DateValue t = DateValue.yesterday(dt.getYear(), dt.getMonth(), dt.getDay());
            dt = new DateTimeValue(t.getYear(), t.getMonth(), t.getDay(),
                    (byte)h, (byte)mi, second, microsecond, timezone);
        }
        if (h > 23) {
            h -= 24;
            DateValue t = DateValue.tomorrow(year, month, day);
            return new DateTimeValue(t.getYear(), t.getMonth(), t.getDay(),
                    (byte)h, (byte)mi, second, microsecond, timezone);
        }
        return dt;
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

    public DateTimeValue add(DurationValue duration) throws XPathException {
        if (duration instanceof DayTimeDurationValue) {
            long microseconds = ((DayTimeDurationValue)duration).getLengthInMicroseconds();
            BigDecimal seconds = BigDecimal.valueOf(microseconds).divide(
                    DecimalValue.BIG_DECIMAL_ONE_MILLION, 6, BigDecimal.ROUND_HALF_EVEN);
            BigDecimal julian = toJulianInstant();
            julian = julian.add(seconds);
            DateTimeValue dt = fromJulianInstant(julian);
            dt.setTimezoneInMinutes(getTimezoneInMinutes());
            return dt;
        } else if (duration instanceof YearMonthDurationValue) {
            int months = ((YearMonthDurationValue)duration).getLengthInMonths();
            int m = (month - 1) + months;
            int y = year + m / 12;
            m = m % 12;
            if (m < 0) {
                m += 12;
                y -= 1;
            }
            m++;
            int d = day;
            while (!DateValue.isValidDate(y, m, d)) {
                d -= 1;
            }
            return  new DateTimeValue(y, (byte)m, (byte)d,
                    hour, minute, second, microsecond, getTimezoneInMinutes());
        } else {
            return (DateTimeValue)super.add(duration); // throw type error
        }
    }

    /**
     * Determine the difference between two points in time, as a duration
     *
     * @param other   the other point in time
     * @param context the XPath dynamic context
     * @return the duration as an xs:dayTimeDuration
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          for example if one value is a date and the other is a time
     */

    public DayTimeDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        if (!(other instanceof DateTimeValue)) {
            throw new XPathException("First operand of '-' is a dateTime, but the second is not", "XPTY0004");
        }
        return super.subtract(other, context);
    }


    /**
     * Get a component of the value. Returns null if the timezone component is
     * requested and is not present.
     */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
        case Component.YEAR:
            int value = year > 0 ? year : year - 1;

            return new IntegerValue(value);
            case Component.MONTH:

                return new IntegerValue(month);
            case Component.DAY:

                return new IntegerValue(day);
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
            // internal use only

                return new IntegerValue(microsecond);
            case Component.TIMEZONE:
            if (hasTimezone()) {
                return DayTimeDurationValue.fromMilliseconds(60000L * getTimezoneInMinutes());
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for dateTime: " + component);
        }
    }

    /**
     * Compare the value to another dateTime value, following the XPath comparison semantics
     *
     *
     *
     * @param other  The other dateTime value
     * @param implicitTimezone timezone to be used by default
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     *         positive value if this one is the later. For this purpose, dateTime values with an unknown
     *         timezone are considered to be values in the implicit timezone (the Comparable interface requires
     *         a total ordering).
     * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
     *                            is declared as CalendarValue to satisfy the interface)
     */

    public int compareTo(CalendarValue other, int implicitTimezone) {
        if (!(other instanceof DateTimeValue)) {
            throw new ClassCastException("DateTime values are not comparable to " + other.getClass());
        }
        DateTimeValue v1 = (DateTimeValue)(hasTimezone() ? this : adjustTimezone(implicitTimezone));
        DateTimeValue v2 = (DateTimeValue)other;
        if (!v2.hasTimezone()) {
            v2 = (DateTimeValue)v2.adjustTimezone(implicitTimezone);
        }
        if (v1.getTimezoneInMinutes() != v2.getTimezoneInMinutes()) {
            v1 = (DateTimeValue)v1.adjustTimezone(v2.getTimezoneInMinutes());
        }
        // both values are now in the same timezone (explicitly or implicitly)
        if (v1.year != v2.year) {
            return IntegerValue.signum(v1.year - v2.year);
        }
        if (v1.month != v2.month) {
            return IntegerValue.signum(v1.month - v2.month);
        }
        if (v1.day != v2.day) {
            return IntegerValue.signum(v1.day - v2.day);
        }
        if (v1.hour != v2.hour) {
            return IntegerValue.signum(v1.hour - v2.hour);
        }
        if (v1.minute != v2.minute) {
            return IntegerValue.signum(v1.minute - v2.minute);
        }
        if (v1.second != v2.second) {
            return IntegerValue.signum(v1.second - v2.second);
        }
        if (v1.microsecond != v2.microsecond) {
            return IntegerValue.signum(v1.microsecond - v2.microsecond);
        }
        return 0;
    }

    /**
     * Context-free comparison of two DateTimeValue values. For this to work,
     * the two values must either both have a timezone or both have none.
     * @param v2 the other value
     * @return the result of the comparison: -1 if the first is earlier, 0 if they
     * are equal, +1 if the first is later
     * @throws ClassCastException if the values are not comparable (which might be because
     * no timezone is available)
     */

    public int compareTo(Object v2) {
        try {
            return compareTo((DateTimeValue)v2, 0);
        } catch (Exception err) {
            throw new ClassCastException("DateTime comparison requires access to implicit timezone");
        }
    }

    /**
     * Context-free comparison of two dateTime values
     * @param o the other date time value
     * @return true if the two values represent the same instant in time
     * @throws ClassCastException if one of the values has a timezone and the other does not
     */

    public boolean equals(Object o) {
        return o instanceof DateTimeValue && compareTo(o) == 0;
    }

    /**
     * Hash code for context-free comparison of date time values. Note that equality testing
     * and therefore hashCode() works only for values with a timezone
     * @return  a hash code
     */

    public int hashCode() {
        return hashCode(year, month, day, hour, minute, second, microsecond, getTimezoneInMinutes());
    }

    static int hashCode(int year, int month, int day, int hour, int minute, int second, int microsecond, int tzMinutes) {
        int tz = -tzMinutes;
        int h = hour;
        int mi = minute;
        mi += tz;
        if (mi < 0 || mi > 59) {
            h += Math.floor(mi / 60.0);
            mi = (mi + 60 * 24) % 60;
        }
        while (h < 0) {
            h += 24;
            DateValue t = DateValue.yesterday(year, month, day);
            year = t.getYear();
            month = t.getMonth();
            day = t.getDay();
        }
        while (h > 23) {
            h -= 24;
            DateValue t = DateValue.tomorrow(year, month, day);
            year = t.getYear();
            month = t.getMonth();
            day = t.getDay();
        }
        return (year<<4) ^ (month<<28) ^ (day<<23) ^ (h<<18) ^ (mi<<13) ^ second ^ microsecond;

    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
