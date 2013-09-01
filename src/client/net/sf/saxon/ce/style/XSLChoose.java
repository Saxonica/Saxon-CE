package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.om.StructuredQName;
import com.google.gwt.logging.client.LogConfiguration;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.BooleanValue;

/**
* An xsl:choose elements in the stylesheet. <br>
*/

public class XSLChoose extends StyleElement {

    private StyleElement otherwise;
    private int numberOfWhens = 0;

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

    public void prepareAttributes() throws XPathException {
		AttributeCollection atts = getAttributeList();
		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
        	checkUnknownAttribute(qn);
        }
    }

    public void validate(Declaration decl) throws XPathException {
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLWhen) {
                if (otherwise!=null) {
                    otherwise.compileError("xsl:otherwise must come last", "XTSE0010");
                }
                numberOfWhens++;
            } else if (curr instanceof XSLOtherwise) {
                if (otherwise!=null) {
                    ((XSLOtherwise)curr).compileError("Only one xsl:otherwise is allowed in an xsl:choose", "XTSE0010");
                } else {
                    otherwise = (StyleElement)curr;
                }
            } else if (curr instanceof StyleElement) {
                ((StyleElement)curr).compileError("Only xsl:when and xsl:otherwise are allowed here", "XTSE0010");
            } else {
                compileError("Only xsl:when and xsl:otherwise are allowed within xsl:choose", "XTSE0010");
            }
        }

        if (numberOfWhens==0) {
            compileError("xsl:choose must contain at least one xsl:when", "XTSE0010");
        }
    }

    /**
    * Mark tail-recursive calls on templates and functions.
    */

    public boolean markTailCalls() {
        boolean found = false;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                return found;
            }
            if (curr instanceof StyleElement) {
                found |= ((StyleElement)curr).markTailCalls();
            }
        }
    }


    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        int entries = numberOfWhens + (otherwise==null ? 0 : 1);
        Expression[] conditions = new Expression[entries];
        Expression[] actions = new Expression[entries];
        String[] conditionTests = null;
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()){
        	conditionTests = new String[entries];
        }

        int w = 0;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLWhen) {
                conditions[w] = ((XSLWhen)curr).getCondition();
                Expression b = ((XSLWhen)curr).compileSequenceConstructor(
                        exec, decl, curr.iterateAxis(Axis.CHILD));
                if (b == null) {
                    b = Literal.makeEmptySequence();
                }
                try {
                    b = makeExpressionVisitor().simplify(b);
                    if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                    	String test = XSLIf.getTestAttribute((XSLWhen)curr);
                    	conditionTests[w] = test;
                    }
                    actions[w] = b;
                } catch (XPathException e) {
                    compileError(e);
                }

                // Optimize for constant conditions (true or false)
                if (conditions[w] instanceof Literal && ((Literal)conditions[w]).getValue() instanceof BooleanValue) {
                    if (((BooleanValue)((Literal)conditions[w]).getValue()).getBooleanValue()) {
                        // constant true: truncate the tests here
                        entries = w+1;
                        break;
                    } else {
                        // constant false: omit this test
                        w--;
                        entries--;
                    }
                }
                w++;
            } else if (curr instanceof XSLOtherwise) {
                conditions[w] = Literal.makeLiteral(BooleanValue.TRUE);
                Expression b = ((XSLOtherwise)curr).compileSequenceConstructor(
                        exec, decl, curr.iterateAxis(Axis.CHILD));
                if (b == null) {
                    b = Literal.makeEmptySequence();
                }
                try {
                    b = makeExpressionVisitor().simplify(b);
                    if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                    	conditionTests[w] = "";
                    }                   
                    actions[w] = b;
                } catch (XPathException e) {
                    compileError(e);
                }
                w++;
            } else {
                // Ignore: problem has already been reported.
            }
        }

        if (conditions.length != entries) {
            // we've optimized some entries away
            if (entries==0) {
                return null; // return a no-op
            }
            if (entries==1 && (conditions[0] instanceof Literal) &&
                    ((Literal)conditions[0]).getValue() instanceof BooleanValue) {
                if (((BooleanValue)((Literal)conditions[0]).getValue()).getBooleanValue()) {
                    // only one condition left, and it's known to be true: return the corresponding action
                    return actions[0];
                } else {
                    // but if it's false, return a no-op
                    return null;
                }
            }
            Expression[] conditions2 = new Expression[entries];
            System.arraycopy(conditions, 0, conditions2, 0, entries);
            Expression[] actions2 = new Expression[entries];
            System.arraycopy(actions, 0, actions2, 0, entries);
            conditions = conditions2;
            actions = actions2;
        }
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	Choose ch = new Choose(conditions, actions);
        	ch.setConditionTests(conditionTests);
        	return ch;
        } else {
        	return new Choose(conditions, actions);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
