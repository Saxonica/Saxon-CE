package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* An xsl:output-character element in the stylesheet. <br>
*/

public class XSLOutputCharacter extends StyleElement {


    public void prepareAttributes() throws XPathException {
        checkAttribute("character", "s1");
        checkAttribute("string", "s1");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        checkEmpty();
        if (!(getParent() instanceof XSLCharacterMap)) {
            compileError("xsl:output-character may appear only as a child of xsl:character-map", "XTSE0010");
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.