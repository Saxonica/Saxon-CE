package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.functions.Last;
import client.net.sf.saxon.ce.functions.Position;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A LocationPathPattern represents a path, for example of the form A/B/C... The components are represented
 * as a linked list, each component pointing to its predecessor
 */

public final class LocationPathPattern extends Pattern {

    /**
     * Create a LocationPathPattern
     */

    public LocationPathPattern() {}

    // the following public variables are exposed to the ExpressionParser

    private Pattern upperPattern = null;
    private byte upwardsAxis = Axis.PARENT;
    public NodeTest nodeTest = AnyNodeTest.getInstance();
    protected Expression[] filters = EMPTY_FILTER_ARRAY;
    protected Expression equivalentExpr = null;
    protected boolean firstElementPattern = false;
    protected boolean lastElementPattern = false;
    protected boolean specialFilter = false;
    private NodeTest refinedNodeTest = null;

    /**
     * Set the NodeTest
     * @param test the NodeTest
     */

    public void setNodeTest(NodeTest test) {
        if (test == null) {
            throw new NullPointerException("test");
        }
        this.nodeTest = test;
    }

    /**
     * Set the superior pattern (matching a parent or ancestor node
     * @param axis the axis (parent or ancestor) connecting to the upper pattern
     * @param upper the pattern that a parent or ancestor must match
     */

    public void setUpperPattern(byte axis, Pattern upper) {
        this.upwardsAxis = axis;
        this.upperPattern = upper;
    }

    /**
     * Add a filter to the pattern (while under construction)
     *
     * @param filter The predicate (a boolean expression or numeric expression) to be added
     */

    public void addFilter(Expression filter) {
        // Because the number of filters is small and fixed, we add entries to the array one at a time

        int len = filters.length;
        Expression[] f2 = new Expression[len+1];

        System.arraycopy(filters, 0, f2, 0, len);
        filters = f2;
        filters[len] = filter;
        filter.setContainer(this);
    }

    public void setSystemId(String systemId) {
        super.setSystemId(systemId);
        if (upperPattern != null) {
            upperPattern.setSystemId(systemId);
        }
    }

    /**
     * Set the executable containing this pattern
     *
     * @param executable the executable
     */

    public void setExecutable(Executable executable) {
        super.setExecutable(executable);
        if (upperPattern != null) {
            upperPattern.setExecutable(executable);
        }
    }

    /**
     * Get the filters assocated with the last step in the pattern
     * @return an array of expression holding the filter predicates in order
     */

    public Expression[] getFilters() {
        return filters;
    }

    /**
     * Get the pattern applying to the parent node, if there is one
     * @return the parent pattern, for example if the pattern is a/b[1]/c then the parent
     * pattern is a/b[1]
     */

    public Pattern getUpperPattern() {
        return upperPattern;
    }

    /**
     * Get the upwards axis, that is, the axis by which the upper pattern is reached.
     * Typically Axis.PARENT or Axis.ANCESTOR
     */

    public byte getUpwardsAxis() {
        return upwardsAxis;
    }

