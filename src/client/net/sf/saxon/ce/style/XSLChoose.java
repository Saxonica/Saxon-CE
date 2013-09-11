package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.BooleanValue;
import com.google.gwt.logging.client.LogConfiguration;

/**
 * An xsl:choose element in the stylesheet. <br>
 */

public class XSLChoose extends StyleElement {

    private StyleElement otherwise;
    private int numberOfWhens = 0;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     *
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }

    public void prepareAttributes() throws XPathException {
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        UnfailingIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo curr = (NodeInfo) kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLWhen) {
                if (otherwise != null) {
                    otherwise.compileError("xsl:otherwise must come last", "XTSE0010");
                }
                numberOfWhens++;
            } else if (curr instanceof XSLOtherwise) {
                if (otherwise != null) {
                    ((XSLOtherwise) curr).compileError("Only one xsl:otherwise is allowed in an xsl:choose", "XTSE0010");
                } else {
                    otherwise = (StyleElement) curr;
                }
            } else {
                StyleElement se = (curr instanceof StyleElement ? (StyleElement)curr : this);
                se.compileError("Only xsl:when and xsl:otherwise are allowed within xsl:choose", "XTSE0010");
            }
        }

        if (numberOfWhens == 0) {
            compileError("xsl:choose must contain at least one xsl:when", "XTSE0010");
        }
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     */

    public boolean markTailCalls() {
        boolean found = false;
        UnfailingIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo curr = (NodeInfo) kids.next();
            if (curr == null) {
                return found;
            }
            if (curr instanceof StyleElement) {
                found |= ((StyleElement) curr).markTailCalls();
            }
        }
    }


    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        int entries = numberOfWhens + (otherwise == null ? 0 : 1);
        Expression[] conditions = new Expression[entries];
        Expression[] actions = new Expression[entries];
        String[] conditionTests = null;
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
            conditionTests = new String[entries];
        }

        int w = 0;
        UnfailingIterator kids = iterateAxis(Axis.CHILD);
        ExpressionVisitor visitor = makeExpressionVisitor();
        while (true) {
            NodeInfo curr = (NodeInfo) kids.next();
            if (curr == null) {
                break;
            }
            Expression action = ((StyleElement)curr).compileSequenceConstructor(
                        exec, decl, curr.iterateAxis(Axis.CHILD));
            actions[w] = visitor.simplify(action);
            if (curr instanceof XSLWhen) {
                conditions[w] = ((XSLWhen) curr).getCondition();
            } else if (curr instanceof XSLOtherwise) {
                conditions[w] = Literal.makeLiteral(BooleanValue.TRUE);
            } else {
                // Ignore: problem has already been reported.
            }
            if (conditionTests != null) {
                conditionTests[w] = (curr instanceof XSLWhen ? ((XSLWhen)curr).getAttributeValue("", "test") : "");
            }
            w++;
        }

        Choose ch = new Choose(conditions, actions);
        ch.setConditionTests(conditionTests);
        return ch;

    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
