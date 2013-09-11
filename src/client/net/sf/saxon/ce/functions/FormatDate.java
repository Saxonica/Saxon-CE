package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.number.Numberer_en;
import client.net.sf.saxon.ce.lib.Numberer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import java.math.BigDecimal;

/**
 * Implement the format-date(), format-time(), and format-dateTime() functions
 * in XSLT 2.0 and XQuery 1.1.
 */

public class FormatDate extends SystemFunction {


    public FormatDate() {
    }

    public FormatDate newInstance() {
        return new FormatDate();
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        int numArgs = argument.length;
        if (numArgs != 2 && numArgs != 5) {
            throw new XPathException("Function " + getDisplayName() +
                    " must have either two or five arguments",
                    getSourceLocator());
        }
        super.checkArguments(visitor);
    }

    /**
     * Evaluate in a general context
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        CalendarValue value = (CalendarValue) argument[0].evaluateItem(context);
        if (value == null) {
            return null;
        }
        String format = argument[1].evaluateItem(context).getStringValue();

        StringValue calendarVal = null;
        StringValue languageVal = null;
        if (argument.length > 2) {
            languageVal = (StringValue) argument[2].evaluateItem(context);
            calendarVal = (StringValue) argument[3].evaluateItem(context);
            // country argument is ignored
        }

        String language = (languageVal == null ? null : languageVal.getStringValue());
        CharSequence result = formatDate(value, format, language);
        if (calendarVal != null) {
            String cal = calendarVal.getStringValue();
            if (!cal.equals("AD") && !cal.equals("ISO")) {
                result = "[Calendar: AD]" + result.toString();
            }
        }
        return new StringValue(result);
    }

    /**
     * This method analyzes the formatting picture and delegates the work of formatting
     * individual parts of the date.
     *
     *
     *
     *
     * @param value    the value to be formatted
     * @param format   the supplied format picture
     * @param language the chosen language
     * @return the formatted date/time
     * @throws XPathException if a dynamic error occurs
     */

    public static CharSequence formatDate(CalendarValue value, String format, String language)
            throws XPathException {

        boolean languageDefaulted = (language == null);
        if (language == null) {
            language = "en";
        }

        Numberer numberer = new Numberer_en();
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        if (!"en".equals(language) && !languageDefaulted) {
            sb.append("[Language: en]");
        }

        int i = 0;
        while (true) {
            while (i < format.length() && format.charAt(i) != '[') {
                sb.append(format.charAt(i));
                if (format.charAt(i) == ']') {
                    i++;
                    if (i == format.length() || format.charAt(i) != ']') {
                        throw new XPathException("Closing ']' in date picture must be written as ']]'", "XTDE1340");
                    }
                }
                i++;
            }
            if (i == format.length()) {
                break;
            }
            // look for '[['
            i++;
            if (i < format.length() && format.charAt(i) == '[') {
                sb.append('[');
                i++;
            } else {
                int close = (i < format.length() ? format.indexOf("]", i) : -1);
                if (close == -1) {
                    throw new XPathException("Date format contains a '[' with no matching ']'", "XTDE1340");
                }
                String componentFormat = format.substring(i, close);
                sb.append(formatComponent(value, Whitespace.removeAllWhitespace(componentFormat), numberer));
                i = close + 1;
            }
        }
        return sb;
    }

    private static RegExp componentPattern =
            RegExp.compile("([YMDdWwFHhmsfZzPCE])\\s*(.*)");

