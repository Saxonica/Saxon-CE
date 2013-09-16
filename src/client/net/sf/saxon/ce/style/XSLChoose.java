package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.NodeImpl;
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
        for (NodeImpl child: allChildren()) {
            if (child instanceof XSLWhen) {
                if (otherwise != null) {
                    otherwise.compileError("xsl:otherwise must come last", "XTSE0010");
                }
                numberOfWhens++;
            } else if (child instanceof XSLOtherwise) {
                if (otherwise != null) {
                    ((XSLOtherwise) child).compileError("Only one xsl:otherwise is allowed in an xsl:choose", "XTSE0010");
                } else {
                    otherwise = (StyleElement) child;
                }
            } else {
                StyleElement se = (child instanceof StyleElement ? (StyleElement)child : this);
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
        for (NodeImpl child : allChildren()) {
            found |= ((StyleElement) child).markTailCalls();
        }
        return found;
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
        ExpressionVisitor visitor = makeExpressionVisitor();
        for (NodeImpl child : allChildren()) {
            Expression action = ((StyleElement)child).compileSequenceConstructor(exec, decl);
            actions[w] = visitor.simplify(action);
            if (child instanceof XSLWhen) {
                conditions[w] = ((XSLWhen) child).getCondition();
            } else if (child instanceof XSLOtherwise) {
                conditions[w] = Literal.makeLiteral(BooleanValue.TRUE);
            } else {
                // Ignore: problem has already been reported.
            }
            if (conditionTests != null) {
                conditionTests[w] = (child instanceof XSLWhen ? ((XSLWhen)child).getAttributeValue("", "test") : "");
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
