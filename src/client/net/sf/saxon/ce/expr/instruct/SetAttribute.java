package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.dom.HTMLNodeWrapper;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.trans.update.PendingUpdateList;
import client.net.sf.saxon.ce.trans.update.SetAttributeAction;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import com.google.gwt.dom.client.Element;

import java.util.Iterator;

/**
 * The compiled form of an ixsl:set-attribute instruction in the stylesheet.
 */

public class SetAttribute extends Instruction  {

    private Expression content;

    /**
     * Create a set-attribute instruction
     * @param content                an instruction to create the attribute
     */

    public SetAttribute(AttributeCreator content) {
        this.content = content;
        adoptChildExpression(content);
    }
    
    public int getInstructionNameCode() {
        return StandardNames.IXSL_SET_ATTRIBUTE;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @param visitor an expression visitor
     * @return the simplified expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        content = visitor.simplify(content);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.typeCheck(content, contextItemType);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.optimize(content, contextItemType);
        return this;
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.HAS_SIDE_EFFECTS;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws client.net.sf.saxon.ce.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        content = doPromotion(content, offer);
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @param th the type hierarchy cache
     * @return the static item type of the instruction. This is empty: the set-attribute instruction
     *         returns nothing.
     */

    public ItemType getItemType(TypeHierarchy th) {
        return EmptySequenceTest.getInstance();
    }


    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        return monoIterator(content);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (content == original) {
            content = replacement;
            found = true;
        }
        return found;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Item element = context.getContextItem();
        if (!(element instanceof HTMLNodeWrapper && ((HTMLNodeWrapper)element).getUnderlyingNode() instanceof Element)) {
            return null;
        }

        Element parent = (Element)((HTMLNodeWrapper)element).getUnderlyingNode();
        final PendingUpdateList pul = context.getController().getPendingUpdateList();
        SequenceIterator iter = content.iterate(context);
        while (true) {
            Item att = iter.next();
            if (att == null) {
                break;
            }
            if (att instanceof NodeInfo && ((NodeInfo)att).getNodeKind() == Type.ATTRIBUTE) {
                pul.add(new SetAttributeAction(parent, ((NodeInfo)att).getURI(), ((NodeInfo)att).getLocalPart(), att.getStringValue()));
            }
        }

        return null;
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