    /**
     * Simplify the pattern: perform any context-independent optimisations
     * @param visitor an expression visitor
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {

        // detect the simple cases: no parent or ancestor pattern, no predicates

        if (upperPattern == null &&
                filters.length == 0 &&
                !firstElementPattern &&
                !lastElementPattern) {
            NodeTestPattern ntp = new NodeTestPattern(nodeTest);
            ntp.setSystemId(getSystemId());
            return ntp;
        }

        // simplify each component of the pattern

        if (upperPattern != null) {
            upperPattern = upperPattern.simplify(visitor);
        }

        for (int i = filters.length - 1; i >= 0; i--) {
            filters[i] = visitor.simplify(filters[i]);
        }

        return this;
    }

    /**
     * Type-check the pattern, performing any type-dependent optimizations.
     * @param visitor an expression visitor
     * @param contextItemType the type of the context item at the point where the pattern appears
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        // analyze each component of the pattern
        StaticContext env = visitor.getStaticContext();
        final TypeHierarchy th = TypeHierarchy.getInstance();
        if (upperPattern != null) {
            upperPattern = upperPattern.analyze(visitor, contextItemType);
            if (upwardsAxis == Axis.PARENT) {
                // Check that this step in the pattern makes sense in the context of the parent step
                AxisExpression step;
                if (nodeTest.getRequiredNodeKind() == Type.ATTRIBUTE) {
                    step = new AxisExpression(Axis.ATTRIBUTE, nodeTest);
                } else {
                    step = new AxisExpression(Axis.CHILD, nodeTest);
                }
                step.setSourceLocator(this);
                step.setContainer(this);
                Expression exp = visitor.typeCheck(step, upperPattern.getNodeTest());
                refinedNodeTest = (NodeTest) exp.getItemType();
            }
        }

        int removeEntries = 0;
        for (int i = filters.length - 1; i >= 0; i--) {
            Expression filter = visitor.typeCheck(filters[i], getNodeTest());
            filter = visitor.optimize(filter, getNodeTest());
            filters[i] = filter;
            if (Literal.isConstantBoolean(filter, true)) {
                // mark the filter for removal
                removeEntries++;
            } else if (Literal.isConstantBoolean(filter, false)) {
                // if a filter is constant false, the pattern doesn't match anything
                return new NodeTestPattern(EmptySequenceTest.getInstance());
            }
        }
        if (removeEntries > 0) {
            if (removeEntries == filters.length) {
                // remove all predicates
                filters = EMPTY_FILTER_ARRAY;
            } else {
                Expression[] f2 = new Expression[filters.length - removeEntries];
                int j = 0;
                for (int i=0; i<filters.length; i++) {
                     if (!Literal.isConstantBoolean(filters[i], true)) {
                         f2[j++] = filters[i];
                     }
                }
                filters = f2;
            }
        }

        // see if it's an element pattern with a single positional predicate of [1]

        if (nodeTest.getRequiredNodeKind() == Type.ELEMENT && filters.length == 1) {
            if (Literal.isConstantOne(filters[0])) {
                firstElementPattern = true;
                specialFilter = true;
                filters = EMPTY_FILTER_ARRAY;
            } else if (filters[0] instanceof ComparisonExpression) {
                ComparisonExpression comp = (ComparisonExpression)filters[0];
                if (comp.getSingletonOperator() == Token.FEQ &&
                        (comp.getOperands()[0] instanceof Position && Literal.isConstantOne(comp.getOperands()[1])) ||
                        (comp.getOperands()[1] instanceof Position && Literal.isConstantOne(comp.getOperands()[0]))) {
                    firstElementPattern = true;
                    specialFilter = true;
                    filters = EMPTY_FILTER_ARRAY;
                }
            }
        }

        // see if it's an element pattern with a single positional predicate
        // of [position()=last()]

        if (nodeTest.getRequiredNodeKind() == Type.ELEMENT &&
                filters.length == 1 &&
                filters[0] instanceof Last) {
            lastElementPattern = true;
            specialFilter = true;
            filters = EMPTY_FILTER_ARRAY;
        }

        // For a positional pattern, construct the equivalent expression
        if (isPositional(th)) {
            equivalentExpr = makeEquivalentExpression();
            equivalentExpr = visitor.typeCheck(equivalentExpr, contextItemType);
            specialFilter = true;
        }

        return this;

        // TODO:PERF: identify subexpressions within a pattern predicate that could be promoted
        // In the case of match patterns in template rules, these would have to become global variables.
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     */

    public int getDependencies() {
        int dependencies = 0;
        if (upperPattern != null) {
            dependencies |= upperPattern.getDependencies();
        }
        for (int i = 0; i < filters.length; i++) {
            dependencies |= filters[i].getDependencies();
        }
        // the only dependency that's interesting is a dependency on local variables
        dependencies &= StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
        return dependencies;
    }

    /**
     * Iterate over the subexpressions within this pattern
     */

    public Iterator<Expression> iterateSubExpressions() {
        List<Expression> list = new ArrayList<Expression>();
        if (getVariableBindingExpression() != null) {
            // Note that the variable binding must come first to ensure slots are allocated to the "current"
            // variable before the variable reference is encountered
            list.add(getVariableBindingExpression());
        }
        list.addAll(Arrays.asList(filters));
        if (upperPattern != null) {
            for (Iterator<Expression> upper = upperPattern.iterateSubExpressions(); upper.hasNext();) {
                list.add(upper.next());
            }
        }
        return list.iterator();
    }

