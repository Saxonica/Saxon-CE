package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.functions.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;

import java.math.BigDecimal;
import java.util.Iterator;

/**
 * A FilterExpression contains a base expression and a filter predicate, which may be an
 * integer expression (positional filter), or a boolean expression (qualifier)
 */

public final class FilterExpression extends Expression {

    private Expression start;
    private Expression filter;
    private boolean filterIsPositional;         // true if the value of the filter might depend on
    // the context position
    private boolean filterIsIndependentNumeric; // true if the filter expression returns a number that doesn't
    // depend on the context item or position

    /**
     * Constructor
     *
     * @param start  A node-set expression denoting the absolute or relative set of nodes from which the
     *               navigation path should start.
     * @param filter An expression defining the filter predicate
     */

    public FilterExpression(Expression start, Expression filter) {
        this.start = start;
        this.filter = filter;
        adoptChildExpression(start);
        adoptChildExpression(filter);
    }

    /**
     * Get the data type of the items returned
     *
     * @return an integer representing the data type
     */

    public ItemType getItemType() {
        // special case the filter [. instance of x]
        if (filter instanceof InstanceOfExpression &&
                ((InstanceOfExpression)filter).getBaseExpression() instanceof ContextItemExpression) {
            return ((InstanceOfExpression)filter).getRequiredItemType();
        }
        return start.getItemType();
    }

    /**
     * Get the underlying expression
     *
     * @return the expression being filtered
     */

    public Expression getControllingExpression() {
        return start;
    }

    /**
     * Get the subexpression that is evaluated in the new context
     * @return the subexpression evaluated in the context set by the controlling expression
     */

    public Expression getControlledExpression() {
        return filter;
    }

    /**
     * Get the filter expression
     *
     * @return the expression acting as the filter predicate
     */

    public Expression getFilter() {
        return filter;
    }


    /**
     * Determine if the filter is positional
     *
     * @param th the Type Hierarchy (for cached access to type information)
     * @return true if the value of the filter depends on the position of the item against
     *         which it is evaluated
     */

    public boolean isPositional(TypeHierarchy th) {
        return isPositionalFilter(filter, th);
    }

     /**
     * Simplify an expression
     *
     * @param visitor the expression visitor
     * @return the simplified expression
     * @throws XPathException if any failure occurs
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {

        start = visitor.simplify(start);
        filter = visitor.simplify(filter);
        return this;

    }

    /**
     * Type-check the expression
     *
     * @param visitor         the expression visitor
     * @param contextItemType the type of the context item for this expression
     * @return the expression after type-checking (potentially modified to add run-time
     *         checks and/or conversions)
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = TypeHierarchy.getInstance();
        start = visitor.typeCheck(start, contextItemType);
        adoptChildExpression(start);

        Expression filter2 = visitor.typeCheck(filter, start.getItemType());
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        if (Literal.isConstantOne(filter)) {
            return new FirstItemExpression(start);
        }

        if (filter instanceof Last) {
            return new LastItemExpression(start);
        }

        // determine whether the filter evaluates to a single number, where the number will be the same for
        // all values in the sequence
        filterIsIndependentNumeric =
                th.isSubType(filter.getItemType(), AtomicType.NUMERIC) &&
                        (filter.getDependencies() &
                                (StaticProperty.DEPENDS_ON_CONTEXT_ITEM | StaticProperty.DEPENDS_ON_POSITION)) == 0 &&
                        !Cardinality.allowsMany(filter.getCardinality());
        visitor.resetStaticProperties();
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final Configuration config = visitor.getConfiguration();
        final TypeHierarchy th = TypeHierarchy.getInstance();

        Expression start2 = visitor.optimize(start, contextItemType);
        if (start2 != start) {
            start = start2;
            adoptChildExpression(start2);
        }

        Expression filter2 = filter.optimize(visitor, start.getItemType());
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // the filter expression may have been reduced to a constant boolean by previous optimizations
        if (filter instanceof Literal && ((Literal)filter).getValue() instanceof BooleanValue) {
            if (((BooleanValue)((Literal)filter).getValue()).getBooleanValue()) {
                return start;
            } else {
                return new Literal(EmptySequence.getInstance());
            }
        }

        // determine whether the filter might depend on position
        filterIsPositional = isPositionalFilter(filter, th);

        Expression subsequence = tryToRewritePositionalFilter(visitor);
        if (subsequence != null) {
            ExpressionTool.copyLocationInfo(this, subsequence);
            return subsequence.simplify(visitor)
                    .typeCheck(visitor, contextItemType)
                    .optimize(visitor, contextItemType);
        }

        // If any subexpressions within the filter are not dependent on the focus,
        // promote them: this causes them to be evaluated once, outside the filter
        // expression. Note: we do this even if the filter is numeric, because it ensures that
        // the subscript is pre-evaluated, allowing direct indexing into the sequence.

        PromotionOffer offer = new PromotionOffer();
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (start.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.containingExpression = this;
        filter2 = doPromotion(filter, offer);
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }
        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression = visitor.optimize(offer.containingExpression, contextItemType);
        }
        return offer.containingExpression;
    }

    /**
     * Attempt to rewrite a filter expression whose predicate is a test of the form
     * [position() op expr] as a call on functions such as subsequence, remove
     * @param visitor the current expression visitor
     * @return the rewritten expression if a rewrite was possible, or null otherwise
     */

