package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.expr.instruct.UserFunction;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceExtent;
import client.net.sf.saxon.ce.value.SequenceTool;

/**
* A TailCallLoop wraps the body of a function that contains tail-recursive function calls. On completion
 * of the "real" body of the function it tests whether the function has executed a tail call, and if so,
 * iterates to evaluate the tail call.
*/

public final class TailCallLoop extends UnaryExpression {

    UserFunction containingFunction;

    /**
     * Constructor - create a TailCallLoop
     * @param function the function in which this tail call loop appears
     */

    public TailCallLoop(UserFunction function) {
        super(function.getBody());
        containingFunction = function;
    }

    /**
     * Get the containing function
     * @return the containing function
     */

    public UserFunction getContainingFunction() {
        return containingFunction;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return operand.getImplementationMethod();
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    private Expression copy() {
        throw new UnsupportedOperationException("TailCallLoop.copy()");
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        final XPathContextMajor cm = (XPathContextMajor)context;
        while (true) {
            SequenceIterator iter = operand.iterate(cm);
            Sequence extent = SequenceExtent.makeSequenceExtent(iter);
            UserFunction fn = cm.getTailCallFunction();
            if (fn == null) {
                return extent.iterate();
            }
            if (fn != containingFunction) {
                return tailCallDifferentFunction(fn, cm).iterate();
            }
            // otherwise, loop round to execute the tail call
        }
    }

    /**
    * Evaluate as an Item.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        final XPathContextMajor cm = (XPathContextMajor)context;
        while (true) {
            Item item = operand.evaluateItem(context);
            UserFunction fn = cm.getTailCallFunction();
            if (fn == null) {
               return item;
            }
            if (fn != containingFunction) {
                return SequenceTool.asItem(tailCallDifferentFunction(fn, cm));
            }
            // otherwise, loop round to execute the tail call
        }
    }

    /**
     * Process the function body
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        final XPathContextMajor cm = (XPathContextMajor)context;
        while (true) {
            operand.process(context);
            UserFunction fn = cm.getTailCallFunction();
            if (fn == null) {
                return;
            }
            if (fn != containingFunction) {
                SequenceTool.process(tailCallDifferentFunction(fn, cm).iterate(), cm);
                return;
            }
            // otherwise, loop round to execute the tail call
        }
    }

    /**
     * Make a tail call on a different function. This reuses the context object and the stack frame array
     * where possible, but it does consume some Java stack space. It's still worth it, because we don't use
     * as much stack as we would if we didn't return down to the TailCallLoop level.
     * @param fn the function to be called
     * @param cm the dynamic context
     * @return the result of calling the other function
     * @throws XPathException if the called function fails
     */

    private Sequence tailCallDifferentFunction(UserFunction fn, XPathContextMajor cm) throws XPathException {
        cm.resetStackFrameMap(fn.getNumberOfSlots(), fn.getNumberOfArguments());
        try {
            return ExpressionTool.evaluate(fn.getBody(), fn.getEvaluationMode(), cm);
        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            throw err;
        }
    }


    /**
     * Determine the data type of the items returned by the expression
     */

	public ItemType getItemType() {
	    return operand.getItemType();
	}


}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.