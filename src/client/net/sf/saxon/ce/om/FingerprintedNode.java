package client.net.sf.saxon.ce.om;

/**
 * This is a marker interface used to identify nodes that contain a namepool fingerprint. Although all nodes
 * are capable of returning a fingerprint, some (notably DOM, XOM, and JDOM nodes) need to calculate it on demand.
 * A node that implements this interface indicates that obtaining the fingerprint for use in name comparisons
 * is more efficient than using the URI and local name.
 */

public interface FingerprintedNode {}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

