package client.net.sf.saxon.ce.tree.util;


/**
* This class defines properties of the UTF-8 character encoding
*/

public abstract class UTF8CharacterSet {

    /**
     * Private constructor to force the singular instance to be used
     */

    private UTF8CharacterSet() {}

    /**
    * Static method to generate the UTF-8 representation of a Unicode character
    * @param in the Unicode character, or the high half of a surrogate pair
    * @param in2 the low half of a surrogate pair (ignored unless the first argument is in the
    * range for a surrogate pair)
    * @param out an array of at least 4 bytes to hold the UTF-8 representation.
    * @return the number of bytes in the UTF-8 representation
    */

    public static int getUTF8Encoding(char in, char in2, byte[] out) {
        // See Tony Graham, "Unicode, a Primer", page 92
        int i = (int)in;
        if (i<=0x7f) {
            out[0] = (byte)i;
            return 1;
        } else if (i<=0x7ff) {
            out[0] = (byte)(0xc0 | ((in >> 6) & 0x1f));
            out[1] = (byte)(0x80 | (in & 0x3f));
            return 2;
        } else if (i>=0xd800 && i<=0xdbff) {
            // surrogate pair
            int j = (int)in2;
            if (!(j>=0xdc00 && j<=0xdfff)) {
                throw new IllegalArgumentException("Malformed Unicode Surrogate Pair (" + i + ',' + j + ')');
            }
            byte xxxxxx = (byte)(j & 0x3f);
            byte yyyyyy = (byte)(((i & 0x03) << 4) | ((j >> 6) & 0x0f));
            byte zzzz = (byte)((i >> 2) & 0x0f);
            byte uuuuu = (byte)(((i >> 6) & 0x0f) + 1);
            out[0] = (byte)(0xf0 | ((uuuuu >> 2) & 0x07));
            out[1] = (byte)(0x80 | ((uuuuu & 0x03) << 4) | zzzz);
            out[2] = (byte)(0x80 | yyyyyy);
            out[3] = (byte)(0x80 | xxxxxx);
            return 4;
        } else if (i>=0xdc00 && i<=0xdfff) {
            // second half of surrogate pair - ignore it
            return 0;
        } else {
            out[0] = (byte)(0xe0 | ((in >> 12) & 0x0f));
            out[1] = (byte)(0x80 | ((in >> 6) & 0x3f));
            out[2] = (byte)(0x80 | (in & 0x3f));
            return 3;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.