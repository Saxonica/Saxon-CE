package client.net.sf.saxon.ce.tree.linked;

 /**
  * System IDs are not held in nodes in the tree, because they are usually the same
  * for a whole document.
  * This class provides a map from element sequence numbers to System IDs: it is
  * linked to the root node of the tree.
  * Note that the System ID is not necessarily the same as the Base URI. The System ID relates
  * to the external entity in which a node was physically located; this provides a default for
  * the Base URI, but this may be modified by specifying an xml:base attribute
  *
  * @author Michael H. Kay
  */

public class SystemIdMap {

    private int[] sequenceNumbers;
    private String[] uris;
    private int allocated;

    public SystemIdMap() {
        sequenceNumbers = new int[4];
        uris = new String[4];
        allocated = 0;
    }

    /**
    * Set the system ID corresponding to a given sequence number
    */

    public void setSystemId(int sequence, String uri) {
        // ignore it if same as previous
        if (allocated>0 && uri.equals(uris[allocated-1])) {
            return;
        }
        if (sequenceNumbers.length <= allocated + 1) {
            int[] s = new int[allocated * 2];
            String[] u = new String[allocated * 2];
            System.arraycopy(sequenceNumbers, 0, s, 0, allocated);
            System.arraycopy(uris, 0, u, 0, allocated);
            sequenceNumbers = s;
            uris = u;
        }
        sequenceNumbers[allocated] = sequence;
        uris[allocated] = uri;
        allocated++;
    }

    /**
    * Get the system ID corresponding to a given sequence number
    */

    public String getSystemId(int sequence) {
        if (allocated==0) return null;
        // could use a binary chop, but it's not important
        for (int i=1; i<allocated; i++) {
            if (sequenceNumbers[i] > sequence) {
                return uris[i-1];
            }
        }
        return uris[allocated-1];
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
