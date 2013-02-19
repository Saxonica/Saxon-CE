package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.Block;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.Message;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.value.Whitespace;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* An xsl:message element in the stylesheet. <br>
*/

public final class XSLMessage extends StyleElement {

    private Expression terminate = null;
    private Expression select = null;

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

        String terminateAtt = null;
        String selectAtt = null;
		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f == StandardNames.TERMINATE) {
        		terminateAtt = Whitespace.trim(atts.getValue(a));
            } else if (f == StandardNames.SELECT) {
                selectAtt = atts.getValue(a);

            } else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }


        if (terminateAtt==null) {
            terminateAtt = "no";
        }

        terminate = makeAttributeValueTemplate(terminateAtt);
        if (terminate instanceof StringLiteral) {
            String t = ((StringLiteral)terminate).getStringValue();
            if (!(t.equals("yes") || t.equals("no"))) {
                compileError("terminate must be 'yes' or 'no'", "XTSE0020");
            }
        }
    }

    public void validate(Declaration decl) throws XPathException {
        select = typeCheck(select);
        terminate = typeCheck(terminate);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        Expression b = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
        if (b != null) {
            if (select == null) {
                select = b;
            } else {
                select = Block.makeBlock(select, b);
                select.setSourceLocator(this);
            }
        }
        if (select == null) {
            select = new StringLiteral("xsl:message (no content)");
        }
        Message inst = new Message(select, terminate);
        return inst;
    }

}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
