package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.SaxonceApi;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.IntegerValue;

import com.google.gwt.logging.client.LogConfiguration;
import com.google.gwt.user.client.Timer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.*;

/**
 * The compiled form of an ixsl:schedule-action instruction in the stylesheet.
 */

public class ScheduleExecution extends Instruction  {

    private CallTemplate call;
    private Expression wait;
    private static Logger logger = Logger.getLogger("ScheduleExecution");
    
    /**
     * Create a schedule-execution instruction
     * @param call the instruction to be executed
     * @param wait expression giving the amount of time to wait, in milliseconds
     */

    public ScheduleExecution(CallTemplate call, Expression wait) {
        this.call = call;
        this.wait = wait;
        adoptChildExpression(call);
        adoptChildExpression(wait);
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
        call = (CallTemplate)visitor.simplify(call);
        wait = visitor.simplify(wait);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        call = (CallTemplate)visitor.typeCheck(call, contextItemType);
        wait = visitor.typeCheck(wait, contextItemType);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        call = (CallTemplate)visitor.optimize(call, contextItemType);
        wait = visitor.optimize(wait, contextItemType);
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
        call = (CallTemplate)doPromotion(call, offer);
        wait = doPromotion(wait, offer);
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
        return Arrays.asList(call, wait).iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (wait == original) {
            wait = replacement;
            found = true;
        }
        return found;
    }
    

    public TailCall processLeavingTail(final XPathContext context) throws XPathException {
        IntegerValue time = (IntegerValue)wait.evaluateItem(context);
        final CallTemplate.CallTemplatePackage pack = (CallTemplate.CallTemplatePackage)call.processLeavingTail(context);
        Timer t = new Timer() {
            public void run() {
                //Window.setTitle("Timer fired " + serial++);
            	boolean success = false;
                logger.fine("processing ixsl:schedule-action");
            	if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
            		LogController.openTraceListener();
            	}
                try {
                    TailCall tc = pack.processLeavingTail();
                    while (tc != null) {
                        tc = tc.processLeavingTail();
                    }
                    context.getController().getPendingUpdateList().apply(context);
                    success = true;
                } catch (Exception err) {
                	logger.log(Level.SEVERE, "In delayed event: " + err.getMessage());
                	if (SaxonceApi.doThrowJsExceptions()) {
                		throw new RuntimeException(err.getMessage());
                	}
                }
            	if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
            		LogController.closeTraceListener(success);
            	}
            }
        };
        //Window.setTitle("Timer started " + serial + " (time " + time + "ms)");
        t.schedule(time.getIntValue());
        return null;
    }

    //private static int serial = 1;

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
