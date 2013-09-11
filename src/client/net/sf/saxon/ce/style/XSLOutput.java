package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* An xsl:output element in the stylesheet.
*/

public class XSLOutput extends StyleElement {

//    When serialization is not being performed, either because the implementation does not support the
//    serialization option, or because the user is executing the transformation in a way that does not
//    invoke serialization, then the content of the xsl:output and xsl:character-map declarations has no effect.
//    Under these circumstances the processor may report any errors in an xsl:output or xsl:character-map
//    declaration, or in the serialization attributes of xsl:result-document, but is not required to do so.

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }    

    public void prepareAttributes() throws XPathException {
        //no action
    }

    public void validate(Declaration decl) throws XPathException {
        checkTopLevel(null);
        checkEmpty();
    }

    public Expression compile(Executable exec, Declaration decl) {
        return null;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
