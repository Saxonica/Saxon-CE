package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.sort.DocumentSorter;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceType;

import java.util.Stack;

/**
 * An expression that establishes a set of nodes by following relationships between nodes
 * in the document. Specifically, it consists of a start expression which defines a set of
 * nodes, and a step which defines a relationship to be followed from those nodes to create
 * a new set of nodes.
 * <p/>
 * <p>This class inherits from SlashExpression; it is used in the common case where the SlashExpression
 * is known to return nodes rather than atomic values.</p>
 * <p/>
 * <p>This class is not responsible for sorting the results into document order or removing duplicates.
 * That is done by a DocumentSorter expression which is wrapped around the path expression. However, this
 * class does contain the logic for deciding statically whether the DocumentSorter is needed or not.</p>
 */

public final class PathExpression extends SlashExpression implements ContextMappingFunction {

    // TODO: combine this class with ForEach. Given that a PathExpression does not do sorting into document
    // order, the run-time semantics are identical with xsl:for-each. The more general SlashExpression
    // could also be compiled into a ForEach wrapped in an expression that tests whether the results
    // are nodes, atomic, or mixed, and does the sort in the case where they are nodes.

    private transient int state = 0;    // 0 = raw, 1 = simplified, 2 = analyzed, 3 = optimized

    /**
     * Constructor
     * @param start A node-set expression denoting the absolute or relative set of nodes from which the
     *              navigation path should start.
     * @param step  The step to be followed from each node in the start expression to yield a new
     *              node-set
     */

    public PathExpression(Expression start, Expression step) {
        super(start, step);

        // If start is a path expression such as a, and step is b/c, then
        // instead of a/(b/c) we construct (a/b)/c. This is because it often avoids
        // a sort.

        // The "/" operator in XPath 2.0 is not always associative. Problems
        // can occur if position() and last() are used on the rhs, or if node-constructors
        // appear, e.g. //b/../<d/>. So we only do this rewrite if the step is a path
        // expression in which both operands are axis expressions optionally with predicates

        if (step instanceof PathExpression) {
            PathExpression stepPath = (PathExpression)step;
            if (isFilteredAxisPath(stepPath.getControllingExpression()) && isFilteredAxisPath(stepPath.getControlledExpression())) {
                setStartExpression(new PathExpression(start, stepPath.start));
                setStepExpression(stepPath.step);
            }
        }
    }

    public boolean isHybrid() {
        return false;
    }

    /**
     * Add a document sorting node to the expression tree, if needed
     */

    Expression addDocumentSorter() {
        int props = getSpecialProperties();
        if ((props & StaticProperty.ORDERED_NODESET) != 0) {
            return this;
        } else if ((props & StaticProperty.REVERSE_DOCUMENT_ORDER) != 0) {
            return SystemFunction.makeSystemFunction("reverse", new Expression[]{this});
        } else {
            return new DocumentSorter(this);
        }
    }

    /**
     * Determine whether an expression is an
     * axis step with optional filter predicates.
     * @param exp the expression to be examined
     * @return true if the supplied expression is an AxisExpression, or an AxisExpression wrapped by one
     *         or more filter expressions
     */

    private static boolean isFilteredAxisPath(Expression exp) {
        if (exp instanceof AxisExpression) {
            return true;
        } else {
            while (exp instanceof FilterExpression) {
                exp = ((FilterExpression)exp).getControllingExpression();
            }
            return exp instanceof AxisExpression;
        }
    }

