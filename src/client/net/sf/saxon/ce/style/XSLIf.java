package client.net.sf.saxon.ce.style;
import com.google.gwt.logging.client.LogConfiguration;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.Value;


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
        test = prepareTestAttribute(this);
        if (test==null) {
            reportAbsence("test");
        }
    }

    /**
     * Process all the attributes, for an element where the only permitted attribute is "test"
     * @param se the containing element
     * @return the expression represented by the test attribute, or null if the attribute is absent
     * @throws XPathException if an error is encountered
     */

    public static Expression prepareTestAttribute(StyleElement se) throws XPathException {

        String testAtt=null;

		AttributeCollection atts = se.getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = se.getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.TEST)) {
        		testAtt = atts.getValue(a);
        	} else {
        		se.checkUnknownAttribute(nc);
        	}
        }

        if (testAtt==null) {
            return null;
        } else {
            return se.makeExpression(testAtt);
        }
    }
    
    public static String getTestAttribute(StyleElement se) throws XPathException {

        String testAtt=null;

		AttributeCollection atts = se.getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = se.getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.TEST)) {
        		testAtt = atts.getValue(a);
        	} else {
        		se.checkUnknownAttribute(nc);
        	}
        }

        return testAtt;
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
            Value testVal = ((Literal)test).getValue();
            // condition known statically, so we only need compile the code if true.
            // This can happen with expressions such as test="function-available('abc')".
            try {
                if (testVal.effectiveBooleanValue()) {
                    return compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
//                    Block block = new Block();
//                    block.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
//                    compileChildren(exec, block, true);
//                    return block.simplify(getStaticContext());
                } else {
                    return null;
                }
            } catch (XPathException err) {
                // fall through to non-optimizing case
            }
        }

        Expression action = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
        if (action == null) {
            return null;
        }
        Expression[] conditions = {test};
        Expression[] actions = {action};

        Choose inst = new Choose(conditions, actions);
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	inst.AddTraceProperty("test", getTestAttribute(this));
        }
        return inst;
    }


}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
