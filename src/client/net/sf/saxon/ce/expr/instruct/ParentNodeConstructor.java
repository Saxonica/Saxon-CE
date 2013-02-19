package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;

import java.util.Iterator;

/**
 * An abstract class to act as a common parent for instructions that create element nodes
 * and document nodes.
 */

public abstract class ParentNodeConstructor extends Instruction {

    protected Expression content;
    private String baseURI;

    /**
     * Create a document or element node constructor instruction
     */

    public ParentNodeConstructor() {}

    /**
     * Set the static base URI of the instruction
     * @param uri the static base URI
     */

    public void setBaseURI(String uri) {
        baseURI = uri;
    }

    /**
     * Get the static base URI of the instruction
     * @return  the static base URI
     */

    public String getBaseURI() {
        return baseURI;
    }

    /**
     * Set the expression that constructs the content of the element
     * @param content the content expression
     */

    public void setContentExpression(Expression content) {
        this.content = content;
        adoptChildExpression(content);
    }

    /**
     * Get the cardinality of the sequence returned by evaluating this instruction
     * @return the static cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }    


    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during expression rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        content = visitor.simplify(content);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.typeCheck(content, contextItemType);
        adoptChildExpression(content);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.optimize(content, contextItemType);
        if (content instanceof Block) {
            content = ((Block)content).mergeAdjacentTextInstructions();
        }
        adoptChildExpression(content);
        return this;
    }


    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws client.net.sf.saxon.ce.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (offer.action != PromotionOffer.UNORDERED) {
            content = doPromotion(content, offer);
        }
    }

    /**
      * Get the immediate sub-expressions of this expression.
      * @return an iterator containing the sub-expressions of this expression
      */

    public Iterator<Expression> iterateSubExpressions() {
        return monoIterator(content);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
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



    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }



}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

