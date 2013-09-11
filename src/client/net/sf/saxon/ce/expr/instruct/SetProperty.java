package client.net.sf.saxon.ce.expr.instruct;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.core.client.JavaScriptObject;

import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.expr.*;

public class SetProperty extends Instruction {
    private Expression targetObject;
    private Expression name;
    private Expression select;

    /**
     * Create a set-property instruction - if object is null this defaults
     * to the window object
     */

    public SetProperty(Expression object, Expression select, Expression name) {
    	this.targetObject = object;
    	this.name = name;
    	this.select = select;
    	adoptChildren();
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
    	targetObject = visitor.simplify(targetObject);
    	name = visitor.simplify(name);
        select = visitor.simplify(select);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
    	targetObject = visitor.typeCheck(targetObject, contextItemType);
    	name = visitor.typeCheck(name, contextItemType);
        select = visitor.typeCheck(select, contextItemType);
        adoptChildren();
        return this;
    }
    
    private void adoptChildren() {
        adoptChildExpression(select);
        adoptChildExpression(targetObject);
        adoptChildExpression(name);
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
    	targetObject = visitor.optimize(targetObject, contextItemType);
    	name = visitor.optimize(name, contextItemType);
        select = visitor.optimize(select, contextItemType);
        adoptChildren();
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
    	targetObject = doPromotion(targetObject, offer);
    	name = doPromotion(name, offer);
        select = doPromotion(select, offer);
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction. This is empty: the set-attribute instruction
     *         returns nothing.
     */

    public ItemType getItemType() {
        return EmptySequenceTest.getInstance();
    }


    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList list = new ArrayList(3);
        list.add(select);
        list.add(targetObject);
        list.add(name);
        return list.iterator();
    }

    private static Object eval(Expression ex, XPathContext context) throws XPathException {
    	return IXSLFunction.convertToJavaScript(ExpressionTool.evaluate(ex, ExpressionTool.ITERATE_AND_MATERIALIZE, context));
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
    	
        Object content = eval(select, context);      
        JavaScriptObject clientObject = (JavaScriptObject)eval(targetObject, context);       		
        String member = (String)eval(name, context);
        try {
        IXSLFunction.setProperty(clientObject, member, content);
        } catch (Exception e){
        	throw new XPathException("Error setting client-property: " + member + " " + e.getMessage());
        }
        return null;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