    /**
     * Simplify an expression
     * @param visitor the expression visitor
     * @return the simplified expression
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        if (state > 0) {
            return this;
        }
        state = 1;

        Expression e2 = super.simplify(visitor);
        if (e2 != this) {
            return e2;
        }

        // Remove a redundant "." from the path
        // Note: we can't move this logic to the superclass. See test qxmp190

        if (start instanceof ContextItemExpression) {
            return step;
        }

        if (step instanceof ContextItemExpression) {
            return start;
        }

        return this;
    }

    // Simplify an expression of the form a//b, where b has no positional filters.
    // This comes out of the contructor above as (a/descendent-or-self::node())/child::b,
    // but it is equivalent to a/descendant::b; and the latter is better as it
    // doesn't require sorting. Note that we can't do this until type information is available,
    // as we need to know whether any filters are positional or not.

    private PathExpression simplifyDescendantPath(StaticContext env) {

        Expression st = start;

        // detect .//x as a special case; this will appear as descendant-or-self::node()/x

        if (start instanceof AxisExpression) {
            AxisExpression stax = (AxisExpression)start;
            if (stax.getAxis() != Axis.DESCENDANT_OR_SELF) {
                return null;
            }
            ContextItemExpression cie = new ContextItemExpression();
            ExpressionTool.copyLocationInfo(this, cie);
            st = new PathExpression(cie, stax);
            ExpressionTool.copyLocationInfo(this, st);
        }

        if (!(st instanceof PathExpression)) {
            return null;
        }

        PathExpression startPath = (PathExpression)st;
        if (!(startPath.step instanceof AxisExpression)) {
            return null;
        }

        AxisExpression mid = (AxisExpression)startPath.step;
        if (mid.getAxis() != Axis.DESCENDANT_OR_SELF) {
            return null;
        }


        NodeTest test = mid.getNodeTest();
        if (!(test == null || test instanceof AnyNodeTest)) {
            return null;
        }

        Expression underlyingStep = step;
        while (underlyingStep instanceof FilterExpression) {
            if (((FilterExpression)underlyingStep).isPositional(TypeHierarchy.getInstance())) {
                return null;
            }
            underlyingStep = ((FilterExpression)underlyingStep).getControllingExpression();
        }

        if (!(underlyingStep instanceof AxisExpression)) {
            return null;
        }

        AxisExpression underlyingAxis = (AxisExpression)underlyingStep;
        if (underlyingAxis.getAxis() == Axis.CHILD) {

            Expression newStep =
                    new AxisExpression(Axis.DESCENDANT,
                            ((AxisExpression)underlyingStep).getNodeTest());
            ExpressionTool.copyLocationInfo(this, newStep);

            underlyingStep = step;
            // Add any filters to the new expression. We know they aren't
            // positional, so the order of the filters doesn't technically matter
            // (XPath section 2.3.4 explicitly allows us to change it.)
            // However, in the interests of predictable execution, hand-optimization, and
            // diagnosable error behaviour, we retain the original order.
            Stack<Expression> filters = new Stack<Expression>();
            while (underlyingStep instanceof FilterExpression) {
                filters.add(((FilterExpression)underlyingStep).getFilter());
                underlyingStep = ((FilterExpression)underlyingStep).getControllingExpression();
            }
            while (!filters.isEmpty()) {
                newStep = new FilterExpression(newStep, filters.pop());
                ExpressionTool.copyLocationInfo(step, newStep);
            }

            //System.err.println("Simplified this:");
            //    display(10);
            //System.err.println("as this:");
            //    new PathExpression(startPath.start, newStep).display(10);

            PathExpression newPath = new PathExpression(startPath.start, newStep);
            ExpressionTool.copyLocationInfo(this, newPath);
            return newPath;
        }

        if (underlyingAxis.getAxis() == Axis.ATTRIBUTE) {

            // turn the expression a//@b into a/descendant-or-self::*/@b

            Expression newStep =
                    new AxisExpression(Axis.DESCENDANT_OR_SELF, NodeKindTest.ELEMENT);
            ExpressionTool.copyLocationInfo(this, newStep);

