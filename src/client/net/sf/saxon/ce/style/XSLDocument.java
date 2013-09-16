package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.DocumentInstr;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* An xsl:document instruction in the stylesheet. <BR>
* This instruction creates a document node in the result tree, optionally
 * validating it.
*/

public class XSLDocument extends StyleElement {

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
        checkAttribute("validation", "v");
        checkAttribute("type", "t");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        //
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        DocumentInstr inst = new DocumentInstr(false, null, getBaseURI());
        Expression b = compileSequenceConstructor(exec, decl);
        if (b == null) {
            b = Literal.makeEmptySequence();
        }
        inst.setContentExpression(b);
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.//
