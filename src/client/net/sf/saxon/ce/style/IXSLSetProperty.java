package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.SetProperty;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.trans.XPathException;

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
        targetObject = (Expression)checkAttribute("object", "e");
        select = (Expression)checkAttribute("select", "e");
        name = (Expression)checkAttribute("name", "a");
        checkForUnknownAttributes();

        if (targetObject == null) {
            targetObject = new IXSLFunction("window", new Expression[0]);
        }
		
	}

	@Override
	public Expression compile(Executable exec, Declaration decl)
			throws XPathException {

        return new SetProperty(targetObject, select, name);
	}

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

