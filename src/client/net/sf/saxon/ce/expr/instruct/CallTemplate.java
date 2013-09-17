package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
* Instruction representing an xsl:call-template element in the stylesheet.
*/

public class CallTemplate extends Instruction {

    private Template template = null;
    private WithParam[] actualParams = null;
    private WithParam[] tunnelParams = null;
    private boolean useTailRecursion = false;

    /**
    * Construct a CallTemplate instruction.
    * @param template the Template object identifying the template to be called, in the normal
    * case where this is known statically
     * @param useTailRecursion true if the call is potentially tail recursive
     */

    public CallTemplate(Template template,
                        boolean useTailRecursion) {

        this.template = template;
        this.useTailRecursion = useTailRecursion;
    }

    public void setUseTailRecursion(boolean useIt) {
        useTailRecursion = useIt;
    }

   /**
    * Set the actual parameters on the call
    * @param actualParams the parameters that are not tunnel parameters
    * @param tunnelParams the tunnel parameters
    */

    public void setActualParameters(
                        WithParam[] actualParams,
                        WithParam[] tunnelParams ) {
        this.actualParams = actualParams;
        this.tunnelParams = tunnelParams;
        for (WithParam actualParam : actualParams) {
            adoptChildExpression(actualParam);
        }
        for (WithParam tunnelParam : tunnelParams) {
            adoptChildExpression(tunnelParam);
        }
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception XPathException if an error is discovered during expression
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
        if (template.getBody() != null) {
            // For non-tunnel parameters, see if the supplied value is type-safe against the declared
            // type of the value, and if so, avoid the dynamic type check
            // Can't do this check unless the target template has been compiled.
            boolean backwards = visitor.getStaticContext().isInBackwardsCompatibleMode();
            for (int p=0; p<actualParams.length; p++) {
                WithParam wp = actualParams[p];
                int id = wp.getParameterId();
                LocalParam lp = template.getLocalParam(id);
                if (lp != null) {
                    SequenceType req = lp.getRequiredType();
                    RoleLocator role = new RoleLocator(RoleLocator.PARAM, wp.getVariableQName().getDisplayName(), p);
                    Expression select = TypeChecker.staticTypeCheck(
                            wp.getSelectExpression(), req, backwards, role);
                    wp.setSelectExpression(select);
                    wp.setTypeChecked(true);
                }
            }

        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        WithParam.optimize(visitor, actualParams, contextItemType);
        WithParam.optimize(visitor, tunnelParams, contextItemType);
        return this;
    }


    /**
     * Get the cardinality of the sequence returned by evaluating this instruction
     *
     * @return the static cardinality
     */

    public int computeCardinality() {
        if (template == null) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return template.getRequiredType().getCardinality();
        }
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     */

    public ItemType getItemType() {
        if (template == null) {
            return AnyItemType.getInstance();
        } else {
            return template.getRequiredType().getPrimaryType();
        }
    }

    public int getIntrinsicDependencies() {
        // we could go to the called template and find which parts of the context it depends on, but this
        // would create the risk of infinite recursion. So we just assume that the dependencies exist
        return StaticProperty.DEPENDS_ON_XSLT_CONTEXT |
                StaticProperty.DEPENDS_ON_FOCUS;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation currently returns true unconditionally.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList<Expression> list = new ArrayList<Expression>(10);
        WithParam.getXPathExpressions(actualParams, list);
        WithParam.getXPathExpressions(tunnelParams, list);
        return list.iterator();
    }


    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws client.net.sf.saxon.ce.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        WithParam.promoteParams(this, actualParams, offer);
        WithParam.promoteParams(this, tunnelParams, offer);
    }

    /**
     * Process this instruction, without leaving any tail calls.
     * @param context the dynamic context for this transformation
     * @throws XPathException if a dynamic error occurs
     */

