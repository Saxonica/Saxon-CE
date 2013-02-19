package client.net.sf.saxon.ce.om;

/**
 * A value that exists in memory and that can be directly addressed
 */
public interface GroundedValue extends ValueRepresentation {

    /**
     * Get the n'th item in the value, counting from 0
     * @param n the index of the required item, with 0 representing the first item in the sequence
     * @return the n'th item if it exists, or null otherwise
     */

    public Item itemAt(int n);

    /**
     * Get a subsequence of the value
     * @param start the index of the first item to be included in the result, counting from zero.
     * A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     * sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     * get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     * is returned. If the value goes off the end of the sequence, the result returns items up to the end
     * of the sequence
     * @return the required subsequence. If min is
     */

    public GroundedValue subsequence(int start, int length);
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
