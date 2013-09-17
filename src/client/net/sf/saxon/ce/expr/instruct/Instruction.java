package client.net.sf.saxon.ce.expr.instruct;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.event.SequenceOutputter;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;


/**
* Abstract superclass for all instructions in the compiled stylesheet.
* This represents a compiled instruction, and as such, the minimum information is
* retained from the original stylesheet. <br>
* Note: this class implements SourceLocator: that is, it can identify where in the stylesheet
* the source instruction was located.
*/

public abstract class Instruction extends Expression implements TailCallReturner {

    /**
    * Constructor
    */

    public Instruction() {}

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction
     */

    public ItemType getItemType() {
        return Type.ITEM_TYPE;
    }

    /**
     * Get the cardinality of the sequence returned by evaluating this instruction
     * @return the static cardinality
     */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
    * ProcessLeavingTail: called to do the real work of this instruction. This method
    * must be implemented in each subclass. The results of the instruction are written
    * to the current Receiver, which can be obtained via the Controller.
    * @param context The dynamic context of the transformation, giving access to the current node,
    * the current variables, etc.
    * @return null if the instruction has completed execution; or a TailCall indicating
    * a function call or template call that is delegated to the caller, to be made after the stack has
    * been unwound so as to save stack space.
    */

    public abstract TailCall processLeavingTail(XPathContext context) throws XPathException;

    /**
    * Process the instruction, without returning any tail calls
    * @param context The dynamic context, giving access to the current node,
    * the current variables, etc.
    */

    public void process(XPathContext context) throws XPathException {
        try {
            TailCall tc = processLeavingTail(context);
            while (tc != null) {
                tc = tc.processLeavingTail();
            }
        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            throw err;
        }
    }

    /**
     * Construct an exception with diagnostic information. Note that this method
     * returns the exception, it does not throw it: that is up to the caller.
     * @param loc the location of the error
     * @param error The exception containing information about the error
     * @param context The controller of the transformation
     * @return an exception based on the supplied exception, but with location information
     * added relating to this instruction
     */

    protected static XPathException dynamicError(SourceLocator loc, XPathException error, XPathContext context) {
        if (error instanceof TerminationException) {
            return error;
        }
        error.maybeSetLocation(loc);
        return error;
    }

    /**
     * Assemble a ParameterSet. Method used by instructions that have xsl:with-param
     * children. This method is used for the non-tunnel parameters.
     * @param context the XPath dynamic context
     * @param actualParams the set of with-param parameters that specify tunnel="no"
     * @return a ParameterSet
     */

    public static ParameterSet assembleParams(XPathContext context,
                                                 WithParam[] actualParams)
    throws XPathException {
        if (actualParams == null || actualParams.length == 0) {
            return null;
        }
        ParameterSet params = new ParameterSet(actualParams.length);
        for (WithParam actualParam : actualParams) {
            params.put(actualParam.getParameterId(),
                    actualParam.getSelectValue(context),
                    actualParam.isTypeChecked());
        }
        return params;
    }

    /**
     * Assemble a ParameterSet. Method used by instructions that have xsl:with-param
     * children. This method is used for the tunnel parameters.
     * @param context the XPath dynamic context
     * @param actualParams the set of with-param parameters that specify tunnel="yes"
     * @return a ParameterSet
     */

    public static ParameterSet assembleTunnelParams(XPathContext context,
                                                       WithParam[] actualParams)
    throws XPathException {
        ParameterSet existingParams = context.getTunnelParameters();
        if (existingParams == null) {
            return assembleParams(context, actualParams);
        }
        ParameterSet newParams = new ParameterSet(existingParams, (actualParams==null ? 0 : actualParams.length));
        if (actualParams == null || actualParams.length == 0) {
            return newParams;
        }
        for (WithParam actualParam : actualParams) {
            newParams.put(actualParam.getParameterId(),
                    actualParam.getSelectValue(context),
                    false);
        }
        return newParams;
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

     public abstract Expression simplify(ExpressionVisitor visitor) throws XPathException;

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (createsNewNodes()) {
            return p;
        } else {
            return p | StaticProperty.NON_CREATIVE;
        }
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns a default value of false
     * @return true if the instruction creates new nodes (or if it can't be proved that it doesn't)
     */

    public boolean createsNewNodes() {
        return false;
    }


    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @param parent
     * @exception client.net.sf.saxon.ce.trans.XPathException if any error is detected
     * @return if the offer is not accepted, return this expression unchanged.
     *      Otherwise return the result of rewriting the expression to promote
     *      this subexpression
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp!=null) {
            return exp;
        } else {
            promoteInst(offer);
            return this;
        }
    }

     /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        int m = getImplementationMethod();
        if ((m & EVALUATE_METHOD) != 0) {
                throw new AssertionError(
                        "evaluateItem() is not implemented in the subclass " + getClass());
        } else if ((m & ITERATE_METHOD) != 0) {
            return iterate(context).next();
        } else {
            SequenceOutputter seq = pushToSequence(1, context);
            Item result = seq.getFirstItem();
            seq.reset();
            return result;
        }
    }

    private SequenceOutputter pushToSequence(int len, XPathContext context) throws XPathException {
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        SequenceOutputter seq = controller.allocateSequenceOutputter(len);
        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        seq.setPipelineConfiguration(pipe);
        c2.setTemporaryReceiver(seq);
        process(c2);
        seq.close();
        return seq;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        int m = getImplementationMethod();
        if ((m & EVALUATE_METHOD) != 0) {
            return super.iterate(context);
        } else if ((m & ITERATE_METHOD) != 0) {
            throw new AssertionError("iterate");
        } else {
            SequenceOutputter seq = pushToSequence(20, context);
            return seq.iterate();
        }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.