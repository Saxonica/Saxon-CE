package client.net.sf.saxon.ce.expr.number;

import client.net.sf.saxon.ce.lib.Numberer;
import client.net.sf.saxon.ce.tree.util.UTF16CharacterSet;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * Class AbstractNumberer is a base implementation of Numberer that provides language-independent
 * default numbering
 * This supports the xsl:number element.
 * Methods and data are declared as protected, and static is avoided, to allow easy subclassing.
 *
 * @author Michael H. Kay
 */

public abstract class AbstractNumberer implements Numberer {

    private String country;

    public static final int UPPER_CASE = 0;
    public static final int LOWER_CASE = 1;
    public static final int TITLE_CASE = 2;


    /**
     * Set the country used by this numberer (currenly used only for names of timezones)
     */

    public void setCountry(String country) {
        this.country = country;
    }


    /**
     * Get the country used by this numberer.
     *
     * @return the country used by this numberer, or null if no country has been set
     */

    public String getCountry() {
        return country;
    }

    /**
     * Format a number into a string. This method is provided for backwards compatibility. It merely
     * calls the other format method after constructing a RegularGroupFormatter. The method is final;
     * localization subclasses should implement the method
     * {@link #format(long, String, NumericGroupFormatter, String, String)} rather than this method.
     *
     * @param number         The number to be formatted
     * @param picture        The format token. This is a single component of the format attribute
     *                       of xsl:number, e.g. "1", "01", "i", or "a"
     * @param groupSize      number of digits per group (0 implies no grouping)
     * @param groupSeparator string to appear between groups of digits
     * @param letterValue    The letter-value specified to xsl:number: "alphabetic" or
     *                       "traditional". Can also be an empty string or null.
     * @param ordinal        The value of the ordinal attribute specified to xsl:number
     *                       The value "yes" indicates that ordinal numbers should be used; "" or null indicates
     *                       that cardinal numbers
     * @return the formatted number. Note that no errors are reported; if the request
     *         is invalid, the number is formatted as if the string() function were used.
     */

    public final String format(long number,
                         String picture,
                         int groupSize,
                         String groupSeparator,
                         String letterValue,
                         String ordinal) {

        return format(number, picture, new RegularGroupFormatter(groupSize, groupSeparator), letterValue, ordinal);
    }

    /**
     * Format a number into a string
     *
     * @param number            The number to be formatted
     * @param picture           The format token. This is a single component of the format attribute
     *                          of xsl:number, e.g. "1", "01", "i", or "a"
     * @param numGroupFormatter object contains separators to appear between groups of digits
     * @param letterValue       The letter-value specified to xsl:number: "alphabetic" or
     *                          "traditional". Can also be an empty string or null.
     * @param ordinal           The value of the ordinal attribute specified to xsl:number
     *                          The value "yes" indicates that ordinal numbers should be used; "" or null indicates
     *                          that cardinal numbers
     * @return the formatted number. Note that no errors are reported; if the request
     *         is invalid, the number is formatted as if the string() function were used.
     */
    public String format(long number,
                         String picture,
                         NumericGroupFormatter numGroupFormatter,
                         String letterValue,
                         String ordinal) {


        if (number < 0 || picture == null || picture.length() == 0) {
            return "" + number;
        }

        int pictureLength = StringValue.getStringLength(picture);
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.TINY);
        int formchar = picture.charAt(0);
        if (UTF16CharacterSet.isHighSurrogate(formchar)) {
            formchar = UTF16CharacterSet.combinePair((char) formchar, picture.charAt(1));
        }

        switch (formchar) {

            case '0':
            case '1':
                sb.append(toRadical(number, westernDigits, pictureLength, numGroupFormatter));
                if (ordinal != null && ordinal.length() > 0) {
                    sb.append(ordinalSuffix(ordinal, number));
                }
                break;

            case 'A':
                if (number == 0) {
                    return "0";
                }
                return toAlphaSequence(number, latinUpper);

            case 'a':
                if (number == 0) {
                    return "0";
                }
                return toAlphaSequence(number, latinLower);

            case 'w':
            case 'W':
                int wordCase;
                if (picture.equals("W")) {
                    wordCase = UPPER_CASE;
                } else if (picture.equals("w")) {
                    wordCase = LOWER_CASE;
                } else {
                    // includes cases like "ww" or "Wz". The action here is conformant, but it's not clear what's best
                    wordCase = TITLE_CASE;
                }
                if (ordinal != null && ordinal.length() > 0) {
                    return toOrdinalWords(ordinal, number, wordCase);
                } else {
                    return toWords(number, wordCase);
                }

            case 'i':
                if (letterValue == null || letterValue.length() == 0 ||
                        letterValue.equals("traditional")) {
                    return toRoman(number);
                }
                break;

            case 'I':
                if (letterValue == null || letterValue.length() == 0 ||
                        letterValue.equals("traditional")) {
                    return toRoman(number).toUpperCase();
                }
                break;

            case '\u2460':
                // circled digits
                if (number == 0 || number > 20) {
                    return "" + number;
                }
                return "" + (char) (0x2460 + number - 1);

            case '\u2474':
                // parenthesized digits
                if (number == 0 || number > 20) {
                    return "" + number;
                }
                return "" + (char) (0x2474 + number - 1);

            case '\u2488':
                // digit full stop
                if (number == 0 || number > 20) {
                    return "" + number;
                }
                return "" + (char) (0x2488 + number - 1);

            case '\u0391':
                return toAlphaSequence(number, greekUpper);

            case '\u03b1':
                return toAlphaSequence(number, greekLower);


            default:

                int digitValue = Alphanumeric.getDigitValue(formchar);
                if (digitValue >= 0) {

                    int zero = formchar - digitValue;
                    int[] digits = new int[10];
                    for (int z = 0; z <= 9; z++) {
                        digits[z] = zero + z;
                    }

                    return toRadical(number, digits, pictureLength, numGroupFormatter);

                } else {
                    if (number == 0) {
                        return "0";
                    }
                    // fallback to western numbering
                    return toRadical(number, westernDigits, pictureLength, numGroupFormatter);

                }
        }

