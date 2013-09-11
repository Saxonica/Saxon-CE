package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.RoleLocator;
import client.net.sf.saxon.ce.expr.SuppliedParameterReference;
import client.net.sf.saxon.ce.expr.TypeChecker;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Whitespace;

/**
* An xsl:param element in the stylesheet. <br>
* The xsl:param element has mandatory attribute name and optional attributes
 *  select, required, as, ...
*/

public class XSLParam extends XSLVariableDeclaration {

    Expression conversion = null;

    protected boolean allowsValue() {
        return !(getParent() instanceof XSLFunction);
        // function parameters cannot take a default value
    }

    protected boolean allowsRequired() {
        return ((StyleElement)getParent()).mayContainParam("required");
    }

    protected boolean allowsTunnelAttribute() {
        return true;
    }

    public void validate(Declaration decl) throws XPathException {

        NodeInfo parent = getParent();
        global = (parent instanceof XSLStylesheet);

        if (!((parent instanceof StyleElement) && ((StyleElement)parent).mayContainParam(null))) {
            compileError("xsl:param must be immediately within a template, function or stylesheet", "XTSE0010");
        }

        if (!global) {
            UnfailingIterator preceding = iterateAxis(Axis.PRECEDING_SIBLING);
            while (true) {
                NodeInfo node = (NodeInfo)preceding.next();
                if (node == null) {
                    break;
                }
                if (node instanceof XSLParam) {
                    if (this.getVariableQName().equals(((XSLParam)node).getVariableQName())) {
                        compileError("The name of the parameter is not unique", "XTSE0580");
                    }
                } else if (node instanceof StyleElement) {
                    compileError("xsl:param must not be preceded by other instructions", "XTSE0010");
                } else {
                    // it must be a text node; allow it if all whitespace
                    if (!Whitespace.isWhite(node.getStringValue())) {
                        compileError("xsl:param must not be preceded by text", "XTSE0010");
                    }
                }
            }

            SlotManager p = getContainingSlotManager();
            if (p==null) {
                compileError("Local variable must be declared within a template or function", "XTSE0010");
            } else {
                setSlotNumber(p.allocateSlotNumber(getVariableQName()));
            }

        }

        if (requiredParam) {
            if (select != null) {
                // NB, we do this test before setting the default select attribute
                compileError("The select attribute should be omitted when required='yes'", "XTSE0010");
            }
            if (hasChildNodes()) {
                compileError("A parameter specifying required='yes' must have empty content", "XTSE0010");
            }
        }

        super.validate(decl);
    }

    /**
    * Compile: this ensures space is available for local variables declared within
    * this global variable
    */

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        if (redundant) {
            return null;
        }
        if (getParent() instanceof XSLFunction) {
            // Do nothing. We did everything necessary while compiling the XSLFunction element.
            return null;
        } else {
            int slot = getSlotNumber();
            if (requiredType != null) {
                SuppliedParameterReference pref = new SuppliedParameterReference(slot);
                pref.setSourceLocator(this);
                RoleLocator role = new RoleLocator(RoleLocator.PARAM, getVariableDisplayName(), 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                role.setErrorCode("XTTE0590");
                conversion = TypeChecker.staticTypeCheck(
                        pref,
                        requiredType,
                        false,
                        role, makeExpressionVisitor());
            }

            GeneralVariable inst;
            if (global) {
                inst = new GlobalParam();
                ((GlobalParam)inst).setExecutable(getExecutable());
                inst.setContainer(((GlobalParam)inst));
                if (isRequiredParam()) {
                    getExecutable().addRequiredParam(getVariableQName());
                }
                if (select != null) {
                    select.setContainer(((GlobalVariable)inst));
                }
                compiledVariable = inst;
            } else {
                PrincipalStylesheetModule psm = getPrincipalStylesheetModule();
                inst = new LocalParam();
                ((LocalParam)inst).setConversion(conversion);
                ((LocalParam)inst).setParameterId(psm.allocateUniqueParameterNumber(getVariableQName()));
            }
            initializeInstruction(exec, decl, inst);
            inst.setVariableQName(getVariableQName());
            inst.setSlotNumber(slot);
            inst.setRequiredType(getRequiredType());
            fixupBinding(inst);
            compiledVariable = inst;
            return inst;
        }
    }


    /**
    * Get the static type of the parameter. This is the declared type, because we cannot know
    * the actual value in advance.
    */

    public SequenceType getRequiredType() {
        if (requiredType!=null) {
            return requiredType;
        } else {
            return SequenceType.ANY_SEQUENCE;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
