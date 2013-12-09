package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.instruct.AnalyzeString;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.NodeImpl;
import client.net.sf.saxon.ce.type.ItemType;

/**
 * An xsl:analyze-string elements in the stylesheet. New at XSLT 2.0<BR>
 */

public class XSLAnalyzeString extends StyleElement {

    private Expression select;
    private Expression regex;
    private Expression flags;
    private StyleElement matching;
    private StyleElement nonMatching;

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
        select = (Expression) checkAttribute("select", "e1");
        regex = (Expression) checkAttribute("regex", "a1");
        flags = (Expression) checkAttribute("flags", "a");
        if (flags == null) {
            flags = makeAttributeValueTemplate("");
        }
        checkForUnknownAttributes();
    }


    public void validate(Declaration decl) throws XPathException {
        //checkWithinTemplate();

        int state = 0;
        for (NodeImpl child : allChildren()) {
            if (child instanceof XSLFallback) {
                state = 3;
            } else if (child instanceof XSLMatchingSubstring) {
                boolean b = child.getLocalPart().equals("matching-substring");
                if (b) {
                    if (state != 0) {
                        outOfOrder("XTSE0010");
                    }
                    state = 1;
                    matching = (StyleElement) child;
                } else {
                    if (state >= 2) {
                        outOfOrder("XTSE0010");
                    }
                    state = 2;
                    nonMatching = (StyleElement) child;
                }
            } else {
                outOfOrder("XTSE0010");
            }
        }

        if (matching == null && nonMatching == null) {
            outOfOrder("XTSE1130");
        }

        select = typeCheck(select);
        regex = typeCheck(regex);
        flags = typeCheck(flags);

    }

    private void outOfOrder(String code) throws XPathException {
        compileError("Content model for xsl:analyze-string is (xsl:matching-substring? xsl:non-matching-substring? xsl:fallback*)", code);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        Expression matchingBlock = null;
        if (matching != null) {
            matchingBlock = matching.compileSequenceConstructor(exec, decl);
        }

        Expression nonMatchingBlock = null;
        if (nonMatching != null) {
            nonMatchingBlock = nonMatching.compileSequenceConstructor(exec, decl);
        }

        try {
            ExpressionVisitor visitor = makeExpressionVisitor();
            return new AnalyzeString(select,
                    regex,
                    flags,
                    (matchingBlock == null ? null : matchingBlock.simplify(visitor)),
                    (nonMatchingBlock == null ? null : nonMatchingBlock.simplify(visitor)));
        } catch (XPathException e) {
            compileError(e);
            return null;
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
