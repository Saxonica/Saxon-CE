package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* An xsl:character-map declaration in the stylesheet. <br>
*/

public class XSLCharacterMap extends StyleElement {

    String use;
                // the value of the use-character-maps attribute, as supplied

    boolean validated = false;
                // set to true once validate() has been called


    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
     * Validate the attributes on this instruction
     * @throws XPathException
     */

    public void prepareAttributes() throws XPathException {
        setObjectName((StructuredQName)checkAttribute("name", "q1"));
        use = (String)checkAttribute("use-character-maps", "s");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {

        if (validated) return;

        // check that this is a top-level declaration

        checkTopLevel(null);

        // check that the only children are xsl:output-character elements

        onlyAllow("output-character");

        validated = true;
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.