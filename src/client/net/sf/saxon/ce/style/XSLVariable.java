package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.GeneralVariable;
import client.net.sf.saxon.ce.expr.instruct.GlobalVariable;
import client.net.sf.saxon.ce.expr.instruct.LocalVariable;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.SequenceType;

/**
* Handler for xsl:variable elements in stylesheet. <br>
* The xsl:variable element has mandatory attribute name and optional attribute select
*/

public class XSLVariable extends XSLVariableDeclaration {

    private int state = 0;
            // 0 = before prepareAttributes()
            // 1 = during prepareAttributes()
            // 2 = after prepareAttributes()

    public void prepareAttributes() throws XPathException {
        if (state==2) return;
        if (state==1) {
            compileError("Circular reference to variable", "XTDE0640");
        }
        state = 1;
        //System.err.println("Prepare attributes of $" + getVariableName());
        super.prepareAttributes();
        state = 2;
    }

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction (well, it can be, anyway)
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Get the static type of the variable. This is the declared type, unless the value
    * is statically known and constant, in which case it is the actual type of the value.
    */

    public SequenceType getRequiredType() {
        // System.err.println("Get required type of $" + getVariableName());
        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        SequenceType defaultType = (requiredType==null ? SequenceType.ANY_SEQUENCE : requiredType);
        if (requiredType != null) {
            return requiredType;
        } else if (select!=null) {
            if (Literal.isEmptySequence(select)) {
                // returning Type.EMPTY gives problems with static type checking
                return defaultType;
            } else {
                try {
                    // try to infer the type from the select expression
                    return SequenceType.makeSequenceType(select.getItemType(th), select.getCardinality());
                } catch (Exception err) {
                    // this may fail because the select expression references a variable or function
                    // whose type is not yet known, because of forwards (perhaps recursive) references.
                    return defaultType;
                }
            }
        } else if (hasChildNodes()) {
            return SequenceType.makeSequenceType(NodeKindTest.DOCUMENT, StaticProperty.EXACTLY_ONE);
        } else {
            // no select attribute or content: value is an empty string
            return SequenceType.SINGLE_STRING;
        }
    }

    /**
     * Compile: used only for global variables.
     * This method ensures space is available for local variables declared within
     * this global variable
     */

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        if (references.isEmpty()) {
            redundant = true;
        }

        if (!redundant) {
            GeneralVariable inst;
            if (global) {
                inst = new GlobalVariable();
                ((GlobalVariable)inst).setExecutable(getExecutable());
                if (select != null) {
                    select.setContainer(((GlobalVariable)inst));
                }
                initializeInstruction(exec, decl, inst);
                inst.setVariableQName(getVariableQName());
                inst.setSlotNumber(getSlotNumber());
                inst.setRequiredType(getRequiredType());
                fixupBinding(inst);
                inst.setContainer(((GlobalVariable)inst));
                compiledVariable = inst;
                return inst;
            } else {
                throw new AssertionError("Local variable found when compiling a global variable");
            }
        }

        return null;
    }

    public Expression compileLocalVariable(Executable exec, Declaration decl) throws XPathException {

        if (references.isEmpty()) {
            redundant = true;
        }

        if (!redundant) {
            GeneralVariable inst;
            if (global) {
                throw new AssertionError("Global variable found when compiling local variable");
            } else {
                inst = new LocalVariable();
                inst.setContainer(this);
                if (select != null) {
                    select.setContainer(this);
                }
                initializeInstruction(exec, decl, inst);
                inst.setVariableQName(getVariableQName());
                inst.setRequiredType(getRequiredType());
                return inst;
            }
        }

        return null;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