    /**
     * Allocate slots to any variables used within the pattern
     * @param nextFree the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(int nextFree) {
        // See tests cnfr23, idky239, match54
        // SlotManager slotManager = env.getStyleElement().getContainingSlotManager();
        if (getVariableBindingExpression() != null) {
            nextFree = ExpressionTool.allocateSlots(getVariableBindingExpression(), nextFree);
        }
        for (int i = 0; i < filters.length; i++) {
            nextFree = ExpressionTool.allocateSlots(filters[i], nextFree);
        }
        if (upperPattern != null) {
            nextFree = upperPattern.allocateSlots(nextFree);
        }
        return nextFree;
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>Unlike the corresponding method on {@link client.net.sf.saxon.ce.expr.Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {

        if (upperPattern != null) {
            upperPattern.promote(offer, parent);
        }
        Binding[] savedBindingList = offer.bindingList;
        if (getVariableBindingExpression() instanceof Assignation) {
            offer.bindingList = ((Assignation)getVariableBindingExpression()).extendBindingList(offer.bindingList);
        }
        for (int i = 0; i < filters.length; i++) {
            filters[i] = filters[i].promote(offer, parent);
        }
        offer.bindingList = savedBindingList;
    }

    /**
     * For a positional pattern, make an equivalent path expression to evaluate the filters.
     * This expression takes the node being tested as the context node, and returns a set of nodes
     * which will include the context node if and only if it matches the pattern. The expression only
     * tests nodes against the filters, not against any parent or ancestor patterns.
     * @return the equivalent path expression
     */

    private Expression makeEquivalentExpression() {
        byte axis = (nodeTest.getRequiredNodeKind() == Type.ATTRIBUTE ?
                Axis.ATTRIBUTE :
                Axis.CHILD);
        Expression step = new AxisExpression(axis, nodeTest);
        for (int n = 0; n < filters.length; n++) {
            step = new FilterExpression(step, filters[n]);
        }
        ParentNodeExpression start = new ParentNodeExpression();
        start.setContainer(this);
        PathExpression path = new PathExpression(start, step);
        path.setContainer(this);
        return path;
        // Note, the resulting expression is not required to deliver results in document order
    }

    /**
     * Determine whether the pattern matches a given node.
     *
     *
     * @param node the node to be tested
     * @return true if the pattern matches, else false
     */

    public boolean matches(NodeInfo node, XPathContext context) {
        // if there is a variable to hold the value of current(), bind it now
        bindCurrent(node, context);
        return internalMatches(node, null, context);
        // matches() and internalMatches() differ in the way they handle the current() function.
        // The variable holding the value of current() is initialized on entry to the top-level
        // LocationPathPattern, but not on entry to its subordinate patterns.
    }

    /**
     * Test whether the pattern matches, but without changing the current() node
     */

