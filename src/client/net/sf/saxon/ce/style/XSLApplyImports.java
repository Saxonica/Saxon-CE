package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.ApplyImports;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.Whitespace;

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
        //checkWithinTemplate();
        UnfailingIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLWithParam) {
                // OK;
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValue())) {
                    compileError("No character data is allowed within xsl:apply-imports", "XTSE0010");
                }
            } else {
                compileError("Child element " + child.getDisplayName() +
                        " is not allowed as a child of xsl:apply-imports", "XTSE0010");
            }
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        ApplyImports inst = new ApplyImports();
        inst.setActualParameters(getWithParamInstructions(exec, decl, false, inst),
                                 getWithParamInstructions(exec, decl, true, inst));
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
