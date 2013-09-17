package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionTool;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import com.google.gwt.logging.client.LogConfiguration;


/**
* Handler for xsl:if elements in stylesheet. <br>
* The xsl:if element has a mandatory attribute test, a boolean expression.
* The content is output if the test condition is true.
*/

public class XSLIf extends StyleElement {

    private Expression test;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
        test = (Expression)checkAttribute("test", "e1");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        test = typeCheck(test);
    }

    /**
    * Mark tail-recursive calls on stylesheet functions. For most instructions, this does nothing.
    */

    public boolean markTailCalls() {
        StyleElement last = getLastChildInstruction();
        return last != null && last.markTailCalls();
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        if (test instanceof Literal) {
            Sequence testVal = ((Literal)test).getValue();
            // condition known statically, so we only need compile the code if true.
            // This can happen with expressions such as test="function-available('abc')".
            try {
                if (ExpressionTool.effectiveBooleanValue(testVal.iterate())) {
                    return compileSequenceConstructor(exec, decl);
                } else {
                    return null;
                }
            } catch (XPathException err) {
                // fall through to non-optimizing case
            }
        }

        Expression action = compileSequenceConstructor(exec, decl);
        if (action == null) {
            return null;
        }
        Expression[] conditions = {test};
        Expression[] actions = {action};

        Choose inst = new Choose(conditions, actions);
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	inst.AddTraceProperty("test", getAttributeValue("", "test"));
        }
        return inst;
    }


}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
