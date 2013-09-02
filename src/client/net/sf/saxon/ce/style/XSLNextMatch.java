package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.NextMatch;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.value.Whitespace;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;

/**
* An xsl:next-match element in the stylesheet
*/

public class XSLNextMatch extends StyleElement {

    private boolean useTailRecursion = false;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain an xsl:fallback
    * instruction
    */

    public boolean mayContainFallback() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
        	checkUnknownAttribute(qn);
        }
    }

    public void validate(Declaration decl) throws XPathException {
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLWithParam || child instanceof XSLFallback) {
                // OK;
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValue())) {
                    compileError("No character data is allowed within xsl:next-match", "XTSE0010");
                }
            } else {
                compileError("Child element " + child.getDisplayName() +
                        " is not allowed as a child of xsl:next-match", "XTSE0010");
            }
        }

    }


    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
     */

    protected boolean markTailCalls() {
        useTailRecursion = true;
        return true;
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        NextMatch inst = new NextMatch(useTailRecursion);
        inst.setActualParameters(getWithParamInstructions(exec, decl, false, inst),
                                 getWithParamInstructions(exec, decl, true, inst));
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
