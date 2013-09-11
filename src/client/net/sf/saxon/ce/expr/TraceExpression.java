package client.net.sf.saxon.ce.expr;


import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.instruct.Instruction;
import client.net.sf.saxon.ce.expr.instruct.TailCall;
import client.net.sf.saxon.ce.lib.TraceListener;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamespaceResolver;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trace.InstructionInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.trans.update.PendingUpdateList;
import client.net.sf.saxon.ce.type.ItemType;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.HashMap;
import java.util.Iterator;

/**
 * A wrapper expression used to trace expressions in XPath and XQuery.
 */

public class TraceExpression extends Instruction implements InstructionInfo {

    private StructuredQName objectName;
    
    private StructuredQName constructType;
    /*@Nullable*/ private NamespaceResolver namespaceResolver = null;
    private HashMap<String, Object> properties = new HashMap<String, Object>(10);
    Expression child;   // the instruction or other expression to be traced

    /**
     * Create a trace expression that traces execution of a given child expression
     * @param child the expression to be traced. This will be available to the TraceListener
     * as the value of the "expression" property of the InstructionInfo.
     */
    public TraceExpression(Expression child) {
        this.child = child;
        adoptChildExpression(child);
        setProperty("expression", child);
    }

    /**
     * Set the type of construct. This will generally be a constant
     * in class {@link client.net.sf.saxon.ce.trace.Location}
     * @param type an integer code for the type of construct being traced
     */

    public void setConstructType(StructuredQName type) {
        constructType = type;
    }

    /**
     * Get the construct type. This will generally be a constant
     * in class {@link client.net.sf.saxon.ce.trace.Location}
     */
    public StructuredQName getConstructType() {
        return constructType;
    }

    /**
     * Set the namespace context for the instruction being traced. This is needed if the
     * tracelistener wants to evaluate XPath expressions in the context of the current instruction
     * @param resolver The namespace resolver, or null if none is needed
     */

    public void setNamespaceResolver(/*@Nullable*/ NamespaceResolver resolver) {
        namespaceResolver = resolver;
    }

    /**
     * Get the namespace resolver to supply the namespace context of the instruction
     * that is being traced
     * @return the namespace resolver, or null if none is in use
     */

    /*@Nullable*/
    public NamespaceResolver getNamespaceResolver() {
        return namespaceResolver;
    }

    /**
     * Set a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * @param qName the name of the object, or null if not applicable
     */

    public void setObjectName(StructuredQName qName) {
        objectName = qName;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * @return the name of the object, or null if not applicable
     */

    public client.net.sf.saxon.ce.om.StructuredQName getObjectName() {
        return objectName;
    }

    /**
     * Set a named property of the instruction/expression
     * @param name the name of the property
     * @param value the value of the property
     */

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Get a named property of the instruction/expression
     * @param name the name of the property
     * @return the value of the property
     */

    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property.
     */

    public Iterator<String> getProperties() {
        return properties.keySet().iterator();
    }


    /**
     * Get the InstructionInfo details about the construct. This is to satisfy the InstructionInfoProvider
     * interface.
     * @return the instruction details
     */

    public InstructionInfo getInstructionInfo() {
        return this;
    }


    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @throws XPathException  if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        child = visitor.simplify(child);
        if (child instanceof TraceExpression) {
            return child;
        }
        return this;
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        child = visitor.typeCheck(child, contextItemType);
        adoptChildExpression(child);
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        child = visitor.optimize(child, contextItemType);
        adoptChildExpression(child);
        return this;
    }

    public int getImplementationMethod() {
        return child.getImplementationMethod();
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent the parent of the subexpression
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        // Many rewrites are not attempted if tracing is activated. But those that are, for example
        // rewriting of calls to current(), must be carried out.
        Expression newChild = child.promote(offer, parent);
        if (newChild != child) {
            child = newChild;
            adoptChildExpression(child);
            return this;
        }
        return this;
    }

    /**
     * Execute this instruction, with the possibility of returning tail calls if there are any.
     * This outputs the trace information via the registered TraceListener,
     * and invokes the instruction being traced.
     * @param context the dynamic execution context
     * @return either null, or a tail call that the caller must invoke on return
     * @throws XPathException
     */
    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        assert controller != null;
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	TraceListener listener = LogController.getTraceListener();
	    	listener.enter(getInstructionInfo(), context);
	    	child.process(context);
	    	listener.leave(getInstructionInfo());
	    	
	    } else {
	    	child.process(context);
	    }
        return null;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return child.getItemType();
    }

    /**
     * Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.
     *
     * @return one of the values Cardinality.ONE_OR_MORE,
     *         Cardinality.ZERO_OR_MORE, Cardinality.EXACTLY_ONE,
     *         Cardinality.ZERO_OR_ONE, Cardinality.EMPTY. This default
     *         implementation returns ZERO_OR_MORE (which effectively gives no
     *         information).
     */

    public int getCardinality() {
        return child.getCardinality();
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as {@link StaticProperty#DEPENDS_ON_CONTEXT_ITEM} and
     * {@link StaticProperty#DEPENDS_ON_CURRENT_ITEM}. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *     the expression
     */

    public int getDependencies() {
        return child.getDependencies();
    }

    /**
     * Determine whether this instruction creates new nodes.
     *
     *
     */

    public final boolean createsNewNodes() {
        return (child.getSpecialProperties() & StaticProperty.NON_CREATIVE) == 0;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeDependencies() {
        return child.computeDependencies();
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @throws XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
    	Item result;
    	if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
    		LogController.getTraceListener().enter(getInstructionInfo(), context);
    		result = child.evaluateItem(context);
    		LogController.getTraceListener().leave(getInstructionInfo());
    	} else {
    		result = child.evaluateItem(context);
    	}
        return result;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @throws XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
    	SequenceIterator result;
    	if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
    		LogController.getTraceListener().enter(getInstructionInfo(), context);
    		result = child.iterate(context);
    		LogController.getTraceListener().leave(getInstructionInfo());
    		
    	} else {
    		result = child.iterate(context);
    	}
        return result;
    }

    /*@NotNull*/
    public Iterator<Expression> iterateSubExpressions() {
        return new MonoIterator(child);
    }

    public Expression getChildExpression() {
        return child;
    }

    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException
     *
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
    	if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
    		LogController.getTraceListener().enter(getInstructionInfo(), context);
    		child.evaluatePendingUpdates(context, pul);
    		LogController.getTraceListener().leave(getInstructionInfo());
    	} else {
    		child.evaluatePendingUpdates(context, pul);
    	}
    }

	public int getLineNumber() {
		return 0;
	}
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.