            PathExpression newPath = new PathExpression(
                    new PathExpression(startPath.start, newStep),
                    step);
            ExpressionTool.copyLocationInfo(this, newPath);
            return newPath;
        }

        return null;
    }

    /**
     * Perform type analysis
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = TypeHierarchy.getInstance();
        if (state >= 2) {
            // we've already done the main analysis, and we don't want to do it again because
            // decisions on sorting get upset. But we have new information, namely the contextItemType,
            // so we use that to check that it's a node
            setStartExpression(visitor.typeCheck(start, contextItemType));
            setStepExpression(visitor.typeCheck(step, start.getItemType()));
            return this;
        }
        state = 2;

        setStartExpression(visitor.typeCheck(start, contextItemType));

        // The first operand must be of type node()*

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, "/", 0);
        //role0.setSourceLocator(this);
        role0.setErrorCode("XPTY0019");
        setStartExpression(
                TypeChecker.staticTypeCheck(start, SequenceType.NODE_SEQUENCE, false, role0));

        // Now check the second operand

        setStepExpression(visitor.typeCheck(step, start.getItemType()));

        // If start expression has been reduced to ".", return the step expression

        if (start instanceof ContextItemExpression) {
            return step;
        }

        if ((step.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {

            // A traditional path expression

            // We don't need the operands to be sorted; any sorting that's needed
            // will be done at the top level

            Configuration config = visitor.getConfiguration();
            setStartExpression(ExpressionTool.unsorted(config, start, false));
            setStepExpression(ExpressionTool.unsorted(config, step, false));

            // Try to simplify expressions such as a//b
            PathExpression p = simplifyDescendantPath(visitor.getStaticContext());
            if (p != null) {
                ExpressionTool.copyLocationInfo(this, p);
                return visitor.typeCheck(visitor.simplify(p), contextItemType);
            } else {
                // a failed attempt to simplify the expression may corrupt the parent pointers
                adoptChildExpression(start);
                adoptChildExpression(step);
            }
        }
        return this;
    }

    /**
     * Optimize the expression and perform type analysis
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        // TODO: recognize explosive path expressions such as ..//../..//.. : eliminate duplicates early to contain the size
        // Mainly for benchmarks, but one sees following-sibling::p/preceding-sibling::h2. We could define an expression as
        // explosive if it contains two adjacent steps with opposite directions (except where both are singletons).

        final TypeHierarchy th = TypeHierarchy.getInstance();
        if (state >= 3) {
            // we've already done the main analysis, and we don't want to do it again because
            // decisions on sorting get upset. But we have new information, namely the contextItemType,
            // so we use that to check that it's a node
            setStartExpression(visitor.optimize(start, contextItemType));
            setStepExpression(step.optimize(visitor, start.getItemType()));
            return this;
        }
        state = 3;

        // Rewrite a/b[filter] as (a/b)[filter] to improve the chance of indexing

        Expression lastStep = getLastStep();
        if (lastStep instanceof FilterExpression && !((FilterExpression)lastStep).isPositional(th)) {
            Expression leading = getLeadingSteps();
            Expression p2 = new PathExpression(leading, ((FilterExpression)lastStep).getControllingExpression());
            Expression f2 = new FilterExpression(p2, ((FilterExpression)lastStep).getFilter());
            return f2.optimize(visitor, contextItemType);
        }

        setStartExpression(visitor.optimize(start, contextItemType));
        setStepExpression(step.optimize(visitor, start.getItemType()));

        if (Literal.isEmptySequence(start) || Literal.isEmptySequence(step)) {
            return new Literal(EmptySequence.getInstance());
        }

        // If any subexpressions within the step are not dependent on the focus,
        // and if they are not "creative" expressions (expressions that can create new nodes), then
        // promote them: this causes them to be evaluated once, outside the path expression

        return promoteFocusIndependentSubexpressions(visitor, contextItemType);

    }


    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression p = this;
        Expression exp = offer.accept(parent, p);
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
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int startProperties = start.getSpecialProperties();
        int stepProperties = step.getSpecialProperties();

        int p = 0;
        if (!Cardinality.allowsMany(start.getCardinality())) {
            startProperties |= StaticProperty.ORDERED_NODESET |
                    StaticProperty.PEER_NODESET |
                    StaticProperty.SINGLE_DOCUMENT_NODESET;
        }
        if (!Cardinality.allowsMany(step.getCardinality())) {
            stepProperties |= StaticProperty.ORDERED_NODESET |
                    StaticProperty.PEER_NODESET |
                    StaticProperty.SINGLE_DOCUMENT_NODESET;
        }


        if ((startProperties & stepProperties & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            p |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        if (((startProperties & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) &&
                ((stepProperties & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0)) {
            p |= StaticProperty.SINGLE_DOCUMENT_NODESET;
        }
        if ((startProperties & stepProperties & StaticProperty.PEER_NODESET) != 0) {
            p |= StaticProperty.PEER_NODESET;
        }
        if ((startProperties & stepProperties & StaticProperty.SUBTREE_NODESET) != 0) {
            p |= StaticProperty.SUBTREE_NODESET;
        }

        if (testNaturallySorted(startProperties, stepProperties)) {
            p |= StaticProperty.ORDERED_NODESET;
        }

        if (testNaturallyReverseSorted()) {
            p |= StaticProperty.REVERSE_DOCUMENT_ORDER;
        }

        if ((startProperties & stepProperties & StaticProperty.NON_CREATIVE) != 0) {
            p |= StaticProperty.NON_CREATIVE;
        }

        return p;
    }


    /**
     * Determine if we can guarantee that the nodes are delivered in document order.
     * This is true if the start nodes are sorted peer nodes
     * and the step is based on an Axis within the subtree rooted at each node.
     * It is also true if the start is a singleton node and the axis is sorted.
     * @param startProperties the properties of the left-hand expression
     * @param stepProperties  the properties of the right-hand expression
     * @return true if the natural nested-loop evaluation strategy for the expression
     *         is known to deliver results with no duplicates and in document order, that is,
     *         if no additional sort is required
     */

    private boolean testNaturallySorted(int startProperties, int stepProperties) {

        // System.err.println("**** Testing pathExpression.isNaturallySorted()");
        // display(20);
        // System.err.println("Start is ordered node-set? " + start.isOrderedNodeSet());
        // System.err.println("Start is naturally sorted? " + start.isNaturallySorted());
        // System.err.println("Start is singleton? " + start.isSingleton());

        if ((stepProperties & StaticProperty.ORDERED_NODESET) == 0) {
            return false;
        }
        if (Cardinality.allowsMany(start.getCardinality())) {
            if ((startProperties & StaticProperty.ORDERED_NODESET) == 0) {
                return false;
            }
        } else {
            //if ((stepProperties & StaticProperty.ORDERED_NODESET) != 0) {
            return true;
            //}
        }

        // We know now that both the start and the step are sorted. But this does
        // not necessarily mean that the combination is sorted.

        // The result is sorted if the start is sorted and the step selects attributes
        // or namespaces

        if ((stepProperties & StaticProperty.ATTRIBUTE_NS_NODESET) != 0) {
            return true;
        }

        // The result is sorted if the start selects "peer nodes" (that is, a node-set in which
        // no node is an ancestor of another) and the step selects within the subtree rooted
        // at the context node

        return ((startProperties & StaticProperty.PEER_NODESET) != 0) &&
                ((stepProperties & StaticProperty.SUBTREE_NODESET) != 0);

    }

    /**
     * Determine if the path expression naturally returns nodes in reverse document order
     * @return true if the natural nested-loop evaluation strategy returns nodes in reverse
     *         document order
     */

    private boolean testNaturallyReverseSorted() {

        // Some examples of path expressions that are naturally reverse sorted:
        //     ancestor::*/@x
        //     ../preceding-sibling::x
        //     $x[1]/preceding-sibling::node()

        // This information is used to do a simple reversal of the nodes
        // instead of a full sort, which is significantly cheaper, especially
        // when using tree models (such as DOM and JDOM) in which comparing
        // nodes in document order is an expensive operation.


        if (!Cardinality.allowsMany(start.getCardinality()) &&
                (step instanceof AxisExpression)) {
            return !Axis.isForwards[((AxisExpression)step).getAxis()];
        }

        return !Cardinality.allowsMany(step.getCardinality()) &&
                (start instanceof AxisExpression) &&
                !Axis.isForwards[((AxisExpression)start).getAxis()];

    }

    /**
     * Get all steps after the first.
     * This is complicated by the fact that A/B/C is represented as ((A/B)/C; we are required
     * to return B/C
     * @return a path expression containing all steps in this path expression other than the first,
     *         after expanding any nested path expressions
     */

    @Override
    public Expression getRemainingSteps() {
        if (start instanceof PathExpression) {
            PathExpression rem =
                    new PathExpression(((PathExpression)start).getRemainingSteps(), step);
            ExpressionTool.copyLocationInfo(start, rem);
            return rem;
        } else {
            return step;
        }
    }

    /**
     * Test whether a path expression is an absolute path - that is, a path whose first step selects a
     * document node
     * @param th the type hierarchy cache
     * @return true if the first step in this path expression selects a document node
     */

    public boolean isAbsolute(TypeHierarchy th) {
        Expression first = getFirstStep();
        if (th.isSubType(first.getItemType(), NodeKindTest.DOCUMENT)) {
            return true;
        }
        // This second test allows keys to be built. See XMark q9.
//        if (first instanceof AxisExpression && ((AxisExpression)first).getContextItemType().getPrimitiveType() == Type.DOCUMENT) {
//            return true;
//        };
        return false;
    }

    /**
     * Iterate the path-expression in a given context
     * @param context the evaluation context
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // This class delivers the result of the path expression in unsorted order,
        // without removal of duplicates. If sorting and deduplication are needed,
        // this is achieved by wrapping the path expression in a DocumentSorter

        SequenceIterator master = start.iterate(context);
        XPathContext context2 = context.newMinorContext();
        context2.setCurrentIterator(master);

        return new ContextMappingIterator(this, context2);

    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return "(" + start.toString() + "/" + step.toString() + ")";
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
