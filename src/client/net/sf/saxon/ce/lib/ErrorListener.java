package client.net.sf.saxon.ce.lib;

import client.net.sf.saxon.ce.trans.XPathException;

/**
 * Simplified variant of the JAXP ErrorListener interface
 */
public interface ErrorListener {

    public void error(XPathException err);
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
