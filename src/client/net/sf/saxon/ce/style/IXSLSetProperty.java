package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.Message;
import client.net.sf.saxon.ce.expr.instruct.SetProperty;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;

public class IXSLSetProperty extends StyleElement {
	
    private Expression targetObject = null;
    private Expression select = null;
    private Expression name = null;

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
        return false;
    }

	@Override
	protected void prepareAttributes() throws XPathException {
        String objectAtt = null;
        String selectAtt = null;
        String nameAtt = null;
		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f == "object") {
        		objectAtt = Whitespace.trim(atts.getValue(a));
            } else if (f == StandardNames.SELECT) {
                selectAtt = atts.getValue(a);
            } else if (f == StandardNames.NAME) {
                nameAtt = atts.getValue(a);
            } else {
        		checkUnknownAttribute(nc);
        	}
        }

        select = makeExpression(selectAtt);
        targetObject = (objectAtt != null)?
        	makeExpression(objectAtt) : new IXSLFunction("window", new Expression[0]);


        name = makeAttributeValueTemplate(nameAtt);
        // check nameatt?
        if (name instanceof StringLiteral) {
            String t = ((StringLiteral)name).getStringValue();
            if (t.length() == 0) {
                compileError("name must be a JavaScript property name - or names separated by '.'");
            }
        }
		
	}

	@Override
	public Expression compile(Executable exec, Declaration decl)
			throws XPathException {

        SetProperty inst = new SetProperty(targetObject, select, name);
        return inst;
	}

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