    private static CharSequence formatComponent(CalendarValue value, CharSequence specifier,
                                                Numberer numberer)
            throws XPathException {
        DateTimeValue dtvalue = value.toDateTime();

        MatchResult matcher = componentPattern.exec(specifier.toString());
        if (matcher == null) {
            throw new XPathException("Unrecognized date/time component [" + specifier + ']', "XTDE1340");
        }
        char component = matcher.getGroup(1).charAt(0);
        String format = matcher.getGroup(2);
        if (format == null) {
            format = "";
        }
        boolean defaultFormat = false;
        if ("".equals(format) || format.startsWith(",")) {
            defaultFormat = true;
            String use;
            switch (component) {
                case 'F':
                    use = "Nn";
                    break;
                case 'P':
                    use = "n";
                    break;
                case 'C':
                case 'E':
                    use = "N";
                    break;
                case 'm':
                case 's':
                    use = "01";
                    break;
                default:
                    use = "1";
            }
            format = use + format;
        }

        if (value instanceof TimeValue && "YMDdWwFE".indexOf(component) >= 0) {
            throw new XPathException("In formatTime(): an xs:time value does not contain component " + component, "XTDE1350");
        } else if (value instanceof DateValue && "hmsfP".indexOf(component) >= 0) {
            throw new XPathException("In formatTime(): an xs:date value does not contain component " + component, "XTDE1350");
        }

        int componentValue;
        switch (component) {
            case 'Y':       // year
                componentValue = dtvalue.getYear();
                if (componentValue < 0) {
                    componentValue = 1 - componentValue;
                }
                break;
            case 'M':       // month
                componentValue = dtvalue.getMonth();
                break;
            case 'D':       // day in month
                componentValue = dtvalue.getDay();
                break;
            case 'd':       // day in year
                componentValue = DateValue.getDayWithinYear(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                break;
            case 'W':       // week of year
                componentValue = DateValue.getWeekNumber(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                break;
            case 'w':       // week in month
                componentValue = DateValue.getWeekNumberWithinMonth(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                break;
            case 'H':       // hour in day
                componentValue = dtvalue.getHour();
                break;
            case 'h':       // hour in half-day (12 hour clock)
                componentValue = dtvalue.getHour();
                if (componentValue > 12) {
                    componentValue = componentValue - 12;
                }
                if (componentValue == 0) {
                    componentValue = 12;
                }
                break;
            case 'm':       // minutes
                componentValue = dtvalue.getMinute();
                break;
            case 's':       // seconds
                componentValue = dtvalue.getSecond();
                break;
            case 'f':       // fractional seconds
                componentValue = dtvalue.getMicrosecond();
                break;
            case 'Z':       // timezone in +hh:mm format (ignore format=N)
            case 'z':
                FastStringBuffer sbz = new FastStringBuffer(8);
                if (component=='z') {
                    sbz.append("GMT");
                }
                dtvalue.appendTimezone(sbz);
                return sbz.toString();
            case 'F':       // day of week
                componentValue = DateValue.getDayOfWeek(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                break;
            case 'P':       // am/pm marker
                componentValue = dtvalue.getHour() * 60 + dtvalue.getMinute();
                break;
            case 'C':       // calendar
                return numberer.getCalendarName("AD");
            case 'E':       // era
                return numberer.getEraName(dtvalue.getYear());
            default:
                throw new XPathException("Unknown formatDate/time component specifier '" + format.charAt(0) + '\'', "XTDE1340");
        }
        return formatNumber(component, componentValue, format, defaultFormat, numberer);
    }

    private static RegExp formatPattern =
            RegExp.compile("([^,]*)(,.*)?");           // Note, the group numbers are different from above

    private static RegExp widthPattern =
            RegExp.compile(",(\\*|[0-9]+)(\\-(\\*|[0-9]+))?");

    private static RegExp alphanumericPattern =
            RegExp.compile("([A-Za-z0-9])*");

    private static RegExp digitsPattern =
            RegExp.compile("[0-9]+"); // was [0-9]* but this always returned a match - java: "\\p{Nd}*"

    private static CharSequence formatNumber(char component, int value,
                                             String format, boolean defaultFormat, Numberer numberer)
            throws XPathException {
        MatchResult matcher = formatPattern.exec(format);
        if (matcher == null) {
            throw new XPathException("Unrecognized format picture [" + component + format + ']', "XTDE1340");
        }
        String primary = matcher.getGroup(1);
        if (primary == null) {
            primary = "";
        }

        String modifier = null;
        if (primary.endsWith("t")) {
            primary = primary.substring(0, primary.length() - 1);
            modifier = "t";
        } else if (primary.endsWith("o")) {
            primary = primary.substring(0, primary.length() - 1);
            modifier = "o";
        }
        String letterValue = ("t".equals(modifier) ? "traditional" : null);
        String ordinal = ("o".equals(modifier) ? numberer.getOrdinalSuffixForDateTime(component) : null);
        String widths = matcher.getGroup(2);
        if (widths == null) {
            widths = "";
        }
        if (!alphanumericPattern.test(primary)) {
            throw new XPathException("In format picture at '" + primary +
                    "', primary format must be alphanumeric", "XTDE1340");
        }
        int min = 1;
        int max = Integer.MAX_VALUE;

        if (widths == null || "".equals(widths)) {
            if (digitsPattern.test(primary)) {
                int len = StringValue.getStringLength(primary);
                if (len > 1) {
                    // "A format token containing leading zeroes, such as 001, sets the minimum and maximum width..."
                    // We interpret this literally: a format token of "1" does not set a maximum, because it would
                    // cause the year 2006 to be formatted as "6".
                    min = len;
                    max = len;
                }
            }
        } else if (primary.equals("I") || primary.equals("i")) {
            // for roman numerals, ignore the width specifier
            min = 1;
            max = Integer.MAX_VALUE;
        } else {
            int[] range = getWidths(widths);
            min = range[0];
            max = range[1];
            if (defaultFormat) {
                // if format was defaulted, the explicit widths override the implicit format
                if (primary.endsWith("1") && min != primary.length()) {
                    FastStringBuffer sb = new FastStringBuffer(min + 1);
                    for (int i = 1; i < min; i++) {
                        sb.append('0');
                    }
                    sb.append('1');
                    primary = sb.toString();
                }
            }
        }

        if (component == 'P') {
            // A.M./P.M. can only be formatted as a name
            if (!("N".equals(primary) || "Nn".equals(primary))) {
                primary = "n";
            }
            if (max == Integer.MAX_VALUE) {
                // if no max specified, use 4. An explicit greater value allows use of "noon" and "midnight"
                max = 4;
            }
        } else if (component == 'f') {
            // value is supplied as integer number of microseconds
            String s;
            if (value == 0) {
                s = "0";
            } else {
                s = ((1000000 + value) + "").substring(1);
                if (s.length() > max) {
                    DecimalValue dec = new DecimalValue(new BigDecimal("0." + s));
                    dec = (DecimalValue) dec.roundHalfToEven(max);
                    s = dec.getStringValue();
                    if (s.length() > 2) {
                        // strip the ".0"
                        s = s.substring(2);
                    } else {
                        // fractional seconds value was 0
                        s = "";
                    }
                }
            }
            while (s.length() < min) {
                s = s + '0';
            }
            while (s.length() > min && s.charAt(s.length() - 1) == '0') {
                s = s.substring(0, s.length() - 1);
            }
            return s;
        }

        if ("N".equals(primary) || "n".equals(primary) || "Nn".equals(primary)) {
            String s = "";
            if (component == 'M') {
                s = numberer.monthName(value, min, max);
            } else if (component == 'F') {
                s = numberer.dayName(value, min, max);
            } else if (component == 'P') {
                s = numberer.halfDayName(value, min, max);
            } else {
                primary = "1";
            }
            if ("N".equals(primary)) {
                return s.toUpperCase();
            } else if ("n".equals(primary)) {
                return s.toLowerCase();
            } else {
                return s;
            }
        }

        String s = numberer.format(value, primary, null, letterValue, ordinal);
        int len = StringValue.getStringLength(s);
        while (len < min) {
            // assert: this can only happen as a result of width specifiers, in which case we're using ASCII digits
            s = ("00000000" + s).substring(s.length() + 8 - min);
            len = StringValue.getStringLength(s);
        }
        if (len > max) {
            // the year is the only field we allow to be truncated
            if (component == 'Y') {
                if (len == s.length()) {
                    // no wide characters
                    s = s.substring(s.length() - max);
                } else {
                    // assert: each character must be two bytes long
                    s = s.substring(s.length() - 2 * max);
                }

            }
        }
        return s;
    }

    private static int[] getWidths(String widths) throws XPathException {
        try {
            int min = -1;
            int max = -1;

            if (!"".equals(widths)) {
                MatchResult widthMatcher = widthPattern.exec(widths);
                if (widthMatcher != null) {
                    String smin = widthMatcher.getGroup(1);
                    if (smin == null || "".equals(smin) || "*".equals(smin)) {
                        min = 1;
                    } else {
                        min = Integer.parseInt(smin);
                    }
                    String smax = widthMatcher.getGroup(3);
                    if (smax == null || "".equals(smax) || "*".equals(smax)) {
                        max = Integer.MAX_VALUE;
                    } else {
                        max = Integer.parseInt(smax);
                    }
                } else {
                    throw new XPathException("Unrecognized width specifier " + Err.wrap(widths, Err.VALUE), "XTDE1340");
                }
            }

            if (min > max && max != -1) {
                throw new XPathException("Minimum width in date/time picture exceeds maximum width", "XTDE1340");
            }
            int[] result = new int[2];
            result[0] = min;
            result[1] = max;
            return result;
        } catch (NumberFormatException err) {
            throw new XPathException("Invalid integer used as width in date/time picture", "XTDE1340");
        }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
