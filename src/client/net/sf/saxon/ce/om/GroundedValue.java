package client.net.sf.saxon.ce.om;

/**
 * A value that exists in memory and that can be directly addressed
 */
public interface GroundedValue extends Sequence {

    /**
     * Get the n'th item in the value, counting from 0
     * @param n the index of the required item, with 0 representing the first item in the sequence
     * @return the n'th item if it exists, or null otherwise
     */

    public Item itemAt(int n);

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
