package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.functions.Component;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;


/**
 * Abstract superclass for the primitive types containing date components: xs:date, xs:gYear,
 * xs:gYearMonth, xs:gMonth, xs:gMonthDay, xs:gDay
 */
public abstract class GDateValue extends CalendarValue {
    protected int year;         // unlike the lexical representation, includes a year zero
    protected int month;
    protected int day;
    /**
     * Test whether a candidate date is actually a valid date in the proleptic Gregorian calendar
     */

    protected static byte[] daysPerMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    protected static final short[] monthData = {306, 337, 0, 31, 61, 92, 122, 153, 184, 214, 245, 275};

    /**
     * Get the year component of the date (in local form)
     * @return the year component, as represented internally (allowing a year zero)
     */

    public int getYear() {
        return year;
    }

    /**
     * Get the month component of the date (in local form)
     * @return the month component (1-12)
     */

    public int getMonth() {
        return month;
    }

    /**
     * Get the day component of the date (in local form)
     * @return the day component (1-31)
     */

    public int getDay() {
        return day;
    }

    private static RegExp datePattern =
            RegExp.compile("\\-?([0-9]+)-([0-9][0-9])-([0-9][0-9])([-+Z].*)?");

    protected static ConversionResult setLexicalValue(GDateValue dt, CharSequence s) {
        String str = s.toString();
        MatchResult match = datePattern.exec(str);
        if (match == null) {
            return badDate("wrong format", str);
        }
        dt.year = DurationValue.simpleInteger(match.getGroup(1));
        if (str.startsWith("-")) {
        	dt.year = dt.year - 1; // no year zero in lexical space for XSD 1.0 - so -1 becomes 0 and -2 becomes -1 etc.
            dt.year = -dt.year; 
        }
        dt.month = DurationValue.simpleInteger(match.getGroup(2));
        dt.day = DurationValue.simpleInteger(match.getGroup(3));
        String tz = match.getGroup(4);
        int tzmin = parseTimezone(tz);
        if (tzmin == BAD_TIMEZONE) {
            return badDate("invalid timezone", str);
        }
        dt.setTimezoneInMinutes(tzmin);
        if (dt.year == 0) {
            return badDate("year zero", str);
        }
        // Check that this is a valid calendar date
        if (!DateValue.isValidDate(dt.year, dt.month, dt.day)) {
            return badDate("non-existent date", s);
        }
        return dt;
    }


    private static ValidationFailure badDate(String msg, CharSequence value) {
        return new ValidationFailure(
                "Invalid date " + Err.wrap(value, Err.VALUE) + " (" + msg + ")", "FORG0001");
    }

    /**
     * Determine whether a given date is valid
     * @param year the year (permitting year zero)
     * @param month the month (1-12)
     * @param day the day (1-31)
     * @return true if this is a valid date
     */

    public static boolean isValidDate(int year, int month, int day) {
        return month > 0 && month <= 12 && day > 0 && day <= daysPerMonth[month - 1]
                || month == 2 && day == 29 && isLeapYear(year);
    }

    /**
     * Test whether a year is a leap year
     * @param year the year (permitting year zero)
     * @return true if the supplied year is a leap year
     */

    public static boolean isLeapYear(int year) {
        return (year % 4 == 0) && !(year % 100 == 0 && !(year % 400 == 0));
    }

    /**
     * The equals() methods on atomic values is defined to follow the semantics of eq when applied
     * to two atomic values. When the other operand is not an atomic value, the result is undefined
     * (may be false, may be an exception). When the other operand is an atomic value that cannot be
     * compared with this one, the method returns false.
     * <p/>
     * <p>The hashCode() method is consistent with equals().</p>
     *
     * <p>This implementation performs a context-free comparison: it fails with ClassCastException
     * if one value has a timezone and the other does not.</p>
     *
     * @param o the other value
     * @return true if the other operand is an atomic value and the two values are equal as defined
     *         by the XPath eq operator
     * @throws ClassCastException if the values are not comparable
     */

    public boolean equals(Object o) {
        if (o instanceof GDateValue) {
            GDateValue gdv = (GDateValue)o;
            return getItemType() == gdv.getItemType() && toDateTime().equals(gdv.toDateTime());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return DateTimeValue.hashCode(year, month, day, 12, 0, 0, 0, getTimezoneInMinutes());
    }

    /**
     * Compare this value to another value of the same type, using the supplied context object
     * to get the implicit timezone if required. This method implements the XPath comparison semantics.
     *
     *
     * @param other the value to be compared
     * @param implicitTimezone timezone to be used if there is none in the value
     * @return -1 if this value is less, 0 if equal, +1 if greater
     */

    public int compareTo(CalendarValue other, int implicitTimezone) {
        if (getItemType() != other.getItemType()) {
            throw new ClassCastException("Cannot compare dates of different types");
            // covers, for example, comparing a gYear to a gYearMonth
        }
        return toDateTime().compareTo(other.toDateTime(), implicitTimezone);
    }

    /**
     * Convert to DateTime.
     * @return the starting instant of the GDateValue (with the same timezone)
     */

    public DateTimeValue toDateTime() {
        return new DateTimeValue(year, month, day, (byte)0, (byte)0, (byte)0, 0, getTimezoneInMinutes());
    }

    /**
     * Return a new date, time, or dateTime with the same normalized value, but
     * in a different timezone
     *
     * @param tz the new timezone offset from UTC, in minutes
     * @return the date/time in the new timezone
     */
    @Override
    public CalendarValue adjustTimezone(int tz) {
        return (DateTimeValue)toDateTime().adjustTimezone(tz).convertPrimitive(getItemType(), false);
    }

    /**
    * Get a component of the value. Returns null if the timezone component is
    * requested and is not present.
    */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
        case Component.YEAR:
            int value = year > 0 ? year : year-1;

            return new IntegerValue(value);
            case Component.MONTH:

                return new IntegerValue(month);
            case Component.DAY:

                return new IntegerValue(day);
            case Component.TIMEZONE:
            if (hasTimezone()) {
                return DayTimeDurationValue.fromMilliseconds(60000L * getTimezoneInMinutes());
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for date: " + component);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
