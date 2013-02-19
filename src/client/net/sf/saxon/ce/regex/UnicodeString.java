package client.net.sf.saxon.ce.regex;

/**
 * A character string, supporting characters outside the BMP.
 *
 * There are two implementations, one for BMP strings in which all characters are 16-bit values,
 * and one for more general strings in which all characters are 32-bit values.
 */

public interface UnicodeString
{
    /**
     * Get a substring of this string
     * @param beginIndex the index of the first character to be included (counting
     * codepoints, not 16-bit characters)
     * @param endIndex the index of the first character to be NOT included (counting
     * codepoints, not 16-bit characters)
     * @return a substring
     */
    UnicodeString substring(int beginIndex, int endIndex);

    /**
     * Get the first match for a given character
     * @param search the character to look for
     * @param start the first position to look
     * @return the position of the first occurrence of the sought character, or -1 if not found
     */

    int indexOf(int search, int start);

    /**
     * Get the character at a specified position
     * @param pos the index of the required character (counting
     * codepoints, not 16-bit characters)
     * @return a character (Unicode codepoint) at the specified position.
     */

    int charAt(int pos);

    /**
     * Get the length of the string, in Unicode codepoints
     * @return the number of codepoints in the string
     */

    int length();

    /**
     * Ask whether a given position is at (or beyond) the end of the string
     * @param pos the index of the required character (counting
     * codepoints, not 16-bit characters)
     * @return <tt>true</tt> iff if the specified index is after the end of the character stream
     */

    boolean isEnd(int pos);
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

