package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.GeneralVariable;
import client.net.sf.saxon.ce.expr.instruct.GlobalVariable;
import client.net.sf.saxon.ce.expr.instruct.SlotManager;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Value;

import java.util.ArrayList;
import java.util.Iterator;
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
    protected List references = new ArrayList(10);

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
     * Get the SlotManager associated with this stylesheet construct. The SlotManager contains the
     * information needed to manage the local stack frames used by run-time instances of the code.
     * @return the associated SlotManager object
     */

    public SlotManager getSlotManager() {
        return slotManager;
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
        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        final Iterator iter = references.iterator();
        while (iter.hasNext()) {
            Value constantValue = null;
            int properties = 0;
            if (this instanceof XSLVariable) {
                if (select instanceof Literal) {
                    // we can't rely on the constant value because it hasn't yet been type-checked,
                    // which could change it (eg by numeric promotion). Rather than attempt all the type-checking
                    // now, we do a quick check. See test bug64
                    int relation = th.relationship(select.getItemType(th), type.getPrimaryType());
                    if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                        constantValue = ((Literal)select).getValue();
                    }
                }
                if (select != null) {
                    properties = select.getSpecialProperties();
                }
            }
            ((VariableReference)iter.next()).setStaticType(type, constantValue, properties);
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
                slotNumber = getExecutable().getGlobalVariableMap().allocateSlotNumber(getVariableQName());
            }
        } 
    }

    /**
     * Notify all variable references of the Binding instruction
     * @param binding the Binding that represents this variable declaration in the executable code tree
    */

    protected void fixupBinding(Binding binding) {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            ((VariableReference)iter.next()).fixup(binding);
        }
    }

    /**
     * Set the number of references to this variable. This code is invoked only for a global variable,
     * and only if there is at least one reference.
     * @param var the variable
     */

    protected void setReferenceCount(GeneralVariable var) {
        var.setReferenceCount(10);  // TODO: temporary
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
            Optimizer opt = getConfiguration().getOptimizer();
            try {
                if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
                    exp2 = exp2.optimize(visitor, AnyNodeTest.getInstance());
                }

            } catch (XPathException err) {
                err.maybeSetLocation(this);
                compileError(err);
            }

            // Try to extract new global variables from the body of the variable declaration
            // (but don't extract the whole body!)
//            if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
//                exp2 = opt.promoteExpressionsToGlobal(exp2, visitor, true);
//            }
            // dropped because it doesn't seem to do much good - just splits up an expression
            // into lots of small global variables.

            allocateSlots(exp2);
            if (slotManager != null && slotManager.getNumberOfVariables() > 0) {
                ((GlobalVariable)compiledVariable).setContainsLocals(slotManager);
            }

            if (exp2 != select) {
                select = exp2;
                compiledVariable.setSelectExpression(select);
            }
        }
    }

    /**
     * Get the compiled variable
     * @return the compiled variable if it has been compiled, or null otherwise
     */

    public GeneralVariable getCompiledVariable() {
        return compiledVariable;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
