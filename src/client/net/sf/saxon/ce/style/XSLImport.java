package client.net.sf.saxon.ce.style;


/**
* xsl:import element in the stylesheet. <br>
*/

public class XSLImport extends XSLGeneralIncorporate {

    /**
    * isImport() returns true if this is an xsl:import statement rather than an xsl:include
    */

    public boolean isImport() {
        return true;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
