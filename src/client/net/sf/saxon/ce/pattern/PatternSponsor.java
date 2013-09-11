package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.BooleanValue;

import java.util.Iterator;

/**
 * The PatternSponsor class allows a Pattern to be treated like an expression. Although
 * patterns are not evaluated at run-time in the same way as expressions, they need to
 * be manipulated in much the same way as expressions at compile time: for example variables
 * need to be bound, dependencies need to be analyzed, and so on. This is especially true
 * of patterns appearing in the xsl:number and xsl:for-each-group instructions (less so for
 * the more common match patterns in xsl:template).
 *
 * <p>This class implements the Expression interface, so that an Expression can have a
 * PatternSponsor as a subexpression; it wraps a Pattern.</p>
 *
 */

public class PatternSponsor extends Expression {

    private Pattern pattern;

    /**
     * Create a sponsor expression for a pattern
     * @param pattern the pattern being wrapped
     */

    public PatternSponsor(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Get the wrapped pattern
     * @return the wrapped pattern
     */

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     */

    public int getImplementationMethod() {
        return EVALUATE_METHOD;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @return the simplified expression
     * @throws XPathException if an error is discovered during expression
     *                                        rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        pattern = pattern.simplify(visitor);
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Perform type checking of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        pattern = pattern.analyze(visitor, contextItemType);
        return this;
    }

    protected int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    private Expression copy() {
        throw new UnsupportedOperationException("PatternSponsor.copy()");
    }

    /**
     * Treat all subexpressions of a pattern as "looping" subexpresions, that is, assume they are
     * evaluated repeatedly
     * @param child the child expression
     * @return true, always
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return true;
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        pattern.promote(offer, this);
        return this;
    }

    /**
     * <p>Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.</p>
     * <p/>
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @return one of the values {@link StaticProperty#ALLOWS_ONE},
     *     {@link StaticProperty#ALLOWS_ZERO_OR_MORE}, {@link StaticProperty#ALLOWS_ZERO_OR_ONE},
     *     {@link StaticProperty#ALLOWS_ONE_OR_MORE}, {@link StaticProperty#EMPTY}.
     */

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the data type of the expression, if possible. All expression return
     * sequences, in general; this method determines the type of the items within the
     * sequence, assuming that (a) this is known in advance, and (b) it is the same for
     * all items in the sequence.
     * <p/>
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
     *         Type.NODE, or Type.ITEM (meaning not known at compile time)
     */

    public ItemType getItemType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as {@link client.net.sf.saxon.ce.expr.StaticProperty#DEPENDS_ON_CONTEXT_ITEM} and
     * {@link client.net.sf.saxon.ce.expr.StaticProperty#DEPENDS_ON_CURRENT_ITEM}. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *         the expression
     */

    public int getDependencies() {
        return pattern.getDependencies();
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator<Expression> iterateSubExpressions() {
        return pattern.iterateSubExpressions();
    }


    /**
     * Get the container that immediately contains this expression. This method
     * returns null for an outermost expression; it also return null in the case
     * of literal values. For an XPath expression occurring within an XSLT stylesheet,
     * this method returns the XSLT instruction containing the XPath expression.
     *
     * @return the expression that contains this expression, if known; return null
     *         if there is no containing expression or if the containing expression is unknown.
     */

    public Container getContainer() {
        return pattern;
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the node or atomic value that results from evaluating the
     *         expression; or null to indicate that the result is an empty
     *         sequence
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return SingletonIterator.makeIterator(evaluateItem(context));
    }

    /**
     * Get the effective boolean value of the expression. Returns true if the underlying
     * pattern matches the context node, otherwise false.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        Item contextItem = context.getContextItem();
        return contextItem instanceof NodeInfo
                && pattern.matches((NodeInfo)contextItem, context);
    }

    /**
     * Evaluate an expression as a String. This function must only be called in contexts
     * where it is known that the expression will return a single string (or where an empty sequence
     * is to be treated as a zero-length string). Implementations should not attempt to convert
     * the result to a string, other than converting () to "". This method is used mainly to
     * evaluate expressions produced by compiling an attribute value template.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *         The expression must return a string or (); if the value of the
     *         expression is (), this method returns "".
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *                            if any dynamic error occurs evaluating the
     *                            expression
     * @throws ClassCastException if the result type of the
     *                            expression is not xs:string?
     */

    public CharSequence evaluateAsString(XPathContext context) throws XPathException {
        return evaluateItem(context).getStringValue();
    }

    /**
     * Process the instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        throw new UnsupportedOperationException("Patterns cannot be evaluated in push mode");
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