    protected boolean internalMatches(NodeInfo node, NodeInfo anchor, XPathContext context) {
        // System.err.println("Matching node type and fingerprint");
        if (!nodeTest.matches(node)) {
            return false;
        }
        if (upperPattern != null) {
            NodeInfo anc = node;
            if (upwardsAxis == Axis.PARENT || upwardsAxis == Axis.ANCESTOR) {
                anc = node.getParent();
            }
            while (true) {
                if (anc == null) {
                    return false;
                }
                if (upperPattern.internalMatches(anc, anchor, context)) {
                    break;
                }
                if (upwardsAxis == Axis.PARENT) {
                    return false;
                }
                anc = anc.getParent();
            }
        }

        if (specialFilter) {
            if (firstElementPattern) {
                UnfailingIterator iter = node.iterateAxis(Axis.PRECEDING_SIBLING, nodeTest);
                return iter.next() == null;
            }

            if (lastElementPattern) {
                UnfailingIterator iter = node.iterateAxis(Axis.FOLLOWING_SIBLING, nodeTest);
                return iter.next() == null;
            }

            if (equivalentExpr != null) {

                // for a positional pattern, we do it the hard way: test whether the
                // node is a member of the nodeset obtained by evaluating the
                // equivalent expression

                // System.err.println("Testing positional pattern against node " + node.generateId());
                XPathContext c2 = context.newMinorContext();
                c2.setSingletonFocus(node);
                try {
                    SequenceIterator nsv = equivalentExpr.iterate(c2);
                    while (true) {
                        NodeInfo n = (NodeInfo) nsv.next();
                        if (n == null) {
                            return false;
                        }
                        if (n.isSameNodeInfo(node)) {
                            return true;
                        }
                    }
                } catch (XPathException e) {
                    return false;
                }
            }
        }

        if (filters.length != 0) {
            XPathContext c2 = context.newMinorContext();
            c2.setSingletonFocus(node);
            // it's a non-positional filter, so we can handle each node separately

            for (Expression filter : filters) {
                try {
                    if (!filter.effectiveBooleanValue(c2)) {
                        return false;
                    }
                } catch (XPathException e) {
//                    if ("XTDE0640".equals(e.getErrorCodeLocalPart())) {
//                        // Treat circularity error as fatal (test error213)
//                        throw e;
//                    }
                    // errors in patterns are recoverable
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Node.NODE
     *
     * @return the type of node matched by this pattern. e.g. Node.ELEMENT or Node.TEXT
     */

    public int getNodeKind() {
        return nodeTest.getRequiredNodeKind();
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public NodeTest getNodeTest() {
        if (refinedNodeTest != null) {
            return refinedNodeTest;
        } else {
            return nodeTest;
        }
    }

    /**
     * Determine if the pattern uses positional filters
     * @param th the type hierarchy cache
     * @return true if there is a numeric filter in the pattern, or one that uses the position()
     *         or last() functions
     */

    public boolean isPositional(TypeHierarchy th) {
        for (int i = 0; i < filters.length; i++) {
            ItemType type = filters[i].getItemType();
            if (type == AtomicType.DOUBLE || type == AtomicType.DECIMAL ||
                    type == AtomicType.INTEGER || type == AtomicType.FLOAT || type == AtomicType.ANY_ATOMIC) {
                return true;
            }
            if ((filters[i].getDependencies() &
                    (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST)) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * If the pattern contains any calls on current(), this method is called to modify such calls
     * to become variable references to a variable declared in a specially-allocated local variable
     *
     * @param let   the expression that assigns the local variable. This returns a dummy result, and is executed
     *              just before evaluating the pattern, to get the value of the context item into the variable.
     * @param offer A PromotionOffer used to process the expressions and change the call on current() into
     *              a variable reference
     * @param topLevel
     * @throws XPathException
     */

    public void resolveCurrent(LetExpression let, PromotionOffer offer, boolean topLevel) throws XPathException {
        for (int i = 0; i < filters.length; i++) {
            filters[i] = filters[i].promote(offer, let);
        }
        if (upperPattern instanceof LocationPathPattern) {
            upperPattern.resolveCurrent(let, offer, false);
        }
        if (topLevel) {
            setVariableBindingExpression(let);
        }
    }

    /**
     * Determine whether this pattern is the same as another pattern
     * @param other the other object
     */

    public boolean equals(Object other) {
        if (other instanceof LocationPathPattern) {
            LocationPathPattern lpp = (LocationPathPattern)other;
            if (!Arrays.equals(filters, lpp.filters)) {
                return false;
            }
            if (!nodeTest.equals(lpp.nodeTest)) {
                return false;
            }
            if (upwardsAxis != lpp.upwardsAxis) {
                return false;
            }
            if (upperPattern == null) {
                if (lpp.upperPattern != null) {
                    return false;
                }
            } else {
                if (!upperPattern.equals(lpp.upperPattern)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * hashcode supporting equals()
     */

    public int hashCode() {
        int h = 88267;
        for (int i=0; i<filters.length; i++) {
            h ^= filters[i].hashCode();
        }
        h ^= nodeTest.hashCode();
        if (upperPattern != null) {
            h ^= upperPattern.hashCode();
        }
        h ^= (upwardsAxis<<22);
        return h;
    }

    private static Expression[] EMPTY_FILTER_ARRAY = new Expression[0];


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
