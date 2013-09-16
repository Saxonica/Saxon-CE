package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.Rule;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.Value;

import java.util.ArrayList;
import java.util.Iterator;

/**
* An instruction representing an xsl:apply-templates element in the stylesheet
*/

public class ApplyTemplates extends Instruction {

    protected Expression select;
    protected WithParam[] actualParams = null;
    protected WithParam[] tunnelParams = null;
    protected boolean useCurrentMode = false;
    protected boolean useTailRecursion = false;
    protected Mode mode;
    protected boolean implicitSelect;

    protected ApplyTemplates() {}

    /**
     * Construct an apply-templates instructino
     * @param select the select expression
     * @param useCurrentMode true if mode="#current" was specified
     * @param useTailRecursion true if this instruction is the last in its template
     * @param mode the mode specified on apply-templates
     */

    public ApplyTemplates(  Expression select,
                            boolean useCurrentMode,
                            boolean useTailRecursion,
                            boolean implicitSelect,
                            Mode mode) {
        init(select, useCurrentMode, useTailRecursion, mode);
        this.implicitSelect = implicitSelect;
    }

    protected void init(  Expression select,
                            boolean useCurrentMode,
                            boolean useTailRecursion,
                            Mode mode) {
        this.select = select;
        this.useCurrentMode = useCurrentMode;
        this.useTailRecursion = useTailRecursion;
        this.mode = mode;
        adoptChildExpression(select);
    }

   /**
    * Set the actual parameters on the call
    * @param actualParams represents the contained xsl:with-param elements having tunnel="no" (the default)
    * @param tunnelParams represents the contained xsl:with-param elements having tunnel="yes"
    */

    public void setActualParameters(
                        WithParam[] actualParams,
                        WithParam[] tunnelParams ) {
        this.actualParams = actualParams;
        this.tunnelParams = tunnelParams;
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
        WithParam.simplify(actualParams, visitor);
        WithParam.simplify(tunnelParams, visitor);
        select = visitor.simplify(select);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        WithParam.typeCheck(actualParams, visitor, contextItemType);
        WithParam.typeCheck(tunnelParams, visitor, contextItemType);
        try {
            select = visitor.typeCheck(select, contextItemType);
        } catch (XPathException e) {
            if (implicitSelect) {
                String code = e.getErrorCodeLocalPart();
                if ("XPTY0020".equals(code)) {
                    XPathException err = new XPathException("Cannot apply-templates to child nodes when the context item is an atomic value");
                    err.setErrorCode("XTTE0510");
                    err.setIsTypeError(true);
                    throw err;
                } else if ("XPDY0002".equals(code)) {
                    XPathException err = new XPathException("Cannot apply-templates to child nodes when the context item is undefined");
                    err.setErrorCode("XTTE0510");
                    err.setIsTypeError(true);
                    throw err;
                }
            }
            throw e;
        }
        adoptChildExpression(select);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        WithParam.optimize(visitor, actualParams, contextItemType);
        WithParam.optimize(visitor, tunnelParams, contextItemType);
        select = visitor.typeCheck(select, contextItemType);  // More info available second time around
        select = visitor.optimize(select, contextItemType);
        adoptChildExpression(select);
        if (Literal.isEmptySequence(select)) {
            return select;
        }
        return this;
    }

