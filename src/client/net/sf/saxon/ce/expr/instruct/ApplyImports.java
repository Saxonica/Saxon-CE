package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.Rule;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;

import java.util.ArrayList;
import java.util.Iterator;

/**
* An xsl:apply-imports element in the stylesheet
*/

public class ApplyImports extends Instruction {

    WithParam[] actualParams = null;
    WithParam[] tunnelParams = null;

    public ApplyImports() {
    }

    /**
     * Set the actual parameters on the call
     */

    public void setActualParameters(
                        WithParam[] actualParams,
                        WithParam[] tunnelParams ) {
        this.actualParams = actualParams;
        this.tunnelParams = tunnelParams;
    }


    /**
     * Get the actual parameters passed to the called template
     * @return the non-tunnel parameters
     */

    public WithParam[] getActualParams() {
        return actualParams;
    }

    /**
     * Get the tunnel parameters passed to the called template
     * @return the tunnel parameters
     */

    public WithParam[] getTunnelParams() {
        return tunnelParams;
    }    

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception client.net.sf.saxon.ce.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        WithParam.simplify(actualParams, visitor);
        WithParam.simplify(tunnelParams, visitor);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        WithParam.typeCheck(actualParams, visitor, contextItemType);
        WithParam.typeCheck(tunnelParams, visitor, contextItemType);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        WithParam.optimize(visitor, actualParams, contextItemType);
        WithParam.optimize(visitor, tunnelParams, contextItemType);
        return this;
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }


    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true (which is almost invariably the case, so it's not worth
     * doing any further analysis to find out more precisely).
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        WithParam.promoteParams(this, actualParams, offer);
        WithParam.promoteParams(this, tunnelParams, offer);
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList list = new ArrayList(10);
        WithParam.getXPathExpressions(actualParams, list);
        WithParam.getXPathExpressions(tunnelParams, list);
        return list.iterator();
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();
        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        Rule currentTemplateRule = context.getCurrentTemplateRule();
        if (currentTemplateRule==null) {
            dynamicError("There is no current template rule", "XTDE0560");
        }

        int min = currentTemplateRule.getMinImportPrecedence();
        int max = currentTemplateRule.getPrecedence()-1;
        Mode mode = context.getCurrentMode();
        if (mode == null) {
            mode = controller.getRuleManager().getUnnamedMode();
        }
        if (context.getCurrentIterator()==null) {
            dynamicError("Cannot call xsl:apply-imports when there is no context item", "XTDE0565");
        }
        Item currentItem = context.getCurrentIterator().current();
        if (!(currentItem instanceof NodeInfo)) {
            dynamicError("Cannot call xsl:apply-imports when context item is not a node", "XTDE0565");
        }
        NodeInfo node = (NodeInfo)currentItem;
        Rule rule = controller.getRuleManager().getTemplateRule(node, mode, min, max, context);

		if (rule==null) {             // use the default action for the node
            mode.getBuiltInRuleSet().process(node, params, tunnels, context, getSourceLocator());
        } else {
            XPathContextMajor c2 = context.newContext();
            Template nh = (Template)rule.getAction();
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnels);
            c2.openStackFrame(nh.getStackFrameMap());
            c2.setCurrentTemplateRule(rule);
            nh.apply(c2);
        }
        return null;
                // We never treat apply-imports as a tail call, though we could
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
