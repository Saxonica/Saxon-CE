package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;


/**
 * A value of type Date. Note that a Date may include a TimeZone.
 */

public class DateValue extends GDateValue implements Comparable {

    /**
     * Private constructor of a skeletal DateValue
     */

    private DateValue() {
    }

    /**
     * Constructor given a year, month, and day. Performs no validation.
     *
     * @param year  The year as held internally (note that the year before 1AD is supplied as 0,
     *              but will be displayed on output as -0001)
     * @param month The month, 1-12
     * @param day   The day, 1-31
     */

    public DateValue(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        typeLabel = BuiltInAtomicType.DATE;
    }

    /**
     * Constructor given a year, month, and day, and timezone. Performs no validation.
     *
     * @param year  The year as held internally (note that the year before 1AD is 0)
     * @param month The month, 1-12
     * @param day   The day, 1-31
     * @param tz    the timezone displacement in minutes from UTC. Supply the value
     *              {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     */

    public DateValue(int year, int month, int day, int tz) {
        // Method is called by generated Java code.
        this.year = year;
        this.month = month;
        this.day = day;
        setTimezoneInMinutes(tz);
        typeLabel = BuiltInAtomicType.DATE;
    }

    /**
     * Constructor: create a date value from a supplied string, in
     * ISO 8601 format
     *
     * @param s     the lexical form of the date value
     * @throws XPathException if the supplied string is not a valid date
     */
    public DateValue(CharSequence s) throws XPathException {
        setLexicalValue(this, s).asAtomic();
        typeLabel = BuiltInAtomicType.DATE;
    }

    /**
     * Static factory method: construct a DateValue from a string in the lexical form
     * of a date, returning a ValidationFailure if the supplied string is invalid
     *
     * @param in    the lexical form of the date
     * @return either a DateValue or a ValidationFailure
     */

    public static ConversionResult makeDateValue(CharSequence in) {
        DateValue d = new DateValue();
        d.typeLabel = BuiltInAtomicType.DATE;
        return setLexicalValue(d, in);
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.DATE;
    }

    /**
     * Get the date that immediately follows a given date
     *
     * @param year  the year
     * @param month the month (1-12)
     * @param day   the day (1-31)
     * @return a new DateValue with no timezone information
     */

    public static DateValue tomorrow(int year, int month, int day) {
        if (DateValue.isValidDate(year, month, day + 1)) {
            return new DateValue(year, month, (day + 1));
        } else if (month < 12) {
            return new DateValue(year, (month + 1),  1);
        } else {
            return new DateValue(year + 1, 1, 1);
        }
    }

    /**
     * Get the date that immediately precedes a given date
     *
     * @param year  the year
     * @param month the month (1-12)
     * @param day   the day (1-31)
     * @return a new DateValue with no timezone information
     */