    private Expression tryToRewritePositionalFilter(ExpressionVisitor visitor) throws XPathException {
        if (filter instanceof Literal) {
            Sequence val = ((Literal)filter).getValue();
            if (val instanceof NumericValue) {
                if (((NumericValue)val).isWholeNumber()) {
                    try {
                        int lvalue = ((NumericValue)val).intValue();
                        if (lvalue <= 0) {
                            return Literal.makeEmptySequence();
                        } else if (lvalue == 1) {
                            return new FirstItemExpression(start);
                        } else {
                            return SystemFunction.makeSystemFunction("subsequence", new Expression[]{start, filter, new Literal(IntegerValue.PLUS_ONE)});
                        }
                    } catch (XPathException err) {
                        // integer out of range
                        return null;
                    }
                } else {
                    return Literal.makeEmptySequence();
                }
            } else {
                return (ExpressionTool.effectiveBooleanValue(val.iterate()) ? start : Literal.makeEmptySequence());
            }
        }
        if (filter instanceof ComparisonExpression) {
            TypeHierarchy th = TypeHierarchy.getInstance();
            Expression[] operands = ((ComparisonExpression)filter).getOperands();
            int operator = ((ComparisonExpression)filter).getSingletonOperator();
            Expression comparand;
            if (operands[0] instanceof Position
                    && th.isSubType(operands[1].getItemType(), AtomicType.NUMERIC)) {
                comparand = operands[1];
            } else if (operands[1] instanceof Position
                    && th.isSubType(operands[0].getItemType(), AtomicType.NUMERIC)) {
                comparand = operands[0];
                operator = Token.inverse(operator);
            } else {
                return null;
            }

            if (ExpressionTool.dependsOnFocus(comparand)) {
                return null;
            }

            int card = comparand.getCardinality();
            if (Cardinality.allowsMany(card)) {
                return null;
            }

            // If the comparand might be an empty sequence, do the base rewrite and then wrap the
            // rewritten expression EXP in "let $n := comparand if exists($n) then EXP else ()
            if (Cardinality.allowsZero(card)) {
                LetExpression let = new LetExpression();
                let.setRequiredType(SequenceType.makeSequenceType(comparand.getItemType(), card));
                let.setVariableQName(new StructuredQName("pp", NamespaceConstant.SAXON, "pp" + let.hashCode()));
                let.setSequence(comparand);
                comparand = new LocalVariableReference(let);
                LocalVariableReference existsArg = new LocalVariableReference(let);
                Exists exists = (Exists)SystemFunction.makeSystemFunction("exists", new Expression[]{existsArg});
                Expression rewrite = tryToRewritePositionalFilterSupport(start, comparand, operator, th);
                if (rewrite == null) {
                    return null;
                }
                Expression choice = Choose.makeConditional(exists, rewrite);
                let.setAction(choice);
                return let;
            } else {
                return tryToRewritePositionalFilterSupport(start, comparand, operator, th);
            }
        } else if (filter instanceof IntegerRangeTest) {
            // rewrite SEQ[position() = N to M]
            // => let $n := N return subsequence(SEQ, $n, (M - ($n - 1))
            // (precise form is optimized for the case where $n is a literal, especially N = 1)
            Expression val = ((IntegerRangeTest)filter).getValueExpression();
            if (!(val instanceof Position)) {
                return null;
            }
            Expression min = ((IntegerRangeTest)filter).getMinValueExpression();
            Expression max = ((IntegerRangeTest)filter).getMaxValueExpression();

            if (ExpressionTool.dependsOnFocus(min)) {
                return null;
            }
            if (ExpressionTool.dependsOnFocus(max)) {
                if (max instanceof Last) {
                    return SystemFunction.makeSystemFunction("subsequence", new Expression[]{start, min});
                } else {
                    return null;
                }
            }

            LetExpression let = new LetExpression();
            let.setRequiredType(SequenceType.SINGLE_INTEGER);
            let.setVariableQName(new StructuredQName("nn", NamespaceConstant.SAXON, "nn" + let.hashCode()));
            let.setSequence(min);
            min = new LocalVariableReference(let);
            LocalVariableReference min2 = new LocalVariableReference(let);

            Expression minMinusOne = new ArithmeticExpression(
                    min2, Token.MINUS, new Literal(new IntegerValue(1)));
            Expression length = new ArithmeticExpression(max, Token.MINUS, minMinusOne);
            Subsequence subs = (Subsequence)SystemFunction.makeSystemFunction(
                    "subsequence", new Expression[]{start, min, length});
            let.setAction(subs);
            return let;

        } else {
            return null;
        }
    }

