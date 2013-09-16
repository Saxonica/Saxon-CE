package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.trans.update.PendingUpdateList;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.StringValue;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.*;

/**
 * Interface supported by an XPath expression. This includes both compile-time
 * and run-time methods.
 *
 * <p>Two expressions are considered equal if they return the same result when evaluated in the
 * same context.</p>
 */

public abstract class Expression {

    public static final int EVALUATE_METHOD = 1;
    public static final int ITERATE_METHOD = 2;
    public static final int PROCESS_METHOD = 4;

    protected int staticProperties = -1;
    protected SourceLocator sourceLocator = null;
    private Container container;
    private ArrayList<String[]> traceProperties;

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     * {@link #PROCESS_METHOD}
     */

    public int getImplementationMethod() {
        if (Cardinality.allowsMany(getCardinality())) {
            return ITERATE_METHOD;
        } else {
            return EVALUATE_METHOD;
        }
    }

    /**
     * Determine whether this expression implements its own method for static type checking
     * @return true if this expression has a non-trivial implementation of the staticTypeCheck()
     * method
     */

    public boolean implementsStaticTypeCheck() {
        return false;
    }
    
    public ArrayList<String[]> getTraceProperties() {
    	return traceProperties;
    }
    
    public void AddTraceProperty(String name, Expression value) {
    	if (traceProperties == null) {
    		traceProperties = new ArrayList<String[]>();
    	}
    	String strValue;
    	if (value instanceof Literal) {
    		strValue = ((StringValue)((Literal)value).getValue()).getPrimitiveStringValue();
    	} else {
    		strValue = value.toString();
    	}
    	String[] entry = new String[] {name, strValue};
    	traceProperties.add(entry);
    }
    
    public void AddTraceProperty(String name, String value) {
    	if (traceProperties == null) {
    		traceProperties = new ArrayList<String[]>();
    	}

    	String[] entry = new String[] {name, value};
    	traceProperties.add(entry);
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
        return this;
    }

    /**
     * Perform type checking of an expression and its subexpressions. This is the second phase of
     * static optimization.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     *
     * <p>If the implementation returns a value other than "this", then it is required to ensure that
     * the location information in the returned expression have been set up correctly.
     * It should not rely on the caller to do this, although for historical reasons many callers do so.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @throws XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary run-time type checks,
     * and to perform other type-related optimizations
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Static type checking of some expressions is delegated to the expression itself, by calling
     * this method. The default implementation of the method throws UnsupportedOperationException.
     * If there is a non-default implementation, then implementsStaticTypeCheck() will return true
     *
     * @param req the required type
     * @param backwardsCompatible true if backwards compatibility mode applies
     * @param role the role of the expression in relation to the required type
     * @return the expression after type checking (perhaps augmented with dynamic type checking code)
     * @throws XPathException if failures occur, for example if the static type of one branch of the conditional
     * is incompatible with the required type
     */

    public Expression staticTypeCheck(SequenceType req,
                                      boolean backwardsCompatible,
                                      RoleLocator role)
    throws XPathException {
        throw new UnsupportedOperationException("staticTypeCheck");
    }

    /**
     * Perform optimisation of an expression and its subexpressions. This is the third and final
     * phase of static optimization.
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @throws XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten if appropriate to optimize execution
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * <p>This method must be overridden for any Expression that has subexpressions.</p>
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
        // The following temporary code checks that this method is implemented for all expressions
        // that have subexpressions. (Another way to do this is to see what errors arise when the
        // method is declared abstract).

//        if (iterateSubExpressions().hasNext()) {
//            throw new UnsupportedOperationException("promote is not implemented for " + getClass());
//        }
        return this;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public final int getSpecialProperties() {
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.SPECIAL_PROPERTY_MASK;
    }

    /**
     * Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.
     *
     * @return one of the values Cardinality.ONE_OR_MORE,
     *     Cardinality.ZERO_OR_MORE, Cardinality.EXACTLY_ONE,
     *     Cardinality.ZERO_OR_ONE, Cardinality.EMPTY. This default
     *     implementation returns ZERO_OR_MORE (which effectively gives no
     *     information).
     */

    public int getCardinality() {
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.CARDINALITY_MASK;
    }

    /**
	 * Determine the data type of the expression, if possible. All expression return
	 * sequences, in general; this method determines the type of the items within the
	 * sequence, assuming that (a) this is known in advance, and (b) it is the same for
	 * all items in the sequence.
     *
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
	 *
	 * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
	 *     Type.NODE, or Type.ITEM (meaning not known at compile time)
     */

    public abstract ItemType getItemType();

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *     the expression
     */

    public int getDependencies() {
        // Implemented as a memo function: we only compute the dependencies
        // for each expression once
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.DEPENDENCY_MASK;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator<Expression> iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Utility method to return an iterator over specific child expressions
     */

    protected Iterator<Expression> nonNullChildren(Expression... children) {
        List<Expression> list = new ArrayList<Expression>(children.length);
        for (Expression child : children) {
            if (child != null) {
                list.add(child);
            }
        }
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
        return false;
    }

    /**
     * Mark an expression as being "flattened". This is a collective term that includes extracting the
     * string value or typed value, or operations such as simple value construction that concatenate text
     * nodes before atomizing. The implication of all of these is that although the expression might
     * return nodes, the identity of the nodes has no significance. This is called during type checking
     * of the parent expression.
     * @param flattened set to true if the result of the expression is atomized or otherwise turned into
     * an atomic value
     */

    public void setFlattened(boolean flattened) {
        // no action in general
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception client.net.sf.saxon.ce.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return iterate(context).next();
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception client.net.sf.saxon.ce.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item value = evaluateItem(context);
        return SingletonIterator.makeIterator(value);
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception client.net.sf.saxon.ce.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return ExpressionTool.effectiveBooleanValue(iterate(context));
    }

    /**
     * Evaluate an expression as a String. This function must only be called in contexts
     * where it is known that the expression will return a single string (or where an empty sequence
     * is to be treated as a zero-length string). Implementations should not attempt to convert
     * the result to a string, other than converting () to "". This method is used mainly to
     * evaluate expressions produced by compiling an attribute value template.
     *
     * @exception client.net.sf.saxon.ce.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @exception ClassCastException if the result type of the
     *     expression is not xs:string?
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *     The expression must return a string or (); if the value of the
     *     expression is (), this method returns "".
     */

    public CharSequence evaluateAsString(XPathContext context) throws XPathException {
        Item o = evaluateItem(context);
        StringValue value = (StringValue) o;  // the ClassCastException is deliberate
        if (value == null) return "";
        return value.getStringValue();
    }

    /**
     * Process the instruction, without returning any tail calls
     * @param context The dynamic context, giving access to the current node,
     * the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        int m = getImplementationMethod();

        if ((m & EVALUATE_METHOD) != 0) {
            Item item = evaluateItem(context);
            if (item != null) {
                context.getReceiver().append(item, NodeInfo.ALL_NAMESPACES);
            }

        } else if ((m & ITERATE_METHOD) != 0) {

            SequenceIterator iter = iterate(context);
            SequenceReceiver out = context.getReceiver();
            try {
                while (true) {
                    Item it = iter.next();
                    if (it == null) {
                        break;
                    }
                    out.append(it, NodeInfo.ALL_NAMESPACES);
                }
            } catch (XPathException e) {
                e.maybeSetLocation(this.getSourceLocator());
                throw e;
            }

        } else {
            throw new AssertionError("process() is not implemented in the subclass " + getClass());
        }
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     * @return a representation of the expression as a string
     */

    public String toString() {
        // fallback implementation
        FastStringBuffer buff = new FastStringBuffer(FastStringBuffer.SMALL);
        String className = getClass().getName();
        while (true) {
            int dot = className.indexOf('.');
            if (dot >= 0) {
                className = className.substring(dot+1);
            } else {
                break;
            }
        }
        buff.append(className);
        Iterator iter = iterateSubExpressions();
        boolean first = true;
        while (iter.hasNext()) {
            buff.append(first ? "(" : ", ");
            buff.append(iter.next().toString());
            first = false;
        }
        if (!first) {
            buff.append(")");
        }
        return buff.toString();
    }

    /**
     * Mark an expression as being in a given Container. This link is used primarily for diagnostics:
     * the container links to the location map held in the executable.
     *
     * <p>This affects the expression and all its subexpressions. Any subexpressions that are not in the
     * same container are marked with the new container, and this proceeds recursively. However, any
     * subexpression that is already in the correct container is not modified.</p>
     *
     * @param container The container of this expression.
     */

    public void setContainer(Container container) {
        this.container = container;
        if (container != null) {
            Iterator children = iterateSubExpressions();
            while (children.hasNext()) {
                Expression child = (Expression)children.next();
                // child can be null while expressions are under construction
                if (child != null && child.getContainer() != container &&
                        (child.container == null || child.container.getContainerGranularity() < container.getContainerGranularity())) {
                    child.setContainer(container);
                }
            }
        }
    }

    /**
     * Get the container in which this expression is located. This will usually be a top-level construct
     * such as a function or global variable, and XSLT template, or an XQueryExpression. In the case of
     * free-standing XPath expressions it will be the StaticContext object
     * @return the expression's container
     */

    public Container getContainer() {
        return container;
    }

    /**
     * Set up a parent-child relationship between this expression and a given child expression.
     * <p>
     * Note: many calls on this method are now redundant, but are kept in place for "belt-and-braces"
     * reasons. The rule is that an implementation of simplify(), typeCheck(), or optimize() that returns
     * a value other than "this" is required to set the location information and parent pointer in the new
     * child expression. However, in the past this was often left to the caller, which did it by calling
     * this method, either unconditionally on return from one of these methods, or after testing that the
     * returned object was not the same as the original.
     * @param child the child expression
     */

    public void adoptChildExpression(Expression child) {
        if (child == null) {
            return;
        }

        if (container == null) {
            container = child.container;
        } else {
            child.setContainer(container);
        }

        if (sourceLocator == null) {
            ExpressionTool.copyLocationInfo(child, this);
        } else if (child.sourceLocator == null) {
            ExpressionTool.copyLocationInfo(this, child);
        }
        resetLocalStaticProperties();
    }

    /**
     * Set the location ID on an expression. In Saxon CE this is the node in the stylesheet tree
     * that contains the expression.
     * @param location the location id
     */

    public void setSourceLocator(SourceLocator location) {
        sourceLocator = location;
        for (Iterator<Expression> iter = iterateSubExpressions(); iter.hasNext(); ) {
            Expression child = iter.next();
            if (child != null && child.getSourceLocator() == null) {
                child.setSourceLocator(location);
            }
        }
    }

    /**
     * Get the location ID of the expression. In Saxon CE this is the node in the stylesheet tree
     * that contains the expression.
     * @return a location identifier, which can be turned into real
     * location information by reference to a location provider
     */

    public final SourceLocator getSourceLocator() {
        if (sourceLocator == null) {
            Container container = getContainer();
            if (container != null) {
                return container.getSourceLocator();
            } else {
                return null;
            }
        } else {
            return sourceLocator;
        }
    }

    /**
     * Get the systemId of the module containing the expression
     */

    public String getSystemId() {
        return (sourceLocator == null)? null : sourceLocator.getSystemId();
    }

    /**
     * Get the executable containing this expression
     * @return the containing Executable
     */

    public Executable getExecutable() {
        return getContainer().getExecutable();
    }

    /**
     * Promote a subexpression if possible, and if the expression was changed, carry out housekeeping
     * to reset the static properties and correct the parent pointers in the tree
     * @param subexpression the subexpression that is a candidate for promotion
     * @param offer details of the promotion being considered @return the result of the promotion. This will be the current expression if no promotion
     * actions have taken place
     */

    public final Expression doPromotion(Expression subexpression, PromotionOffer offer)
            throws XPathException {
        if (subexpression == null) {
            return null;
        }
        Expression e = subexpression.promote(offer, this);
        if (e != subexpression) {
            adoptChildExpression(e);
        } else if (offer.accepted) {
            resetLocalStaticProperties();
        }
        return e;
    }

    /**
     * Compute the static properties. This should only be done once for each
     * expression.
     */

    public final void computeStaticProperties() {
        staticProperties =
                computeDependencies() |
                computeCardinality() |
                computeSpecialProperties();
    }

    /**
     * Reset the static properties of the expression to -1, so that they have to be recomputed
     * next time they are used.
     */

    protected void resetLocalStaticProperties() {
        staticProperties = -1;
    }

    /**
     * Compute the static cardinality of this expression
     * @return the computed cardinality, as one of the values {@link StaticProperty#ALLOWS_ZERO_OR_ONE},
     * {@link StaticProperty#EXACTLY_ONE}, {@link StaticProperty#ALLOWS_ONE_OR_MORE},
     * {@link StaticProperty#ALLOWS_ZERO_OR_MORE}
     */

    protected abstract int computeCardinality();

    /**
     * Compute the special properties of this expression. These properties are denoted by a bit-significant
     * integer, possible values are in class {@link StaticProperty}. The "special" properties are properties
     * other than cardinality and dependencies, and most of them relate to properties of node sequences, for
     * example whether the nodes are in document order.
     * @return the special properties, as a bit-significant integer
     */

    protected int computeSpecialProperties() {
        return 0;
    }

    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     * @return the depencies, as a bit-mask
     */

    public int computeDependencies() {
        int dependencies = getIntrinsicDependencies();
        for (Iterator children = iterateSubExpressions(); children.hasNext();) {
            Expression child = (Expression)children.next();
            dependencies |= child.getDependencies();
        }
        return dependencies;
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *     dependencies. The flags are documented in class client.net.sf.saxon.ce.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        return 0;
    }

    /**
     * Mark tail-recursive calls on stylesheet functions. For most expressions, this does nothing.
     * @param qName the name of the function
     * @param arity the arity (number of parameters) of the function
     *
     * @return 0 if no tail call was found; 1 if a tail call on a different function was found;
     * 2 if a tail recursive call was found and if this call accounts for the whole of the value.
     */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        return 0;
    }

    /**
     * Method used in subclasses to signal a dynamic error
     * @param message the error message
     * @param code the error code
     */

    protected void dynamicError(String message, String code) throws XPathException {
        if (LogConfiguration.loggingIsEnabled()){
        	throw new XPathException(message, code, getSourceLocator());
        } else {
        	throw new XPathException("", code, getSourceLocator());
        }
    }

    /**
     * Method used in subclasses to signal a runtime type error
     * @param message the error message
     * @param errorCode the error code
     */

    protected void typeError(String message, String errorCode) throws XPathException {
        // can't compile out error messages here for production variant
        typeError(null, message, errorCode);
    }
    
    protected void typeError(ExpressionVisitor visitor, String message, String errorCode) throws XPathException {
        XPathException e;
        // can't compile out error messages here for production variant       	
        if (visitor != null){
    		String path="at " + visitor.getLocation() + ": ";
        	e = new XPathException(path + message, errorCode, getSourceLocator());
        } else {
        	e = new XPathException(message, errorCode, getSourceLocator());
        }
        e.setIsTypeError(true);
        throw e;
    }

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
	}

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.