    public int getIntrinsicDependencies() {
        // If the instruction uses mode="#current", this represents a dependency on the context
        // which means the instruction cannot be loop-lifted or moved to a global variable.
        // We overload the dependency DEPENDS_ON_CURRENT_ITEM to achieve this effect.
        return super.getIntrinsicDependencies() | (useCurrentMode ? StaticProperty.DEPENDS_ON_CURRENT_ITEM : 0);
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true (which is almost invariably the case, so it's not worth
     * doing any further analysis to find out more precisely).
     */

    public final boolean createsNewNodes() {
        return true;
    }

    public void process(XPathContext context) throws XPathException {
        apply(context, false);
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        return apply(context, useTailRecursion);
    }

    protected TailCall apply(XPathContext context, boolean returnTailCall) throws XPathException {
        Mode thisMode = mode;
        if (useCurrentMode) {
            thisMode = context.getCurrentMode();
        }

        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        if (returnTailCall) {
            XPathContextMajor c2 = context.newContext();
            final int evaluationMode = ExpressionTool.lazyEvaluationMode(select);
            return new ApplyTemplatesPackage(
                    ExpressionTool.evaluate(select, evaluationMode, context),
                    thisMode, params, tunnels, c2, getSourceLocator());
        }

        // Get an iterator to iterate through the selected nodes in original order

        SequenceIterator iter = select.iterate(context);

        // Quick exit if the iterator is empty

        if (iter instanceof EmptyIterator) {
            return null;
        }

        // process the selected nodes now
        XPathContextMajor c2 = context.newContext();
//        try {
            TailCall tc = applyTemplates(iter, thisMode, params, tunnels, c2, getSourceLocator());
            while (tc != null) {
                tc = tc.processLeavingTail();
            }
//        } catch (StackOverflowError e) {
//            XPathException err = new XPathException("Too many nested apply-templates calls. The stylesheet may be looping.");
//            err.setErrorCode(SaxonErrorCode.SXLM0001);
//            err.setLocator(this);
//            err.setXPathContext(context);
//            throw err;
//        }
        return null;

    }

    /**
     * Process selected nodes using the handlers registered for a particular
     * mode.
     *
     * @exception XPathException if any dynamic error occurs
     * @param iterator an Iterator over the nodes to be processed, in the
     *     correct (sorted) order
     * @param mode Identifies the processing mode. It should match the
     *     mode defined when the element handler was registered using
     *     setHandler with a mode parameter. Set this parameter to null to
     *     invoke the default mode.
     * @param parameters A ParameterSet containing the parameters to
     *     the handler/template being invoked. Specify null if there are no
     *     parameters.
     * @param tunnelParameters A ParameterSet containing the parameters to
     *     the handler/template being invoked. Specify null if there are no
     *     parameters.
     * @param context A newly-created context object (this must be freshly created by the caller,
     * as it will be modified by this method)
     * @param sourceLocator location of this apply-templates instruction in the stylesheet
     * @return a TailCall returned by the last template to be invoked, or null,
     *     indicating that there are no outstanding tail calls.
     */

    public static TailCall applyTemplates(SequenceIterator iterator,
                                          Mode mode,
                                          ParameterSet parameters,
                                          ParameterSet tunnelParameters,
                                          XPathContextMajor context,
                                          SourceLocator sourceLocator)
                                throws XPathException {
        TailCall tc = null;


        context.setCurrentIterator(iterator);
        context.setCurrentMode(mode);
        Template previousTemplate = null;
        while(true) {

            // process any tail calls returned from previous nodes. We need to do this before changing
            // the context. We need to execute the outstanding tail calls before moving the iterator

            if (tc != null) {
                do {
                    tc = tc.processLeavingTail();
                } while (tc != null);
            }

            NodeInfo node = (NodeInfo)iterator.next();
                    // We can assume it's a node - we did static type checking
            if (node == null) {
                break;
            }

            // find the template rule for this node

            Rule rule = mode.getRule(node, context);
            if (rule==null) {
            	rule = mode.getVirtualRule(context);
            }

            if (rule==null) {
            	// Use the default action for the node
                // No need to open a new stack frame!
                mode.getBuiltInRuleSet().process(node, parameters, tunnelParameters, context, sourceLocator);
            } else {
                Template template = (Template)rule.getAction();
                if (template != previousTemplate) {
                    // Reuse the previous stackframe unless it's a different template rule
                    previousTemplate = template;
                    context.openStackFrame(template.getNumberOfSlots());
                    context.setLocalParameters(parameters);
                    context.setTunnelParameters(tunnelParameters);
                }
                context.setCurrentTemplateRule(rule);
                if (rule.isVirtual()){
                	SequenceIterator iter = IXSLFunction.convertFromJavaScript(context.getController().getUserData("Saxon-CE", "current-object"), 
                			context.getConfiguration());
                	iter.next(); // position on the item;
                    context.setCurrentIterator(iter);
                }
                tc = template.applyLeavingTail(context);
            }
        }

        // return the TailCall returned from the last node processed
        return tc;
    }

    /**
     * Perform the built-in template action for a given node.
     *
     * @param node the node to be processed
     * @param parameters the parameters supplied to apply-templates
     * @param tunnelParams the tunnel parameters to be passed through
     * @param context the dynamic evaluation context
     * @param sourceLocator location of the instruction (apply-templates, apply-imports etc) that caused
     * the built-in template to be invoked
     * @exception XPathException if any dynamic error occurs
     */

    public static void defaultAction(NodeInfo node,
                                     ParameterSet parameters,
                                     ParameterSet tunnelParams,
                                     XPathContext context,
                                     SourceLocator sourceLocator) throws XPathException {
        switch(node.getNodeKind()) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                SequenceIterator iter = node.iterateAxis(Axis.CHILD, AnyNodeTest.getInstance());
                XPathContextMajor c2 = context.newContext();
	            TailCall tc = applyTemplates(
                        iter, context.getCurrentMode(), parameters, tunnelParams, c2, sourceLocator);
                while (tc != null) {
                    tc = tc.processLeavingTail();
                }
	            return;
	        case Type.TEXT:
	            // NOTE: I tried changing this to use the text node's copy() method, but
	            // performance was worse
	        case Type.ATTRIBUTE:
	            context.getReceiver().characters(node.getStringValue());
	            return;
	        case Type.COMMENT:
	        case Type.PROCESSING_INSTRUCTION:
	        case Type.NAMESPACE:
	            // no action
        }
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList<Expression> list = new ArrayList<Expression>(10);
        list.add(select);
        WithParam.getXPathExpressions(actualParams, list);
        WithParam.getXPathExpressions(tunnelParams, list);
        return list.iterator();
    }

    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return child instanceof WithParam;
    }


    /**
     * Get the select expression
     * @return the select expression
     */

    public Expression getSelectExpression() {
        return select;
    }

    /**
     * Ask if the select expression was implicit
     * @return true if no select attribute was explicitly specified
     */

    public boolean isImplicitSelect() {
        return implicitSelect;
    }

    /**
     * Ask if tail recursion is to be used
     * @return true if tail recursion is used
     */

    public boolean useTailRecursion() {
        return useTailRecursion;
    }

    /**
     * Ask if mode="#current" was specified
     * @return true if mode="#current" was specified
     */

    public boolean usesCurrentMode() {
        return useCurrentMode;
    }

    /**
     * Get the Mode
     * @return the mode, or null if mode="#current" was specified
     */

    public Mode getMode() {
        return mode;
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
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
        WithParam.promoteParams(this, actualParams, offer);
        WithParam.promoteParams(this, tunnelParams, offer);
    }



    /**
    * An ApplyTemplatesPackage is an object that encapsulates the sequence of nodes to be processed,
    * the mode, the parameters to be supplied, and the execution context. This object can be returned as a tail
    * call, so that the actual call is made from a lower point on the stack, allowing a tail-recursive
    * template to execute in a finite stack size
    */

    private static class ApplyTemplatesPackage implements TailCall {

        private Sequence selectedNodes;
        private Mode mode;
        private ParameterSet params;
        private ParameterSet tunnelParams;
        private XPathContextMajor evaluationContext;
        private SourceLocator sourceLocator;

        ApplyTemplatesPackage(Sequence selectedNodes,
                                     Mode mode,
                                     ParameterSet params,
                                     ParameterSet tunnelParams,
                                     XPathContextMajor context,
                                     SourceLocator sourceLocator
                                     ) {
            this.selectedNodes = selectedNodes;
            this.mode = mode;
            this.params = params;
            this.tunnelParams = tunnelParams;
            evaluationContext = context;
            this.sourceLocator = sourceLocator;
        }

        public TailCall processLeavingTail() throws XPathException {
            return applyTemplates(
                    Value.asIterator(selectedNodes),
                    mode, params, tunnelParams, evaluationContext, sourceLocator);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