    private static Expression tryToRewritePositionalFilterSupport(
            Expression start, Expression comparand, int operator,
            TypeHierarchy th)
            throws XPathException {
        if (th.isSubType(comparand.getItemType(), AtomicType.INTEGER)) {
            switch (operator) {
            case Token.FEQ: {
                if (Literal.isConstantOne(comparand)) {
                    return new FirstItemExpression(start);
                } else {
                    return SystemFunction.makeSystemFunction("subsequence", new Expression[]{start, comparand, new Literal(IntegerValue.PLUS_ONE)});
                }
            }
            case Token.FLT: {

                Expression[] args = new Expression[3];
                args[0] = start;

                args[1] = new Literal(new IntegerValue(1));
                if (Literal.isAtomic(comparand)) {
                    long n = ((NumericValue)((Literal)comparand).getValue()).intValue();
                    args[2] = new Literal(new IntegerValue(new BigDecimal(n - 1)));
                } else {

                    args[2] = new ArithmeticExpression(
                            comparand, Token.MINUS, new Literal(new IntegerValue(1)));
                }
                return SystemFunction.makeSystemFunction("subsequence", args);
            }
            case Token.FLE: {
                Expression[] args = new Expression[3];
                args[0] = start;

                args[1] = new Literal(new IntegerValue(1));
                args[2] = comparand;
                return SystemFunction.makeSystemFunction("subsequence", args);
            }
            case Token.FNE: {
                return SystemFunction.makeSystemFunction("remove", new Expression[]{start, comparand});
            }
            case Token.FGT: {
                Expression[] args = new Expression[2];
                args[0] = start;
                if (Literal.isAtomic(comparand)) {
                    long n = ((NumericValue)((Literal)comparand).getValue()).intValue();
                    args[1] = new Literal(new IntegerValue(new BigDecimal(n + 1)));
                } else {

                    args[1] = new ArithmeticExpression(
                            comparand, Token.PLUS, new Literal(new IntegerValue(1)));
                }
                return SystemFunction.makeSystemFunction("subsequence", args);
            }
            case Token.FGE: {
                return SystemFunction.makeSystemFunction("subsequence", new Expression[]{start, comparand});
            }
            default:
                throw new IllegalArgumentException("operator");
            }

        } else {
            return null;
        }
    }

    /**
     * Promote this expression if possible
     *
     * @param offer details of the promotion that is possible
     * @param parent
     * @return the promoted expression (or the original expression, unchanged)
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            if (!(offer.action == PromotionOffer.UNORDERED && filterIsPositional)) {
                start = doPromotion(start, offer);
            }
            if (offer.action == PromotionOffer.REPLACE_CURRENT) {
                filter = doPromotion(filter, offer);
            } else {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
            }
            return this;
        }
    }

    /**
     * Determine whether an expression, when used as a filter, is potentially positional;
     * that is, where it either contains a call on position() or last(), or where it is capable of returning
     * a numeric result.
     *
     * @param exp the expression to be examined
     * @param th  the type hierarchy cache
     * @return true if the expression depends on position() or last() explicitly or implicitly
     */

    private static boolean isPositionalFilter(Expression exp, TypeHierarchy th) {
        ItemType type = exp.getItemType();
        if (type.equals(AtomicType.BOOLEAN)) {
            // common case, get it out of the way quickly
            return isExplicitlyPositional(exp);
        }
        return (type.equals(AtomicType.ANY_ATOMIC) ||
                type instanceof AnyItemType ||
                type.equals(AtomicType.INTEGER) ||
                type.equals(AtomicType.NUMERIC) ||
                th.isSubType(type, AtomicType.NUMERIC) ||
                isExplicitlyPositional(exp));
    }

