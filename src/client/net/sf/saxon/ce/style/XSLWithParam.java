package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.WithParam;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;

/**
* An xsl:with-param element in the stylesheet. <br>
* The xsl:with-param element has mandatory attribute name and optional attribute select
*/

public class XSLWithParam extends XSLGeneralVariable {

    protected boolean allowsAsAttribute() {
        return true;
    }

    protected boolean allowsTunnelAttribute() {
        return true;
    }

    public void validate(Declaration decl) throws XPathException {
        super.validate(decl);

        // Check for duplicate parameter names

        UnfailingIterator iter = iterateAxis(Axis.PRECEDING_SIBLING);
        while (true) {
            Item prev = iter.next();
            if (prev == null) {
                break;
            }
            if (prev instanceof XSLWithParam) {
                if (this.getVariableQName().equals(((XSLWithParam)prev).getVariableQName())) {
                    compileError("Duplicate parameter name", "XTSE0670");
                }
            }
        }

    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        PrincipalStylesheetModule psm = getPrincipalStylesheetModule();
        WithParam inst = new WithParam();
        inst.adoptChildExpression(select);
        inst.setParameterId(psm.allocateUniqueParameterNumber(getVariableQName()));
        initializeInstruction(exec, decl, inst);
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
