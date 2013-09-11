package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.List;


/**
 * Glue class to interface the Jakarta regex engine to Saxon
 */

public class ARegularExpression implements RegularExpression {

    UnicodeString rawPattern;
    String rawFlags;
    REProgram regex;

    public static ARegularExpression make(String pattern) {
        try {
            return new ARegularExpression(pattern, "", "XP20", null);
        } catch (XPathException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ARegularExpression(CharSequence pattern, String flags, String hostLanguage, List<String> warnings) throws XPathException {
        rawFlags = flags;
        REFlags reFlags;
        try {
            reFlags = new REFlags(flags, hostLanguage);
        } catch (RESyntaxException err) {
            throw new XPathException(err.getMessage(),  "FORX0001");
        }
        try {
            rawPattern = GeneralUnicodeString.makeUnicodeString(pattern);
            RECompiler comp2 = new RECompiler();
            comp2.setFlags(reFlags);
            regex = comp2.compile(rawPattern);
            if (warnings != null) {
                for (String s : comp2.getWarnings()) {
                    warnings.add(s);
                }
            }
        } catch (RESyntaxException err) {
            throw new XPathException(err.getMessage(), "FORX0002");
        }
    }

    /**
     * Determine whether the regular expression matches a given string in its entirety
     *
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */
    public boolean matches(CharSequence input) {
        if (input.length() == 0) {
            return regex.isNullable();
        }
        REMatcher matcher = new REMatcher(regex);
        return matcher.anchoredMatch(GeneralUnicodeString.makeUnicodeString(input));
    }

    /**
     * Determine whether the regular expression contains a match of a given string
     *
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */
    public boolean containsMatch(CharSequence input) {
        REMatcher matcher = new REMatcher(regex);
        return matcher.match(GeneralUnicodeString.makeUnicodeString(input), 0);
    }

    /**
     * Use this regular expression to tokenize an input string.
     *
     * @param input the string to be tokenized
     * @return a SequenceIterator containing the resulting tokens, as objects of type StringValue
     */
    public SequenceIterator tokenize(CharSequence input) {
        return new ATokenIterator(GeneralUnicodeString.makeUnicodeString(input), new REMatcher(regex));
    }

    /**
     * Use this regular expression to analyze an input string, in support of the XSLT
     * analyze-string instruction. The resulting RegexIterator provides both the matching and
     * non-matching substrings, and allows them to be distinguished. It also provides access
     * to matched subgroups.
     *
     * @param input the character string to be analyzed using the regular expression
     * @return an iterator over matched and unmatched substrings
     */
    public RegexIterator analyze(CharSequence input) {
        return new ARegexIterator(GeneralUnicodeString.makeUnicodeString(input), rawPattern, new REMatcher(regex));
    }

    /**
     * Replace all substrings of a supplied input string that match the regular expression
     * with a replacement string.
     *
     * @param input       the input string on which replacements are to be performed
     * @param replacement the replacement string in the format of the XPath replace() function
     * @return the result of performing the replacement
     * @throws XPathException if the replacement string is invalid
     */
    public CharSequence replace(CharSequence input, CharSequence replacement) throws XPathException {
        REMatcher matcher = new REMatcher(regex);
        if (matcher.match("")) {
            throw new XPathException("The regular expression must not be one that matches a zero-length string", "FORX0003");
        }
        UnicodeString in = GeneralUnicodeString.makeUnicodeString(input);
        UnicodeString rep = GeneralUnicodeString.makeUnicodeString(replacement);
        try {
            return matcher.subst(in, rep);
        } catch (RESyntaxException err) {
            throw new XPathException(err.getMessage(), "FORX0004");
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
