package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.CopyOf;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* An xsl:copy-of element in the stylesheet. <br>
*/

public final class XSLCopyOf extends StyleElement {

    private Expression select;
    private boolean copyNamespaces;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
        select = (Expression)checkAttribute("select", "e1");
        Boolean b = (Boolean)checkAttribute("copy-namespaces", "b");
        if (b != null) {
            copyNamespaces = b;
        }
        checkAttribute("type", "t");
        checkAttribute("validation", "v");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        checkEmpty();
        select = typeCheck(select);
    }

    public Expression compile(Executable exec, Declaration decl) {
        CopyOf inst = new CopyOf(select, copyNamespaces);
        inst.setStaticBaseUri(getBaseURI());
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
