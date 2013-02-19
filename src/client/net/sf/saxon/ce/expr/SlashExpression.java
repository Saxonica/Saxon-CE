package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.ForEach;
import client.net.sf.saxon.ce.expr.sort.DocumentOrderIterator;
import client.net.sf.saxon.ce.expr.sort.GlobalOrderComparer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.OneItemGoneIterator;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceType;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A slash expression is any expression using the binary slash operator "/". The parser initially generates a slash
 * expression for all occurrences of the binary "/" operator. Subsequently, as a result of type inferencing, the
 * majority of slash expressions will be rewritten as instances of PathExpression (returning nodes) or
 * ForEach instructions (when they return atomic values). However, in the rare case where it is not possible to determine
 * statically whether the rh operand returns nodes or atomic values, instances of this class may need to be interpreted
 * directly at run time.
 */

public class SlashExpression extends Expression
        implements ContextMappingFunction {

    Expression start;
    Expression step;

    /**
     * Constructor
     * @param start The left hand operand (which must always select a sequence of nodes).
     * @param step The step to be followed from each node in the start expression to yield a new
     * sequence; this may return either nodes or atomic values (but not a mixture of the two)
     */

    public SlashExpression(Expression start, Expression step) {
        this.start = start;
        this.step = step;
        adoptChildExpression(start);
        adoptChildExpression(step);
    }

    protected void setStartExpression(Expression start2) {
        if (start != start2) {
            start = start2;
            adoptChildExpression(start);
        }
    }

    protected void setStepExpression(Expression step2) {
        if (step != step2) {
            step = step2;
            adoptChildExpression(step);
        }
    }

    /**
     * Get the start expression (the left-hand operand)
     * @return the first operand
     */

    public Expression getControllingExpression() {
        return start;
    }

    /**
     * Get the step expression (the right-hand operand)
     * @return the second operand
     */

    public Expression getControlledExpression() {
        return step;
    }

    /**
     * Determine whether this expression is capable (as far as static analysis is concerned)
     * of returning a mixture of nodes and atomic values. If so, this needs to be prevented
     * at run time
     * @return true if the static type allows both nodes and atomic values
     */

    public boolean isHybrid() {
        return true;
    }

    /**
     * Simplify an expression
     * @return the simplified expression
     * @param visitor the expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        setStartExpression(visitor.simplify(start));
        setStepExpression(visitor.simplify(step));

        // if the start expression is an empty sequence, then the whole PathExpression is empty
        if (Literal.isEmptySequence(start)) {
            return start;
        }

        // if the simplified Step is an empty sequence, then the whole PathExpression is empty
        if (Literal.isEmptySequence(step)) {
            return step;
        }

        // the expression /.. is sometimes used to represent the empty node-set. Applying this simplification
        // now avoids generating warnings for this case.
        if (start instanceof RootExpression && step instanceof ParentNodeExpression) {
            return Literal.makeEmptySequence();
        }

        return this;
    }


    /**
     * Determine the data type of the items returned by this exprssion
     * @return the type of the step
     * @param th the type hierarchy cache
     */

    public final ItemType getItemType(TypeHierarchy th) {
        return step.getItemType(th);
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        Expression start2 = visitor.typeCheck(start, contextItemType);

        // The first operand must be of type node()*

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, "/", 0);
        role0.setErrorCode("XPTY0019");
        setStartExpression(
                TypeChecker.staticTypeCheck(start2, SequenceType.NODE_SEQUENCE, false, role0, visitor));

        // Now check the second operand

        setStepExpression(visitor.typeCheck(step, start.getItemType(th)));

        // We distinguish three cases for the second operand: either it is known statically to deliver
        // nodes only (a traditional path expression), or it is known statically to deliver atomic values
        // only (a simple mapping expression), or we don't yet know.

        ItemType stepType = step.getItemType(th);
        if (th.isSubType(stepType, Type.NODE_TYPE)) {

            if ((step.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {

                // A traditional path expression

                // We don't need the operands to be sorted; any sorting that's needed
                // will be done at the top level

                Optimizer opt = visitor.getConfiguration().getOptimizer();
                start2 = ExpressionTool.unsorted(opt, start, false);
                Expression step2 = ExpressionTool.unsorted(opt, step, false);
                PathExpression path = new PathExpression(start2, step2);
                ExpressionTool.copyLocationInfo(this, path);
                Expression sortedPath = path.addDocumentSorter();
                ExpressionTool.copyLocationInfo(this, sortedPath);
                sortedPath = sortedPath.simplify(visitor);
                return sortedPath.typeCheck(visitor, contextItemType);

            } else {

                // We can still use a path expression, but need to retain the sorting of operands

                PathExpression path = new PathExpression(start, step);
                ExpressionTool.copyLocationInfo(this, path);
                Expression sortedPath = path.addDocumentSorter();
                ExpressionTool.copyLocationInfo(this, sortedPath);
                sortedPath = sortedPath.simplify(visitor);
                return sortedPath.typeCheck(visitor, contextItemType);
            }

            // Decide whether the result needs to be wrapped in a sorting
            // expression to deliver the results in document order

//            int props = getSpecialProperties();
//
//            if ((props & StaticProperty.ORDERED_NODESET) != 0) {
//                return this;
//            } else if ((props & StaticProperty.REVERSE_DOCUMENT_ORDER) != 0) {
//                return SystemFunction.makeSystemFunction("reverse", new Expression[]{this});
//            } else {
//                return new DocumentSorter(this);
//            }

        } else if (stepType.isAtomicType()) {
            // This is a simple mapping expression: a/b where b returns atomic values
            ForEach ame = new ForEach(start, step, false);
            ExpressionTool.copyLocationInfo(this, ame);
            return visitor.typeCheck(visitor.simplify(ame), contextItemType);
        } else {
            // This is a hybrid mapping expression, one where we don't know the type of the step
            // (and therefore, we don't know whether sorting into document order is required) until run-time
            return this;
        }
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        setStartExpression(visitor.optimize(start, contextItemType));
        setStepExpression(step.optimize(visitor, start.getItemType(th)));

        if (Literal.isEmptySequence(start) || Literal.isEmptySequence(step)) {
            return new Literal(EmptySequence.getInstance());
        }

        return promoteFocusIndependentSubexpressions(visitor, contextItemType);
    }

    /**
     * If any subexpressions within the step are not dependent on the focus,
     * and if they are not "creative" expressions (expressions that can create new nodes), then
     * promote them: this causes them to be evaluated once, outside the path expression
     * @param visitor the expression visitor
     * @param contextItemType the type of the context item for evaluating the start expression
     * @return the rewritten expression, or the original expression if no rewrite was possible
     */

    protected Expression promoteFocusIndependentSubexpressions(
            ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        Optimizer opt = visitor.getConfiguration().getOptimizer();

        PromotionOffer offer = new PromotionOffer(opt);
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (start.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.containingExpression = this;

        setStepExpression(doPromotion(step, offer));
        visitor.resetStaticProperties();
        if (offer.containingExpression != this) {
            offer.containingExpression =
                    visitor.optimize(visitor.typeCheck(offer.containingExpression, contextItemType), contextItemType);
            return offer.containingExpression;
        }
        return this;
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            setStartExpression(doPromotion(start, offer));
            if (offer.action == PromotionOffer.REPLACE_CURRENT) {
                // Don't pass on other requests. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
                setStepExpression(doPromotion(step, offer));
            }
            return this;
        }
    }

    /**
     * Get the immediate subexpressions of this expression
     */

    public Iterator<Expression> iterateSubExpressions() {
        return Arrays.asList((new Expression[]{start, step})).iterator();
    }

    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return child == step;
    }

   /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (start == original) {
            setStartExpression(replacement);
            found = true;
        }
        if (step == original) {
            setStepExpression(replacement);
            found = true;
        }
        return found;
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE
     */

    public int computeDependencies() {
        return start.getDependencies() |
                // not all dependencies in the step matter, because the context node, etc,
                // are not those of the outer expression
                (step.getDependencies() &
                (StaticProperty.DEPENDS_ON_XSLT_CONTEXT |
                    StaticProperty.DEPENDS_ON_LOCAL_VARIABLES |
                    StaticProperty.DEPENDS_ON_USER_FUNCTIONS));
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if ((start.getSpecialProperties() & step.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {
            p |= StaticProperty.NON_CREATIVE;
        }
        return p;
    }

    /**
     * Determine the static cardinality of the expression
     */

    public int computeCardinality() {
        int c1 = start.getCardinality();
        int c2 = step.getCardinality();
        return Cardinality.multiply(c1, c2);
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        if (!(other instanceof SlashExpression)) {
            return false;
        }
        SlashExpression p = (SlashExpression) other;
        return (start.equals(p.start) && step.equals(p.step));
    }

    /**
     * get HashCode for comparing two expressions
     */

    public int hashCode() {
        return "SlashExpression".hashCode() + start.hashCode() + step.hashCode();
    }

    /**
     * Iterate the path-expression in a given context
     * @param context the evaluation context
     */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {

        // This class delivers the result of the path expression in unsorted order,
        // without removal of duplicates. If sorting and deduplication are needed,
        // this is achieved by wrapping the path expression in a DocumentSorter

        SequenceIterator result = start.iterate(context);
        XPathContext context2 = context.newMinorContext();
        context2.setCurrentIterator(result);

        result = new ContextMappingIterator(this, context2);

        // Peek at the first item, and depending on its type, check that all the items
        // are atomic values or that all are nodes.
        final SourceLocator loc = getSourceLocator();
        Item first = result.next();
        if (first == null) {
            return EmptyIterator.getInstance();
        } else if (first instanceof AtomicValue) {
            ItemMappingFunction atomicValueChecker = new ItemMappingFunction() {
                public Item mapItem(Item item) throws XPathException {
                    if (item instanceof AtomicValue) {
                        return item;
                    } else {
                        throw reportMixedItems(loc, context);
                    }
                }
            };
            return new ItemMappingIterator(new OneItemGoneIterator(result), atomicValueChecker, true);
        } else {
            ItemMappingFunction nodeChecker = new ItemMappingFunction() {
                public Item mapItem(Item item) throws XPathException {
                    if (item instanceof NodeInfo) {
                        return item;
                    } else {
                        throw reportMixedItems(loc, context);
                    }
                }
            };
            return new DocumentOrderIterator(
                new ItemMappingIterator(new OneItemGoneIterator(result), nodeChecker, true),
                GlobalOrderComparer.getInstance());
        }

    }

    private XPathException reportMixedItems(SourceLocator loc, XPathContext context) {
        XPathException err = new XPathException("Cannot mix nodes and atomic values in the result of a path expression");
        err.setErrorCode("XPTY0018");
        err.setLocator(loc);
        err.setXPathContext(context);
        return err;
    }

    /**
     * Mapping function, from a node returned by the start iteration, to a sequence
     * returned by the child.
     */

    public final SequenceIterator map(XPathContext context) throws XPathException {
        return step.iterate(context);
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     * @return a representation of the expression as a string
     */

    public String toString() {
        return start.toString() + "/" + step.toString();
    }

    /**
       * Get the first step in this expression. A path expression A/B/C is represented as (A/B)/C, but
       * the first step is A
       * @return the first step in the expression, after expanding any nested path expressions
       */

      public Expression getFirstStep() {
          if (start instanceof SlashExpression) {
              return ((SlashExpression) start).getFirstStep();
          } else {
              return start;
          }
      }

    /**
       * Get all steps after the first.
       * This is complicated by the fact that A/B/C is represented as ((A/B)/C; we are required
       * to return B/C
       * @return a path expression containing all steps in this path expression other than the first,
       * after expanding any nested path expressions
       */

      public Expression getRemainingSteps() {
          if (start instanceof SlashExpression) {
              SlashExpression rem =
                      new SlashExpression(((PathExpression) start).getRemainingSteps(), step);
              ExpressionTool.copyLocationInfo(start, rem);
              return rem;
          } else {
              return step;
          }
      }

    /**
       * Get the last step of the path expression
       * @return the last step in the expression, after expanding any nested path expressions
       */

      public Expression getLastStep() {
          if (step instanceof SlashExpression) {
              return ((SlashExpression)step).getLastStep();
          } else {
              return step;
          }
      }

    /**
       * Get a path expression consisting of all steps except the last
       * @return a path expression containing all steps in this path expression other than the last,
       * after expanding any nested path expressions
       */

      public Expression getLeadingSteps() {
          if (step instanceof SlashExpression) {
              SlashExpression rem =
                      new SlashExpression(start, ((SlashExpression) step).getLeadingSteps());
              ExpressionTool.copyLocationInfo(start, rem);
              return rem;
          } else {
              return start;
          }
      }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