    public void process(XPathContext context) throws XPathException {

        Template t = getTargetTemplate();
        XPathContext c2 = context.newContext();
        c2.openStackFrame(t.getNumberOfSlots());
        c2.setLocalParameters(assembleParams(context, actualParams));
        c2.setTunnelParameters(assembleTunnelParams(context, tunnelParams));

        TailCall tc = t.expand(c2);
        while (tc != null) {
            tc = tc.processLeavingTail();
        }
    }

    /**
    * Process this instruction. If the called template contains a tail call (which may be
    * an xsl:call-template of xsl:apply-templates instruction) then the tail call will not
    * actually be evaluated, but will be returned in a TailCall object for the caller to execute.
    * @param context the dynamic context for this transformation
    * @return an object containing information about the tail call to be executed by the
    * caller. Returns null if there is no tail call.
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException
    {
        if (!useTailRecursion) {
            process(context);
            return null;
        }

        // if name is determined dynamically, determine it now

        Template target = getTargetTemplate();

        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        // Call the named template. Actually, don't call it; rather construct a call package
        // and return it to the caller, who will then process this package.

        //System.err.println("Call template using tail recursion");
        if (params==null) {                  // bug 490967
            params = ParameterSet.EMPTY_PARAMETER_SET;
        }

        // clear all the local variables: they are no longer needed
        Arrays.fill(context.getStackFrame(), null);
        // TODO: this is fine for tail calls, but not so good for ixsl:schedule-action.

        return new CallTemplatePackage(target, params, tunnels, context);
    }

    /**
     * Get the template, in the case where it is specified dynamically.
     * @return                  The template to be called
     * @throws XPathException if a dynamic error occurs: specifically, if the
     * template name is computed at run-time (Saxon extension) and the name is invalid
     * or does not reference a known template
     */

    public Template getTargetTemplate() throws XPathException {
        return template;
    }


    public StructuredQName getObjectName() {
        return (template==null ? null : template.getTemplateName());
    }


    /**
    * A CallTemplatePackage is an object that encapsulates the name of a template to be called,
    * the parameters to be supplied, and the execution context. This object can be returned as a tail
    * call, so that the actual call is made from a lower point on the stack, allowing a tail-recursive
    * template to execute in a finite stack size
    */

    public static class CallTemplatePackage implements TailCall {

        private Template target;
        private ParameterSet params;
        private ParameterSet tunnelParams;
        private XPathContext evaluationContext;

        /**
         * Construct a CallTemplatePackage that contains information about a call.
         * @param template the Template to be called
         * @param params the parameters to be supplied to the called template
         * @param tunnelParams the tunnel parameter supplied to the called template
         * @param evaluationContext saved context information from the Controller (current mode, etc)
         * which must be reset to ensure that the template is called with all the context information
         * intact
         */

        public CallTemplatePackage(Template template,
                                   ParameterSet params,
                                   ParameterSet tunnelParams,
                                   XPathContext evaluationContext) {
            target = template;
            this.params = params;
            this.tunnelParams = tunnelParams;
            this.evaluationContext = evaluationContext;
        }

        public void setEvaluationContext(XPathContext evaluationContext) {
            this.evaluationContext = evaluationContext;
        }

        /**
        * Process the template call encapsulated by this package.
        * @return another TailCall. This will never be the original call, but it may be the next
        * recursive call. For example, if A calls B which calls C which calls D, then B may return
        * a TailCall to A representing the call from B to C; when this is processed, the result may be
        * a TailCall representing the call from C to D.
         * @throws XPathException if a dynamic error occurs
        */

        public TailCall processLeavingTail() throws XPathException {
            // TODO: the idea of tail call optimization is to reuse the caller's stack frame rather than
            // creating a new one. We're doing this for the Java stack, but not for the context stack where
            // local variables are held. It should be possible to avoid creating a new context, and instead
            // to update the existing one in situ.
            XPathContext c2 = evaluationContext.newContext();
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnelParams);
            c2.openStackFrame(target.getNumberOfSlots());

            // System.err.println("Tail call on template");

            return target.expand(c2);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
