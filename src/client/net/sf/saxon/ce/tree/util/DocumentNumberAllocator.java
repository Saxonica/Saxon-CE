package client.net.sf.saxon.ce.tree.util;

;

/**
 * This class (which has one instance per Configuration) is used to allocate unique document
 * numbers. It's a separate class so that it can act as a monitor for synchronization
 */
public class DocumentNumberAllocator  {

    private int nextDocumentNumber = 0;

    /**
     * Allocate a unique document number
     * @return a unique document number
     */

    public synchronized int allocateDocumentNumber() {
        return nextDocumentNumber++;
    }
}

// Contributors: Michael Kay (Saxonica), Wolfgang Hoschek

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
