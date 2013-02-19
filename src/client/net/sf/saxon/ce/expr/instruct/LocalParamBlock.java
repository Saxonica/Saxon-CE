package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;

import java.util.Arrays;
import java.util.Iterator;


/**
* Represents the set of xsl:param elements at the start of an xsl:iterate instruction
*/

public class LocalParamBlock extends Instruction {

    private LocalParam[] children;

    /**
     * Create the block of parameters
     */

    public LocalParamBlock(LocalParam[] params) {
        this.children = params;
        for (int c=0; c<children.length; c++) {
            adoptChildExpression(children[c]);
        }
    }

    public String getExpressionName() {
        return "block";
    }

    /**
    * Get the children of this instruction
    * @return the children of this instruction, as an array of Instruction objects. May return
     * a zero-length array if there are no children
    */

    public LocalParam[] getChildren() {
        return children;
    }


    public int computeSpecialProperties() {
        return 0;
    }

    public Iterator<Expression> iterateSubExpressions() {
        return Arrays.asList((Expression[])children).iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (replacement instanceof LocalParam) {
            for (int c=0; c<children.length; c++) {
                if (children[c] == original) {
                    children[c] = (LocalParam)replacement;
                    found = true;
                }
            }
        }
        return found;
    }


    /**
     * Determine the data type of the items returned by this expression
     * @return the data type
     * @param th the type hierarchy cache
     */

    public final ItemType getItemType(TypeHierarchy th) {
        return EmptySequenceTest.getInstance();
    }

    /**
     * Determine the cardinality of the expression
     */

    public final int getCardinality() {
        return StaticProperty.EMPTY;
    }

     /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception client.net.sf.saxon.ce.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        for (int c=0; c<children.length; c++) {
            children[c] = (LocalParam)visitor.simplify(children[c]);
            adoptChildExpression(children[c]);
        }
        return this;
    }

     public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int c=0; c<children.length; c++) {
            children[c] = (LocalParam)visitor.typeCheck(children[c], contextItemType);
            adoptChildExpression(children[c]);
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int c=0; c<children.length; c++) {
            children[c] = (LocalParam)visitor.optimize(children[c], contextItemType);
            adoptChildExpression(children[c]);
        }
        return this;
    }



    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws client.net.sf.saxon.ce.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        for (int c=0; c<children.length; c++) {
            // TODO: prevent certain promotions, such as making the LocalParam global.
            children[c] = (LocalParam)doPromotion(children[c], offer);
        }
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        for (int i=0; i<children.length; i++) {
            try {
                LocalParam param = children[i];
                context.setLocalVariable(param.getSlotNumber(), param.getSelectValue(context));
            } catch (XPathException e) {
                e.maybeSetLocation(children[i].getSourceLocator());
                e.maybeSetContext(context);
                throw e;
            }
        }
    	return null;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return PROCESS_METHOD;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.