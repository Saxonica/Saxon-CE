package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.functions.Component;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.StringTokenizer;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
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
                         int hours, int minutes, long seconds, int microseconds)
    throws IllegalArgumentException {
        this(positive, years, months, days, hours, minutes, seconds, microseconds, BuiltInAtomicType.DURATION);
    }

    /**
     * Constructor for xs:duration taking the components of the duration, plus a user-specified
     * type which must be a subtype of xs:duration. There is no requirement
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
     * @param seconds      the number of seconds (long to allow copying)
     * @param microseconds the number of microseconds
     * @param type         the user-defined subtype of xs:duration. Note that this constructor cannot
     *                     be used to create an instance of xs:dayTimeDuration or xs:yearMonthDuration.
     * @throws IllegalArgumentException if the size of the duration exceeds implementation-defined
     * limits: specifically, if the total number of months exceeds 2^31, or if the total number
     * of seconds exceeds 2^63.
     */

    public DurationValue(boolean positive, int years, int months, int days,
                         int hours, int minutes, long seconds, int microseconds, BuiltInAtomicType type) {
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
        typeLabel = type;
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
        return makeDuration(s, true, true);
    }

    protected static ConversionResult makeDuration(CharSequence s, boolean allowYM, boolean allowDT) {
        int years = 0, months = 0, days = 0, hours = 0, minutes = 0, seconds = 0, microseconds = 0;
        boolean negative = false;
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-+.PYMDTHS", true);
        int components = 0;
        if (!tok.hasMoreElements()) {
            return badDuration("empty string", s);
        }
        String part = (String)tok.nextElement();
        if ("+".equals(part)) {
            return badDuration("+ sign not allowed in a duration", s);
        } else if ("-".equals(part)) {
            negative = true;
            part = (String)tok.nextElement();
        }
        if (!"P".equals(part)) {
            return badDuration("missing 'P'", s);
        }
        int state = 0;
        while (tok.hasMoreElements()) {
            part = (String)tok.nextElement();
            if ("T".equals(part)) {
                state = 4;
                if (!tok.hasMoreElements()) {
                    return badDuration("T must be followed by time components", s);
                }
                part = (String)tok.nextElement();
            }
            int value = simpleInteger(part);
            if (value < 0) {
                if (part.length() > 8) {
                    return badDuration("component invalid or too large", s);
                } else {
                    return badDuration("non-numeric component", s);
                }
            }
            if (!tok.hasMoreElements()) {
                return badDuration("missing unit letter at end", s);
            }
            char delim = ((String)tok.nextElement()).charAt(0);
            switch (delim) {
            case'Y':
                if (state > 0) {
                    return badDuration("Y is out of sequence", s);
                }
                if (!allowYM) {
                    return badDuration("Year component is not allowed in dayTimeDuration", s);
                }
                years = value;
                state = 1;
                components++;
                break;
            case'M':
                if (state == 4 || state == 5) {
                    if (!allowDT) {
                        return badDuration("Minute component is not allowed in yearMonthDuration", s);
                    }
                    minutes = value;
                    state = 6;
                    components++;
                    break;
                } else if (state == 0 || state == 1) {
                    if (!allowYM) {
                        return badDuration("Month component is not allowed in dayTimeDuration", s);
                    }
                    months = value;
                    state = 2;
                    components++;
                    break;
                } else {
                    return badDuration("M is out of sequence", s);
                }
            case'D':
                if (state > 2) {
                    return badDuration("D is out of sequence", s);
                }
                if (!allowDT) {
                    return badDuration("Day component is not allowed in yearMonthDuration", s);
                }
                days = value;
                state = 3;
                components++;
                break;
            case'H':
                if (state != 4) {
                    return badDuration("H is out of sequence", s);
                }
                if (!allowDT) {
                    return badDuration("Hour component is not allowed in yearMonthDuration", s);
                }
                hours = value;
                state = 5;
                components++;
                break;
            case'.':
                if (state < 4 || state > 6) {
                    return badDuration("misplaced decimal point", s);
                }
                seconds = value;
                state = 7;
                break;
            case'S':
                if (state < 4 || state > 7) {
                    return badDuration("S is out of sequence", s);
                }
                if (!allowDT) {
                    return badDuration("Seconds component is not allowed in yearMonthDuration", s);
                }
                if (state == 7) {
                    while (part.length() < 6) {
                        part += "0";
                    }
                    if (part.length() > 6) {
                        part = part.substring(0, 6);
                    }
                    value = simpleInteger(part);
                    if (value < 0) {
                        return badDuration("non-numeric fractional seconds", s);
                    }
                    microseconds = value;
                } else {
                    seconds = value;
                }
                state = 8;
                components++;
                break;
            default:
                return badDuration("misplaced " + delim, s);
            }
        }

        if (components == 0) {
            return badDuration("Duration specifies no components", s);
        }

        try {
            return new DurationValue(
                    !negative, years, months, days, hours, minutes, seconds, microseconds, BuiltInAtomicType.DURATION);
        } catch (IllegalArgumentException err) {
            // catch values that exceed limits
            return new ValidationFailure(err.getMessage());
        }
    }


    protected static ValidationFailure badDuration(String msg, CharSequence s) {
        ValidationFailure err = new ValidationFailure("Invalid duration value '" + s + "' (" + msg + ')');
        err.setErrorCode("FORG0001");
        return err;
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

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.DURATION;
    }

    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @param validate     if set to false, the caller asserts that the value is known to be valid
     * @return an AtomicValue, a value of the required type; or a {@link ValidationFailure} if
     *         the value cannot be converted.
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC || requiredType == BuiltInAtomicType.DURATION) {
            return this;
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValueCS());
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValueCS());
        } else if (requiredType == BuiltInAtomicType.YEAR_MONTH_DURATION) {
            return YearMonthDurationValue.fromMonths(months * (negative ? -1 : +1));
        } else if (requiredType == BuiltInAtomicType.DAY_TIME_DURATION) {
            return new DayTimeDurationValue((negative ? -1 : +1), 0, 0, 0, seconds, microseconds);
        } else {
            ValidationFailure err = new ValidationFailure("Cannot convert duration to " +
                    requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Normalize the duration, so that months<12, hours<24, minutes<60, seconds<60.
     * Since durations are now always normalized, this method has become a no-op, but is retained
     * for backwards compatibility
     * @deprecated since 9.0 - the method does nothing
     *
     * @return the duration unchanged
     */

    public DurationValue normalizeDuration() {
        return this;
    }

    /**
     * Return the signum of the value
     *
     * @return -1 if the duration is negative, zero if it is zero-length, +1 if it is positive
     */

    public int signum() {
        if (negative) {
            return -1;
        }
        if (months == 0 && seconds == 0L && microseconds == 0) {
            return 0;
        }
        return +1;
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
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. This method is refined for AtomicValues
     * so that it never throws an Exception.
     */

//    public String getStringValue() {
//        return getStringValueCS().toString();
//    }

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
     * Convert to Java object (for passing to external functions)
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target.isAssignableFrom(DurationValue.class)) {
//            return this;
//        } else if (target == Object.class) {
//            return getStringValue();
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                XPathException err = new XPathException("Conversion of xs:duration to " + target.getName() +
//                        " is not supported");
//                err.setXPathContext(context);
//                err.setErrorCode(SaxonErrorCode.SXJE0003);
//            }
//            return o;
//        }
//    }

    /**
     * Get a component of the normalized value
     */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
        case Component.YEAR:
            return IntegerValue.makeIntegerValue((negative ? -getYears() : getYears()));
        case Component.MONTH:
            return IntegerValue.makeIntegerValue((negative ? -getMonths() : getMonths()));
        case Component.DAY:
            return IntegerValue.makeIntegerValue((negative ? -getDays() : getDays()));
        case Component.HOURS:
            return IntegerValue.makeIntegerValue((negative ? -getHours() : getHours()));
        case Component.MINUTES:
            return IntegerValue.makeIntegerValue((negative ? -getMinutes() : getMinutes()));
        case Component.SECONDS:
            FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.TINY);
            String ms = ("000000" + microseconds);
            ms = ms.substring(ms.length() - 6);
            sb.append((negative ? "-" : "") + getSeconds() + '.' + ms);
            return (AtomicValue)DecimalValue.makeDecimalValue(sb);
        case Component.WHOLE_SECONDS:
            return new IntegerValue(new BigDecimal(negative ? -seconds : seconds));
        case Component.MICROSECONDS:
            return IntegerValue.makeIntegerValue((negative ? -microseconds : microseconds));
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
     * @param collator collation used for comparing string values
     * @param context the XPath dynamic evaluation context, used in cases where the comparison is context
*                sensitive @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
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
     */

    public DurationValue add(DurationValue other) throws XPathException {
        XPathException err = new XPathException("Only subtypes of xs:duration can be added");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
     * Subtract two durations
     *
     * @param other the duration to be subtracted from this one
     * @return the difference of the two durations
     */

    public DurationValue subtract(DurationValue other) throws XPathException {
        XPathException err = new XPathException("Only subtypes of xs:duration can be subtracted");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     *
     * @return the original duration with its sign reversed, retaining its type
     */

    public DurationValue negate() {
        return new DurationValue(negative, 0, months, 0, 0, 0, seconds, microseconds, typeLabel);
    }

    /**
     * Multiply a duration by a number
     *
     * @param factor the number to multiply by
     * @return the result of the multiplication
     */

    public DurationValue multiply(double factor) throws XPathException {
        XPathException err = new XPathException("Only subtypes of xs:duration can be multiplied by a number");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
     * Divide a duration by a another duration
     *
     * @param other the duration to divide by
     * @return the result of the division
     */

    public DecimalValue divide(DurationValue other) throws XPathException {
        XPathException err = new XPathException("Only subtypes of xs:duration can be divided by another duration");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

     /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * This implementation handles the ordering rules for durations in XML Schema.
     * It is overridden for the two subtypes DayTimeDuration and YearMonthDuration.
     *
     * @return a suitable Comparable
     */

     private Comparable getSchemaComparable() {
        return getSchemaComparable(this);
    }

    /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * This implementation handles the ordering rules for durations in XML Schema.
     *
     * @param value the duration for which a comparison key is required
     * @return a suitable Comparable
     */

    public static Comparable getSchemaComparable(DurationValue value) {
        int m = value.months;
        double s = value.seconds + ((double)value.microseconds / 1000000);
        if (value.negative) {
            s = -s;
            m = -m;
        }
        return new DurationComparable(m, s);
    }

    /**
     * DurationValueOrderingKey is a Comparable value that acts as a surrogate for a Duration,
     * having ordering rules that implement the XML Schema specification.
     */

    private static class DurationComparable implements Comparable {

        private int months;
        private double seconds;

        public DurationComparable(int m, double s) {
            months = m;
            seconds = s;
        }

        /**
         * Compare two durations according to the XML Schema rules.
         *
         * @param o the other duration
         * @return -1 if this duration is smaller; 0 if they are equal; +1 if this duration is greater;
         *         {@link AtomicValue#INDETERMINATE_ORDERING} if there is no defined order
         */

        public int compareTo(Object o) {
            DurationComparable other;
            if (o instanceof DurationComparable) {
                other = (DurationComparable)o;
            } else if (o instanceof YearMonthDurationValue) {
                other = (DurationComparable)getSchemaComparable((YearMonthDurationValue)o);
            } else if (o instanceof DayTimeDurationValue) {
                other = (DurationComparable)getSchemaComparable((DayTimeDurationValue)o);
            } else {
                return INDETERMINATE_ORDERING;
            }
            if (months == other.months) {
                return Double.compare(seconds, other.seconds);
            } else if (seconds == other.seconds) {
                return (months == other.months ? 0 : (months < other.months ? -1 : +1));
            } else {
                double oneDay = 24e0 * 60e0 * 60e0;
                double min0 = monthsToDaysMinimum(months) * oneDay + seconds;
                double max0 = monthsToDaysMaximum(months) * oneDay + seconds;
                double min1 = monthsToDaysMinimum(other.months) * oneDay + other.seconds;
                double max1 = monthsToDaysMaximum(other.months) * oneDay + other.seconds;
                if (max0 < min1) {
                    return -1;
                } else if (min0 > max1) {
                    return +1;
                } else {
                    return INDETERMINATE_ORDERING;
                }
            }
        }

        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }

        public int hashCode() {
            return months ^ (int)seconds;
        }

        private int monthsToDaysMinimum(int months) {
            if (months < 0) {
                return -monthsToDaysMaximum(-months);
            }
            if (months < 12) {
                int[] shortest = {0, 28, 59, 89, 120, 150, 181, 212, 242, 273, 303, 334};
                return shortest[months];
            } else {
                int years = months / 12;
                int remainingMonths = months % 12;
                // the -1 is to allow for the fact that we might miss a leap day if we time the start badly
                int yearDays = years * 365 + (years % 4) - (years % 100) + (years % 400) - 1;
                return yearDays + monthsToDaysMinimum(remainingMonths);
            }
        }

        private int monthsToDaysMaximum(int months) {
            if (months < 0) {
                return -monthsToDaysMinimum(-months);
            }
            if (months < 12) {
                int[] longest = {0, 31, 62, 92, 123, 153, 184, 215, 245, 276, 306, 337};
                return longest[months];
            } else {
                int years = months / 12;
                int remainingMonths = months % 12;
                // the +1 is to allow for the fact that we might miss a leap day if we time the start badly
                int yearDays = years * 365 + (years % 4) - (years % 100) + (years % 400) + 1;
                return yearDays + monthsToDaysMaximum(remainingMonths);
            }
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
