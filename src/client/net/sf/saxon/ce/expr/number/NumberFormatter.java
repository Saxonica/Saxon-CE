package client.net.sf.saxon.ce.expr.number;

import client.net.sf.saxon.ce.lib.Numberer;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.UTF16CharacterSet;
import client.net.sf.saxon.ce.value.IntegerValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
  * Class NumberFormatter defines a method to format a ArrayList of integers as a character
  * string according to a supplied format specification.
  * @author Michael H. Kay
  */

public class NumberFormatter{

    private ArrayList formatTokens;
    private ArrayList punctuationTokens;
    private boolean startsWithPunctuation;

    /**
    * Tokenize the format pattern.
    * @param format the format specification. Contains one of the following values:<ul>
    * <li>"1": conventional decimal numbering</li>
    * <li>"a": sequence a, b, c, ... aa, ab, ac, ...</li>
    * <li>"A": sequence A, B, C, ... AA, AB, AC, ...</li>
    * <li>"i": sequence i, ii, iii, iv, v ...</li>
    * <li>"I": sequence I, II, III, IV, V, ...</li>
    * </ul>
    * This symbol may be preceded and followed by punctuation (any other characters) which is
    * copied to the output string.
    */

    public void prepare(String format) {

        // Tokenize the format string into alternating alphanumeric and non-alphanumeric tokens

        if (format.length()==0) {
            format="1";
        }

        formatTokens = new ArrayList(10);
        punctuationTokens = new ArrayList(10);

        int len = format.length();
        int i=0;
        int t;
        boolean first = true;
        startsWithPunctuation = true;

        while (i<len) {
            int c = format.charAt(i);
            t=i;
            if (UTF16CharacterSet.isHighSurrogate(c)) {
                c = UTF16CharacterSet.combinePair((char)c, format.charAt(++i));
            }
            while (isLetterOrDigit(c)) {
                i++;
                if (i==len) break;
                c = format.charAt(i);
                if (UTF16CharacterSet.isHighSurrogate(c)) {
                    c = UTF16CharacterSet.combinePair((char)c, format.charAt(++i));
                }
            }
            if (i>t) {
                String tok = format.substring(t, i);
                formatTokens.add(tok);
                if (first) {
                    punctuationTokens.add(".");
                    startsWithPunctuation = false;
                    first = false;
                }
            }
            if (i==len) break;
            t=i;
            c = format.charAt(i);
            if (UTF16CharacterSet.isHighSurrogate(c)) {
                c = UTF16CharacterSet.combinePair((char)c, format.charAt(++i));
            }
            while (!isLetterOrDigit(c)) {
                first = false;
                i++;
                if (i==len) break;
                c = format.charAt(i);
                if (UTF16CharacterSet.isHighSurrogate(c)) {
                    c = UTF16CharacterSet.combinePair((char)c, format.charAt(++i));
                }
            }
            if (i>t) {
                String sep = format.substring(t, i);
                punctuationTokens.add(sep);
            }
        }

        if (formatTokens.isEmpty()) {
            formatTokens.add("1");
            if (punctuationTokens.size() == 1) {
                punctuationTokens.add(punctuationTokens.get(0));
            }
        }

    }

    /**
     * Determine whether a (possibly non-BMP) character is a letter or digit.
     * @param c the codepoint of the character to be tested
     * @return true if this is a number or letter as defined in the XSLT rules for xsl:number pictures.
     */

    private static boolean isLetterOrDigit(int c) {
        if (c <= 0x7F) {
            // Fast path for ASCII characters
            return (c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5A) || (c >= 0x61 && c <= 0x7A);
        } else {
            return Alphanumeric.isAlphanumeric(c);
        }
    }

    /**
    * Format a list of numbers.
    * @param numbers the numbers to be formatted (a sequence of integer values; it may also contain
     * preformatted strings as part of the error recovery fallback)
    * @return the formatted output string.
    */

    public CharSequence format(List numbers, int groupSize, String groupSeparator,
                        String letterValue, String ordinal, Numberer numberer) {

        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.TINY);
        int num = 0;
        int tok = 0;
        // output first punctuation token
        if (startsWithPunctuation) {
            sb.append((String)punctuationTokens.get(tok));
        }
        // output the list of numbers
        while (num<numbers.size()) {
            if (num>0) {
                if (tok==0 && startsWithPunctuation) {
                    // The first punctuation token isn't a separator if it appears before the first
                    // formatting token. Such a punctuation token is used only once, at the start.
                    sb.append(".");
                } else {
                    sb.append((String)punctuationTokens.get(tok));
                }
            }
            Object o = numbers.get(num++);
            String s;
            if (o instanceof Long) {
                long nr = ((Long)o).longValue();
                RegularGroupFormatter rgf = new RegularGroupFormatter(groupSize, groupSeparator);
                s = numberer.format(nr, (String)formatTokens.get(tok), rgf, letterValue, ordinal);
            } else if (o instanceof BigDecimal) {
                s = new IntegerValue((BigDecimal)o).getStringValue();
            } else {
                s = o.toString();
            }
            sb.append(s);
            tok++;
            if (tok==formatTokens.size()) tok--;
        }
        // output the final punctuation token
        if (punctuationTokens.size()>formatTokens.size()) {
            sb.append((String)punctuationTokens.get(punctuationTokens.size()-1));
        }
        return sb.condense();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
