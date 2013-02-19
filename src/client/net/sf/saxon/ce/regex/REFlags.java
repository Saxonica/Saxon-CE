package client.net.sf.saxon.ce.regex;

/**
 * Class representing a set of regular expression flags (some combination of i, m, s, x, q).
 * Also contains options affecting the regular expression dialect: whether or not XPath 2.0
 * and XPath 3.0 extensions to XSD regex syntax are accepted.
 */

public class REFlags {

    private boolean caseIndependent;
    private boolean multiLine;
    private boolean singleLine;
    private boolean allowWhitespace;
    private boolean literal;
    private boolean xpath20;
    private boolean xpath30;
    private boolean debug;

    /**
     * Create the regular expression flags
     * @param flags a string containing zero or more of 'i', 'x', 'm', 's'
     * @param language one of "XSD", "XP20", or "XP30" indicating the regular expression dialect
     */
    public REFlags(String flags, String language) {
        int semi = flags.indexOf(';');
        int endStd = (semi >= 0 ? semi : flags.length());
        for (int i=0; i<endStd; i++) {
            char c = flags.charAt(i);
            switch (c) {
                case 'i':
                    caseIndependent = true;
                    break;
                case 'm':
                    multiLine = true;
                    break;
                case 's':
                    singleLine = true;
                    break;
                case 'q':
                    literal = true;
                    break;
                case 'x':
                    allowWhitespace = true;
                    break;
                default:
                    throw new RESyntaxException("unrecognized flag '" + c + "'");
            }
        }
        for (int i=semi+1; i<flags.length(); i++) {
            char c = flags.charAt(i);
            switch (c) {
                case 'g':
                    debug = true;
                    break;
            }
        }
        if (language.equals("XSD")) {
            // no action
        } else if (language.equals("XP20")) {
            xpath20 = true;
            if (isLiteral()) {
                throw new RESyntaxException("'q' flag requires XPath 3.0 to be enabled");
            }
        } else if (language.equals("XP30")) {
            xpath20 = true;
            xpath30 = true;
        }
    }

    public boolean isCaseIndependent() {
        return caseIndependent;
    }

    public boolean isMultiLine() {
        return multiLine;
    }

    public boolean isSingleLine() {
        return singleLine;
    }

    public boolean isAllowWhitespace() {
        return allowWhitespace;
    }

    public boolean isLiteral() {
        return literal;
    }

    public boolean isAllowsXPath20Extensions() {
        return xpath20;
    }

    public boolean isAllowsXPath30Extensions() {
        return xpath30;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return debug;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
