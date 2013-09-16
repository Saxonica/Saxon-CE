package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.ApplyImports;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* An xsl:apply-imports element in the stylesheet
*/

public class XSLApplyImports extends StyleElement {


    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
		checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        onlyAllow("with-param");
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        ApplyImports inst = new ApplyImports(ApplyImports.APPLY_IMPORTS);
        inst.setActualParameters(getWithParamInstructions(exec, decl, false, inst),
                                 getWithParamInstructions(exec, decl, true, inst));
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
