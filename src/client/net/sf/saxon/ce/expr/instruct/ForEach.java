package client.net.sf.saxon.ce.expr.instruct;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.TraceListener;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;

import java.util.Arrays;
import java.util.Iterator;

import com.google.gwt.logging.client.LogConfiguration;


/**
* Handler for xsl:for-each elements in a stylesheet.
*/

public class ForEach extends Instruction implements ContextMappingFunction {

    protected Expression select;
    protected Expression action;
    protected boolean containsTailCall;

    /**
     * Base constructor to allow subclassing
     */

    public ForEach() {}

    /**
     * Create an xsl:for-each instruction
     * @param select the select expression
     * @param action the body of the xsl:for-each loop
     */

    public ForEach(Expression select, Expression action, boolean containsTailCall) {
        this.select = select;
        this.action = action;
        this.containsTailCall = containsTailCall;
        adoptChildExpression(select);
        adoptChildExpression(action);
    }


    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     * @return the code for name xsl:for-each
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_FOR_EACH;
    }

    /**
     * Get the select expression
     * @return the select expression. Note this will have been wrapped in a sort expression
     * if sorting was requested.
     */

    public Expression getSelectExpression() {
        return select;
    }

    /**
     * Get the action expression (the content of the for-each)
     * @return the body of the for-each loop
     */

    public Expression getActionExpression() {
        return action;
    }

    /**
     * Determine the data type of the items returned by this expression
     * @return the data type
     * @param th the type hierarchy cache
     */

    public final ItemType getItemType(TypeHierarchy th) {
        return action.getItemType(th);
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if the "action" creates new nodes.
     * (Nodes created by the condition can't contribute to the result).
     */

    public final boolean createsNewNodes() {
        int props = action.getSpecialProperties();
        return ((props & StaticProperty.NON_CREATIVE) == 0);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor the expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        action = visitor.simplify(action);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        select = visitor.typeCheck(select, contextItemType);
        adoptChildExpression(select);
        action = visitor.typeCheck(action, select.getItemType(th));
        adoptChildExpression(action);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        if (Literal.isEmptySequence(action)) {
            return action;
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        select = visitor.optimize(select, contextItemType);
        adoptChildExpression(select);
        action = action.optimize(visitor, select.getItemType(th));
        adoptChildExpression(action);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        if (Literal.isEmptySequence(action)) {
            return action;
        }

        // If any subexpressions within the body of the for-each are not dependent on the focus,
        // promote them: this causes them to be evaluated once, outside the for-each loop

        PromotionOffer offer = new PromotionOffer(visitor.getConfiguration().getOptimizer());
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (select.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.promoteXSLTFunctions = false;
        offer.containingExpression = this;
        offer.bindingList = new Binding[0];
        action = doPromotion(action, offer);

        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression =
                    visitor.optimize(offer.containingExpression, contextItemType);
        }
        Expression e2 = offer.containingExpression;
        if (e2 != this) {
            return e2;
        }

        return this;
    }


    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     *
     * @return the depencies, as a bit-mask
     */

    public int computeDependencies() {
        // Some of the dependencies aren't relevant. Note that the sort keys are absorbed into the select
        // expression.
        int dependencies = 0;
        dependencies |= select.getDependencies();
        dependencies |= (action.getDependencies() & ~StaticProperty.DEPENDS_ON_FOCUS);
        return dependencies;
    }


    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
        // Don't pass on other requests
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        return Arrays.asList((new Expression[]{select, action})).iterator();
    }

    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return child == action;
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (action == original) {
            action = replacement;
            found = true;
        }
        return found;
    }



    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD | PROCESS_METHOD;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        SequenceIterator iter = select.iterate(context);

        XPathContextMajor c2 = context.newContext();
        c2.setCurrentIterator(iter);
        c2.setCurrentTemplateRule(null);

        if (containsTailCall) {
            if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) { 
                TraceListener listener = LogController.getTraceListener();
                Item item = iter.next();
                if (item == null) {
                    return null;
                }
                listener.startCurrentItem(item);
                TailCall tc = ((TailCallReturner)action).processLeavingTail(c2);
                listener.endCurrentItem(item);
                return tc;
            	
            } else {
	            Item item = iter.next();
	            if (item == null) {
	                return null;
	            }
            }
            return ((TailCallReturner)action).processLeavingTail(c2);
        } else {
            if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) { 
                TraceListener listener = LogController.getTraceListener();
                while(true) {
                    Item item = iter.next();
                    if (item == null) {
                        break;
                    }
                    listener.startCurrentItem(item);
                    action.process(c2);
                    listener.endCurrentItem(item);
                }
            } else {
	            while(true) {
	                Item item = iter.next();
	                if (item == null) {
	                    break;
	                }
	                action.process(c2);
	            }
            }
        }
        return null;
    }

    /**
     * Return an Iterator to iterate over the values of the sequence. 
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator master = select.iterate(context);
        XPathContextMajor c2 = context.newContext();
        c2.setCurrentTemplateRule(null);
        c2.setCurrentIterator(master);
        master = new ContextMappingIterator(this, c2);
        return master;
    }

    /**
     * Map one item to a sequence.
     * @param context The processing context. This is supplied only for mapping constructs that
     * set the context node, position, and size. Otherwise it is null.
     * @return either (a) a SequenceIterator over the sequence of items that the supplied input
     * item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
     * sequence.
     */

    public SequenceIterator map(XPathContext context) throws XPathException {
        return action.iterate(context);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