    public static DateValue yesterday(int year, int month, int day) {
        if (day > 1) {
            return new DateValue(year, month, (day - 1));
        } else if (month > 1) {
            if (month == 3 && isLeapYear(year)) {
                return new DateValue(year, 2, 29);
            } else {
                return new DateValue(year, (month - 1), daysPerMonth[month - 2]);
            }
        } else {
            return new DateValue(year - 1, 12, 31);
        }
    }

    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC || requiredType == BuiltInAtomicType.DATE) {
            return this;
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValueCS());
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValueCS());
        } else if (requiredType == BuiltInAtomicType.DATE_TIME) {
            return toDateTime();
        } else if (requiredType == BuiltInAtomicType.G_YEAR) {
            return new GYearValue(year, getTimezoneInMinutes());
        } else if (requiredType == BuiltInAtomicType.G_YEAR_MONTH) {
            return new GYearMonthValue(year, month, getTimezoneInMinutes());
        } else if (requiredType == BuiltInAtomicType.G_MONTH) {
            return new GMonthValue(month, getTimezoneInMinutes());
        } else if (requiredType == BuiltInAtomicType.G_MONTH_DAY) {
            return new GMonthDayValue(month, day, getTimezoneInMinutes());
        } else if (requiredType == BuiltInAtomicType.G_DAY) {
            return new GDayValue(day, getTimezoneInMinutes());
        } else {
            ValidationFailure err = new ValidationFailure("Cannot convert date to " +
                    requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Convert to string
     *
     * @return ISO 8601 representation.
     */

    public CharSequence getPrimitiveStringValue() {

        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.TINY);
        int yr = year;
        if (year <= 0) {
            yr = -yr + 1;           // no year zero in lexical space for XSD 1.0
            if (yr != 0) {
                sb.append('-');
            }
        }
        appendString(sb, yr, (yr > 9999 ? (yr + "").length() : 4));
        sb.append('-');
        appendTwoDigits(sb, month);
        sb.append('-');
        appendTwoDigits(sb, day);

        if (hasTimezone()) {
            appendTimezone(sb);
        }

        return sb;

    }

    /**
     * Get the canonical lexical representation as defined in XML Schema. This is not always the same
     * as the result of casting to a string according to the XPath rules. For xs:date, the timezone is
     * adjusted to be in the range +12:00 to -11:59
     *
     * @return the canonical lexical representation if defined in XML Schema; otherwise, the result
     *         of casting to string according to the XPath 2.0 rules
     */

    public CharSequence getCanonicalLexicalRepresentation() {
        DateValue target = this;
        if (hasTimezone()) {
            if (getTimezoneInMinutes() > 12 * 60) {
                target = (DateValue) adjustTimezone(getTimezoneInMinutes() - 24 * 60);
            } else if (getTimezoneInMinutes() <= -12 * 60) {
                target = (DateValue) adjustTimezone(getTimezoneInMinutes() + 24 * 60);
            }
        }
        return target.getStringValueCS();
    }

    /**
     * Make a copy of this date value, but with a new type label
     *
     * @return the new xs:date value
     */

    public AtomicValue copy() {
        DateValue v = new DateValue(year, month, day, getTimezoneInMinutes());
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Return a new date with the same normalized value, but
     * in a different timezone. This is called only for a DateValue that has an explicit timezone
     *
     * @param timezone the new timezone offset, in minutes
     * @return the time in the new timezone. This will be a new TimeValue unless no change
     *         was required to the original value
     */

    public CalendarValue adjustTimezone(int timezone) {
        DateTimeValue dt = (DateTimeValue) toDateTime().adjustTimezone(timezone);
        return new DateValue(dt.getYear(), dt.getMonth(), dt.getDay(), dt.getTimezoneInMinutes());
    }

    /**
     * Add a duration to a date
     *
     * @param duration the duration to be added (may be negative)
     * @return the new date
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if the duration is an xs:duration, as distinct from
     *          a subclass thereof
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        if (duration instanceof DayTimeDurationValue) {
            long microseconds = ((DayTimeDurationValue) duration).getLengthInMicroseconds();
            boolean negative = (microseconds < 0);
            microseconds = Math.abs(microseconds);
            int days = (int) Math.floor((double) microseconds / (1000000L * 60L * 60L * 24L));
            boolean partDay = (microseconds % (1000000L * 60L * 60L * 24L)) > 0;
            int julian = getJulianDayNumber(year, month, day);
            DateValue d = dateFromJulianDayNumber(julian + (negative ? -days : days));
            if (partDay) {
                if (negative) {
                    d = yesterday(d.year, d.month, d.day);
                }
            }
            d.setTimezoneInMinutes(getTimezoneInMinutes());
            return d;
        } else if (duration instanceof YearMonthDurationValue) {
            int months = ((YearMonthDurationValue) duration).getLengthInMonths();
            int m = (month - 1) + months;
            int y = year + m / 12;
            m = m % 12;
            if (m < 0) {
                m += 12;
                y -= 1;
            }
            m++;
            int d = day;
            while (!isValidDate(y, m, d)) {
                d -= 1;
            }
            return new DateValue(y, (byte) m, (byte) d, getTimezoneInMinutes());
        } else {
            XPathException err = new XPathException("Date arithmetic is not supported on xs:duration, only on its subtypes");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Determine the difference between two points in time, as a duration
     *
     * @param other   the other point in time
     * @param context the XPath dynamic context
     * @return the duration as an xs:dayTimeDuration
     * @throws XPathException for example if one value is a date and the other is a time
     */

    public DayTimeDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        if (!(other instanceof DateValue)) {
            XPathException err = new XPathException("First operand of '-' is a date, but the second is not");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
            throw err;
        }
        return super.subtract(other, context);
    }


    /**
     * Context-free comparison of two DateValue values. For this to work,
     * the two values must either both have a timezone or both have none.
     *
     * @param v2 the other value
     * @return the result of the comparison: -1 if the first is earlier, 0 if they
     *         are equal, +1 if the first is later
     * @throws ClassCastException if the values are not comparable (which might be because
     *                            no timezone is available)
     */

    public int compareTo(Object v2) {
        try {
            return compareTo((DateValue) v2, null);
        } catch (Exception err) {
            throw new ClassCastException("DateTime comparison requires access to implicit timezone");
        }
    }

    /**
     * Calculate the Julian day number at 00:00 on a given date. This algorithm is taken from
     * http://vsg.cape.com/~pbaum/date/jdalg.htm and
     * http://vsg.cape.com/~pbaum/date/jdalg2.htm
     * (adjusted to handle BC dates correctly)
     * <p/>
     * <p>Note that this assumes dates in the proleptic Gregorian calendar</p>
     *
     * @param year  the year
     * @param month the month (1-12)
     * @param day   the day (1-31)
     * @return the Julian day number
     */

    public static int getJulianDayNumber(int year, int month, int day) {
        int z = year - (month < 3 ? 1 : 0);
        short f = monthData[month - 1];
        if (z >= 0) {
            return day + f + 365 * z + z / 4 - z / 100 + z / 400 + 1721118;
        } else {
            // for negative years, add 12000 years and then subtract the days!
            z += 12000;
            int j = day + f + 365 * z + z / 4 - z / 100 + z / 400 + 1721118;
            return j - (365 * 12000 + 12000 / 4 - 12000 / 100 + 12000 / 400);  // number of leap years in 12000 years
        }
    }

    /**
     * Get the Gregorian date corresponding to a particular Julian day number. The algorithm
     * is taken from http://www.hermetic.ch/cal_stud/jdn.htm#comp
     *
     * @param julianDayNumber the Julian day number
     * @return a DateValue with no timezone information set
     */

    public static DateValue dateFromJulianDayNumber(int julianDayNumber) {
        if (julianDayNumber >= 0) {
            int L = julianDayNumber + 68569 + 1;    // +1 adjustment for days starting at noon
            int n = (4 * L) / 146097;
            L = L - (146097 * n + 3) / 4;
            int i = (4000 * (L + 1)) / 1461001;
            L = L - (1461 * i) / 4 + 31;
            int j = (80 * L) / 2447;
            int d = L - (2447 * j) / 80;
            L = j / 11;
            int m = j + 2 - (12 * L);
            int y = 100 * (n - 49) + i + L;
            return new DateValue(y, (byte) m, (byte) d);
        } else {
            // add 12000 years and subtract them again...
            DateValue dt = dateFromJulianDayNumber(julianDayNumber +
                    (365 * 12000 + 12000 / 4 - 12000 / 100 + 12000 / 400));
            dt.year -= 12000;
            return dt;
        }
    }

    /**
     * Get the ordinal day number within the year (1 Jan = 1, 1 Feb = 32, etc)
     *
     * @param year  the year
     * @param month the month (1-12)
     * @param day   the day (1-31)
     * @return the ordinal day number within the year
     */

    public static int getDayWithinYear(int year, int month, int day) {
        int j = getJulianDayNumber(year, month, day);
        int k = getJulianDayNumber(year, 1, 1);
        return j - k + 1;
    }

    /**
     * Get the day of the week.  The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday)
     *
     * @param year  the year
     * @param month the month (1-12)
     * @param day   the day (1-31)
     * @return the day of the week, 1=Monday .... 7=Sunday
     */

    public static int getDayOfWeek(int year, int month, int day) {
        int d = getJulianDayNumber(year, month, day);
        d -= 2378500;   // 1800-01-05 - any Monday would do
        while (d <= 0) {
            d += 70000000;  // any sufficiently high multiple of 7 would do
        }
        return (d - 1) % 7 + 1;
    }

    /**
     * Get the ISO week number for a given date.  The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday), and week 1 in any calendar year is the week (from Monday to Sunday)
     * that includes the first Thursday of that year
     *
     * @param year  the year
     * @param month the month (1-12)
     * @param day   the day (1-31)
     * @return the ISO week number
     */

    public static int getWeekNumber(int year, int month, int day) {
        int d = getDayWithinYear(year, month, day);
        int firstDay = getDayOfWeek(year, 1, 1);
        if (firstDay > 4 && (firstDay + d) <= 8) {
            // days before week one are part of the last week of the previous year (52 or 53)
            return getWeekNumber(year - 1, 12, 31);
        }
        int inc = (firstDay < 5 ? 1 : 0);   // implements the First Thursday rule
        return ((d + firstDay - 2) / 7) + inc;

    }

    /**
     * Get the week number within a month. This is required for the XSLT format-date() function,
     * and the rules are not entirely clear. The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday), and by analogy with the ISO week number, we consider that week 1
     * in any calendar month is the week (from Monday to Sunday) that includes the first Thursday
     * of that month. Unlike the ISO week number, we put the previous days in week zero.
     *
     * @param year  the year
     * @param month the month (1-12)
     * @param day   the day (1-31)
     * @return the week number within a month
     */

    public static int getWeekNumberWithinMonth(int year, int month, int day) {
        int firstDay = getDayOfWeek(year, month, 1);
        int inc = (firstDay < 5 ? 1 : 0);   // implements the First Thursday rule
        return ((day + firstDay - 2) / 7) + inc;
    }

    /**
     * Temporary test rig
     */

//    public static void main(String[] args) throws Exception {
//        DateValue date = new DateValue(args[0]);
//        System.out.println(date.getStringValue());
//        int jd = getJulianDayNumber(date.year,  date.month, date.day);
//        System.out.println(jd);
//        System.out.println(dateFromJulianDayNumber(jd).getStringValue());
//    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