    /**
     * Determine whether an expression, when used as a filter, has an explicit dependency on position() or last()
     *
     * @param exp the expression being tested
     * @return true if the expression is explicitly positional, that is, if it contains an explicit call on
     *         position() or last()
     */

    private static boolean isExplicitlyPositional(Expression exp) {
        return (exp.getDependencies() & (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST)) != 0;
    }

    /**
     * Get the immediate subexpressions of this expression
     *
     * @return the subexpressions, as an array
     */

    public Iterator<Expression> iterateSubExpressions() {
        return nonNullChildren(start, filter);
    }

    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     *
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return child == filter;
    }


    /**
     * Get the static cardinality of this expression
     *
     * @return the cardinality. The method attempts to determine the case where the
     *         filter predicate is guaranteed to select at most one item from the sequence being filtered
     */

    public int computeCardinality() {
        if (filter instanceof Literal && ((Literal)filter).getValue() instanceof NumericValue) {
            if (((NumericValue)((Literal)filter).getValue()).compareTo(1) == 0 &&
                    !Cardinality.allowsZero(start.getCardinality())) {
                return StaticProperty.ALLOWS_ONE;
            } else {
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            }
        }
        if (filterIsIndependentNumeric) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }

        if (!Cardinality.allowsMany(start.getCardinality())) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }

        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-significant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return the static properties of the expression, as a bit-significant value
     */

    public int computeSpecialProperties() {
        return start.getSpecialProperties();
    }

    /**
     * Is this expression the same as another expression?
     *
     * @param other the expression to be compared with this one
     * @return true if the two expressions are statically equivalent
     */

    public boolean equals(Object other) {
        if (other instanceof FilterExpression) {
            FilterExpression f = (FilterExpression)other;
            return (start.equals(f.start) &&
                    filter.equals(f.filter));
        }
        return false;
    }

    /**
     * get HashCode for comparing two expressions
     *
     * @return the hash code
     */

    public int hashCode() {
        return "FilterExpression".hashCode() + start.hashCode() + filter.hashCode();
    }

    /**
     * Iterate over the results, returning them in the correct order
     *
     * @param context the dynamic context for the evaluation
     * @return an iterator over the expression results
     * @throws XPathException if any dynamic error occurs
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // Fast path where both operands are constants, or simple variable references

        Expression startExp = start;
        Sequence startValue = null;
        if (startExp instanceof Literal) {
            startValue = ((Literal)startExp).getValue();
        } else if (startExp instanceof VariableReference) {
            startValue = ((VariableReference)startExp).evaluateVariable(context);
            startExp = new Literal(startValue);
        }

        if (startValue instanceof EmptySequence) {
            return EmptyIterator.getInstance();
        }

        Sequence filterValue = null;
        if (filter instanceof Literal) {
            filterValue = ((Literal)filter).getValue();
        } else if (filter instanceof VariableReference) {
            filterValue = ((VariableReference)filter).evaluateVariable(context);
        }

        // Handle the case where the filter is a value. Because of earlier static rewriting, this covers
        // all cases where the filter expression is independent of the context, that is, where the
        // value of the filter expression is the same for all items in the sequence being filtered.

        if (filterValue instanceof SequenceTool) {
            if (filterValue instanceof NumericValue) {
                // Filter is a constant number
                if (((NumericValue)filterValue).isWholeNumber()) {
                    int pos = ((NumericValue)filterValue).intValue();
                    if (startValue != null) {
                        // if sequence is a value, use direct indexing - unless its a Closure!
                        return SingletonIterator.makeIterator(startValue.itemAt(pos - 1));
                    }
                    if (pos >= 1) {
                        SequenceIterator base = startExp.iterate(context);
                        return Subsequence.makeIterator(base, pos, pos);
                    } else {
                        // index is less than one, no items will be selected
                        return EmptyIterator.getInstance();
                    }
                } else {
                    // a non-integer value will never be equal to position()
                    return EmptyIterator.getInstance();
                }
            }
        }

        // get an iterator over the base nodes

        SequenceIterator base = startExp.iterate(context);
        return new FilterIterator(base, filter, context);

    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE
     *
     * @return the dependencies
     */

    public int computeDependencies() {
        // not all dependencies in the filter expression matter, because the context node,
        // position, and size are not dependent on the outer context.
        return (start.getDependencies() |
                (filter.getDependencies() & (StaticProperty.DEPENDS_ON_XSLT_CONTEXT |
                        StaticProperty.DEPENDS_ON_LOCAL_VARIABLES |
                        StaticProperty.DEPENDS_ON_USER_FUNCTIONS)));
    }


    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return "(" + start.toString() + "[" + filter.toString() + "])";
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
