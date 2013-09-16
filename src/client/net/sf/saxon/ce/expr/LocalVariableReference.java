package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * Variable reference: a reference to a local variable. This subclass of VariableReference
 * bypasses the Binding object to get the value directly from the relevant slot in the local
 * stackframe.
 */

public class LocalVariableReference extends VariableReference {

    int slotNumber = -999;

    /**
     * Create a local variable reference. The binding and slot number will be supplied later
     */

    public LocalVariableReference() {
    }

    /**
     * Create a LocalVariableReference bound to a given Binding
     * @param binding the binding (that is, the declaration of this local variable)
     */

    public LocalVariableReference(Binding binding) {
        super(binding);
    }

    /**
     * Create a clone copy of this VariableReference
     * @return the cloned copy
     */

    private Expression copy() {
        if (binding == null) {
            throw new UnsupportedOperationException("Cannot copy a variable reference whose binding is unknown");
        }
        LocalVariableReference ref = new LocalVariableReference();
        ref.binding = binding;
        ref.staticType = staticType;
        ref.slotNumber = slotNumber;
        ref.constantValue = constantValue;
        ref.displayName = displayName;
        ExpressionTool.copyLocationInfo(this, ref);
        return ref;
    }

    /**
     * Set the slot number for this local variable, that is, its position in the local stack frame
     * @param slotNumber the slot number to be used
     */

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    /**
     * Get the slot number allocated to this local variable
     * @return the slot number
     */

    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Return the value of the variable
     * @param c the XPath dynamic context
     * @return the value of the variable
     * @throws XPathException if any dynamic error occurs while evaluating the variable
     */

    public Sequence evaluateVariable(XPathContext c) throws XPathException {
        try {
            return c.getStackFrame()[slotNumber];
        } catch (ArrayIndexOutOfBoundsException err) {
            if (slotNumber == -999) {
                throw new ArrayIndexOutOfBoundsException(
                        "Local variable " + getDisplayName() + " has not been allocated a stack frame slot");
            }
            throw err;
        }
    }

    /**
     * Replace this VariableReference where appropriate by a more efficient implementation. This
     * can only be done after all slot numbers are allocated. The efficiency is gained by binding the
     * VariableReference directly to a local or global slot, rather than going via the Binding object
     *
     * @param parent the parent expression of this variable reference
     */

//    public void refineVariableReference(Expression parent) {
//        // no-op
//    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
