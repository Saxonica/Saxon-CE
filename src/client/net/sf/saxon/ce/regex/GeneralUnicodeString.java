package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.value.*;

/**
 * A Unicode string which, in general, may contain non-BMP characters (that is, codepoints
 * outside the range 0-65535)
 */

public final class GeneralUnicodeString implements UnicodeString {

    private int[] chars;
    private int start;
    private int end;

    public GeneralUnicodeString(CharSequence in) {
        chars = StringValue.expand(in);
        start = 0;
        end = chars.length;
    }

    private GeneralUnicodeString(int[] chars, int start, int end) {
        this.chars = chars;
        this.start = start;
        this.end = end;
    }

    public static boolean containsSurrogatePairs(CharSequence value) {
        for (int i = 0; i < value.length(); i++) {
            int c = (int) value.charAt(i);
            if (c >= 55296 && c < 56319) {
                return true;
            }
        }
        return false;
    }

    /**
     * Make a UnicodeString for a given CharSequence
     * @param in the input CharSequence
     * @return a UnicodeString using an appropriate implementation class
     */

    public static UnicodeString makeUnicodeString(CharSequence in) {
        if (containsSurrogatePairs(in)) {
            return new GeneralUnicodeString(in);
        } else {
            return new BMPString(in);
        }
    }

    public UnicodeString substring(int beginIndex, int endIndex) {
        if (endIndex > chars.length) {
            throw new IndexOutOfBoundsException("endIndex=" + endIndex
                                                + "; sequence size=" + chars.length);
        }
        if (beginIndex < 0 || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException("beginIndex=" + beginIndex
                                                + "; endIndex=" + endIndex);
        }
        return new GeneralUnicodeString(chars, start + beginIndex, start + endIndex);
    }

    public int charAt(int pos) {
        return chars[start + pos];
    }

    public int indexOf(int search, int pos) {
        for (int i=pos; i<length(); i++) {
            if (chars[start+i] == search) {
                return i;
            }
        }
        return -1;
    }

    public int length() {
        return end - start;
    }

    public boolean isEnd(int pos) {
        return (pos >= (end - start));
    }

    public String toString() {
        int[] c = chars;
        if (start != 0) {
            c = new int[end - start];
            System.arraycopy(chars, start, c, 0, end - start);
        }
        return StringValue.contract(c, end - start).toString();
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
