package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.Comment;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.StringValue;

/**
* An xsl:comment elements in the stylesheet. <br>
*/

public final class XSLComment extends XSLLeafNodeConstructor {

    public void prepareAttributes() throws XPathException {
        select = (Expression)checkAttribute("select", "e");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        select = typeCheck(select);
        super.validate(decl);
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0940";
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        Comment inst = new Comment();
        compileContent(exec, decl, inst, new StringLiteral(StringValue.SINGLE_SPACE));
        return inst;
    }

}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
