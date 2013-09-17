package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.GlobalVariable;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.SequenceType;

import java.util.ArrayList;
import java.util.List;


/**
* Generic class for xsl:variable and xsl:param elements. <br>
*/

public abstract class XSLVariableDeclaration
        extends XSLGeneralVariable
        implements VariableDeclaration, StylesheetProcedure {

    // The slot number for the variable is allocated at this level (a) for global variables, and
    // (b) for local parameters. For local variables, slot numbers are allocated only after an entire
    // template or function has been compiled.

    private int slotNumber = -9876;  // initial value designed solely to show up when debugging

    // List of VariableReference objects that reference this XSLVariableDeclaration
    protected List<VariableReference> references = new ArrayList<VariableReference>(10);

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }    

    /**
     * Get the slot number allocated to this variable (its position in the stackframe)
     * @return the allocated slot number
     */

    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Allocate a slot number to this variable
     * @param slot the position of the variable on the local stack frame
     */

    public void setSlotNumber(int slot) {
        slotNumber = slot;
    }

    /**
     * Get the static type of the variable.
     * @return the static type declared for the variable
    */

    public abstract SequenceType getRequiredType();

    /**
    * Method called by VariableReference to register the variable reference for
    * subsequent fixup
    */

    public void registerReference(VariableReference ref) {
        references.add(ref);
    }

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction (well, it can be, anyway)
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Notify all references to this variable of the data type
    */

    public void fixupReferences() throws XPathException {
        final SequenceType type = getRequiredType();
        final TypeHierarchy th = TypeHierarchy.getInstance();
        for (VariableReference reference : references) {
            Sequence constantValue = null;
            int properties = 0;
            if (this instanceof XSLVariable) {
                if (select instanceof Literal) {
                    // we can't rely on the constant value because it hasn't yet been type-checked,
                    // which could change it (eg by numeric promotion). Rather than attempt all the type-checking
                    // now, we do a quick check. See test bug64
                    int relation = th.relationship(select.getItemType(), type.getPrimaryType());
                    if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                        constantValue = ((Literal) select).getValue();
                    }
                }
                if (select != null) {
                    properties = select.getSpecialProperties();
                }
            }
            (reference).setStaticType(type, constantValue, properties);
        }
        super.fixupReferences();
    }

    /**
    * Check that the variable is not already declared, and allocate a slot number
     * @param decl
     */

    public void validate(Declaration decl) throws XPathException {
        super.validate(decl);
        if (global) {
            if (!redundant) {
                slotNumber = getExecutable().allocateGlobalVariableSlot();
            }
        } 
    }

    /**
     * Notify all variable references of the Binding instruction
     * @param binding the Binding that represents this variable declaration in the executable code tree
    */

    protected void fixupBinding(Binding binding) {
        for (VariableReference reference : references) {
            (reference).fixup(binding);
        }
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
        top.indexVariableDeclaration(decl);
    }

    /**
     * Optimize the stylesheet construct
     * @param declaration
     */

    public void optimize(Declaration declaration) throws XPathException {
        if (global && !redundant && select!=null) {
            Expression exp2 = select;
            ExpressionVisitor visitor = makeExpressionVisitor();
            try {
                exp2 = exp2.optimize(visitor, AnyNodeTest.getInstance());

            } catch (XPathException err) {
                err.maybeSetLocation(this);
                compileError(err);
            }

            int numberOfSlots = ExpressionTool.allocateSlots(exp2, 0);
            ((GlobalVariable)compiledVariable).setContainsLocals(numberOfSlots);

            if (exp2 != select) {
                select = exp2;
                compiledVariable.setSelectExpression(select);
            }
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