        return sb.toString();
    }


    /**
     * Construct the ordinal suffix for a number, for example "st", "nd", "rd". The default
     * (language-neutral) implementation returns a zero-length string
     *
     * @param ordinalParam the value of the ordinal attribute (used in non-English
     *                     language implementations)
     * @param number       the number being formatted
     * @return the ordinal suffix to be appended to the formatted number
     */

    protected String ordinalSuffix(String ordinalParam, long number) {
        return "";
    }

    protected static final int[] westernDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    protected static final String latinUpper =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    protected static final String latinLower =
            "abcdefghijklmnopqrstuvwxyz";

    protected static final String greekUpper =
            "\u0391\u0392\u0393\u0394\u0395\u0396\u0397\u0398\u0399\u039a" +
                    "\u039b\u039c\u039d\u039e\u039f\u03a0\u03a1\u03a2\u03a3\u03a4" +
                    "\u03a5\u03a6\u03a7\u03a8\u03a9";

    protected static final String greekLower =
            "\u03b1\u03b2\u03b3\u03b4\u03b5\u03b6\u03b7\u03b8\u03b9\u03ba" +
                    "\u03bb\u03bc\u03bd\u03be\u03bf\u03c0\u03c1\u03c2\u03c3\u03c4" +
                    "\u03c5\u03c6\u03c7\u03c8\u03c9";

    /**
     * Format the number as an alphabetic label using the alphabet consisting
     * of consecutive Unicode characters from min to max
     *
     * @param number the number to be formatted
     * @param min    the start of the Unicode codepoint range
     * @param max    the end of the Unicode codepoint range
     * @return the formatted number
     */

    protected String toAlpha(long number, int min, int max) {
        if (number <= 0) {
            return "" + number;
        }
        int range = max - min + 1;
        char last = (char) (((number - 1) % range) + min);
        if (number > range) {
            return toAlpha((number - 1) / range, min, max) + last;
        } else {
            return "" + last;
        }
    }

    /**
     * Convert the number into an alphabetic label using a given alphabet.
     * For example, if the alphabet is "xyz" the sequence is x, y, z, xx, xy, xz, ....
     *
     * @param number   the number to be formatted
     * @param alphabet a string containing the characters to be used, for example "abc...xyz"
     * @return the formatted number
     */

    protected String toAlphaSequence(long number, String alphabet) {
        if (number <= 0) {
            return "" + number;
        }
        int range = alphabet.length();
        char last = alphabet.charAt((int) ((number - 1) % range));
        if (number > range) {
            return toAlphaSequence((number - 1) / range, alphabet) + last;
        } else {
            return "" + last;
        }
    }

    /**
     * Convert the number into a decimal or other representation using the given set of
     * digits.
     * For example, if the digits are "01" the sequence is 1, 10, 11, 100, 101, 110, 111, ...
     * More commonly, the digits will be "0123456789", giving the usual decimal numbering.
     *
     * @param number            the number to be formatted
     * @param digits            the codepoints to be used for the digits
     * @param pictureLength     the length of the picture that is significant: for example "3" if the
     *                          picture is "001"
     * @param numGroupFormatter an object that encapsulates the rules for inserting grouping separators
     * @return the formatted number
     */

    private String toRadical(long number, int[] digits, int pictureLength,
                             NumericGroupFormatter numGroupFormatter) {

        FastStringBuffer temp = new FastStringBuffer(FastStringBuffer.TINY);
        int base = digits.length;
        FastStringBuffer s = new FastStringBuffer(FastStringBuffer.TINY);
        long n = number;
        int count = 0;
        while (n > 0) {
            int digit = digits[(int) (n % base)];
            s.prependWideChar(digit);
            count++;
            n = n / base;
        }

        for (int i = 0; i < (pictureLength - count); i++) {
            temp.appendWideChar(digits[0]);
        }
        temp.append(s);

        if (numGroupFormatter == null) {
            return temp.toString();
        }

        return numGroupFormatter.format(temp);
    }

    /**
     * Generate a Roman numeral (in lower case)
     *
     * @param n the number to be formatted
     * @return the Roman numeral representation of the number in lower case
     */

    public static String toRoman(long n) {
        if (n <= 0 || n > 9999) {
            return "" + n;
        }
        return romanThousands[(int) n / 1000] +
                romanHundreds[((int) n / 100) % 10] +
                romanTens[((int) n / 10) % 10] +
                romanUnits[(int) n % 10];
    }

    // Roman numbers beyond 4000 use overlining and other conventions which we won't
    // attempt to reproduce. We'll go high enough to handle present-day Gregorian years.

    private static String[] romanThousands =
            {"", "m", "mm", "mmm", "mmmm", "mmmmm", "mmmmmm", "mmmmmmm", "mmmmmmmm", "mmmmmmmmm"};
    private static String[] romanHundreds =
            {"", "c", "cc", "ccc", "cd", "d", "dc", "dcc", "dccc", "cm"};
    private static String[] romanTens =
            {"", "x", "xx", "xxx", "xl", "l", "lx", "lxx", "lxxx", "xc"};
    private static String[] romanUnits =
            {"", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix"};


    /**
     * Show the number as words in title case. (We choose title case because
     * the result can then be converted algorithmically to lower case or upper case).
     *
     * @param number the number to be formatted
     * @return the number formatted as English words
     */

    public abstract String toWords(long number);

    /**
     * Format a number as English words with specified case options
     *
     * @param number   the number to be formatted
     * @param wordCase the required case for example {@link #UPPER_CASE},
     *                 {@link #LOWER_CASE}, {@link #TITLE_CASE}
     * @return the formatted number
     */

    public String toWords(long number, int wordCase) {
        String s;
        if (number == 0) {
            s = "Zero";
        } else {
            s = toWords(number);
        }
        if (wordCase == UPPER_CASE) {
            return s.toUpperCase();
        } else if (wordCase == LOWER_CASE) {
            return s.toLowerCase();
        } else {
            return s;
        }
    }

    /**
     * Show an ordinal number as English words in a requested case (for example, Twentyfirst)
     *
     * @param ordinalParam the value of the "ordinal" attribute as supplied by the user
     * @param number       the number to be formatted
     * @param wordCase     the required case for example {@link #UPPER_CASE},
     *                     {@link #LOWER_CASE}, {@link #TITLE_CASE}
     * @return the formatted number
     */

    public abstract String toOrdinalWords(String ordinalParam, long number, int wordCase);

    /**
     * Get a month name or abbreviation
     *
     * @param month    The month number (1=January, 12=December)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public abstract String monthName(int month, int minWidth, int maxWidth);

    /**
     * Get a day name or abbreviation
     *
     * @param day      The day of the week (1=Monday, 7=Sunday)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public abstract String dayName(int day, int minWidth, int maxWidth);

    /**
     * Get an am/pm indicator. Default implementation works for English, on the basis that some
     * other languages might like to copy this (most non-English countries don't actually use the
     * 12-hour clock, so it's irrelevant).
     *
     * @param minutes  the minutes within the day
     * @param minWidth minimum width of output
     * @param maxWidth maximum width of output
     * @return the AM or PM indicator
     */

    public String halfDayName(int minutes, int minWidth, int maxWidth) {
        String s;
        if (minutes == 0 && maxWidth >= 8) {
            s = "Midnight";
        } else if (minutes < 12 * 60) {
            switch (maxWidth) {
                case 1:
                    s = "A";
                    break;
                case 2:
                case 3:
                    s = "Am";
                    break;
                default:
                    s = "A.M.";
            }
        } else if (minutes == 12 * 60 && maxWidth >= 8) {
            s = "Noon";
        } else {
            switch (maxWidth) {
                case 1:
                    s = "P";
                    break;
                case 2:
                case 3:
                    s = "Pm";
                    break;
                default:
                    s = "P.M.";
            }
        }
        return s;
    }

    /**
     * Get an ordinal suffix for a particular component of a date/time.
     *
     *
     * @param component the component specifier from a format-dateTime picture, for
     *                  example "M" for the month or "D" for the day.
     * @return a string that is acceptable in the ordinal attribute of xsl:number
     *         to achieve the required ordinal representation. For example, "-e" for the day component
     *         in German, to have the day represented as "dritte August".
     */

    public String getOrdinalSuffixForDateTime(char component) {
        return "yes";
    }

    /**
     * Get the name for an era (e.g. "BC" or "AD")
     *
     * @param year the proleptic gregorian year, using "0" for the year before 1AD
     */

    public String getEraName(int year) {
        return (year > 0 ? "AD" : "BC");
    }

    /**
     * Get the name of a calendar
     *
     * @param code The code representing the calendar as in the XSLT 2.0 spec, e.g. AD for the Gregorian calendar
     */

    public String getCalendarName(String code) {
        if (code.equals("AD")) {
            return "Gregorian";
        } else {
            return code;
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
