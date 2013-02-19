package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;


/**
 * This interface defines an iterator that supports the evaluation of xsl:analyze-string.
 * It returns all the matching and non-matching substrings in an input string, and
 * provides access to their captured groups
 */
public interface RegexIterator extends SequenceIterator {

    /**
     * Determine whether the current item in the sequence is a matching item or a non-matching item
     * @return true if the current item is a matching item
     */

    public boolean isMatching();

    /**
    * Get a substring that matches a parenthesised group within the regular expression
    * @param number    the number of the group to be obtained
    * @return the substring of the current item that matches the n'th parenthesized group
    * within the regular expression
    */

    /*@Nullable*/ public String getRegexGroup(int number);

    /**
     * Get a sequence containing all the regex captured groups relating to the current matching item
     * (except group 0, because we want to use indexing from 1).
     * This is used by the saxon:analyze-string() higher-order extension function.
     */

    public SequenceIterator getRegexGroupIterator();

    /**
     * Process a matching substring, performing specified actions at the start and end of each matching
     * group
     */

    public void processMatchingSubstring(XPathContext context, OnGroup action) throws XPathException;

    /**
     * Interface defining a call-back action for processing captured groups
     */

    public static interface OnGroup {

        /**
         * Method to be called when the start of a captured group is encountered
         * @param c the dynamic evaluation context
         * @param groupNumber the group number of the captured group
         */

        public void onGroupStart(XPathContext c, int groupNumber) throws XPathException;

       /**
         * Method to be called when the end of a captured group is encountered
         * @param c the dynamic evaluation context
         * @param groupNumber the group number of the captured group
         */

        public void onGroupEnd(XPathContext c, int groupNumber) throws XPathException;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.