package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.DocumentInstr;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.lib.Validation;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;

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
		AttributeCollection atts = getAttributeList();

        String validationAtt = null;
        String typeAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
            if (f.equals("validation")) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("type")) {
                typeAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }

        if (validationAtt != null && Validation.getCode(validationAtt) != Validation.STRIP) {
            compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
        }
        if (typeAtt!=null) {
            compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
        }
    }

    public void validate(Declaration decl) throws XPathException {
        //
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        DocumentInstr inst = new DocumentInstr(false, null, getBaseURI());
        Expression b = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
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
