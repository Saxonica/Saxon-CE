package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.SlotManager;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;

import java.util.*;

/**
 * A pattern formed as the union (or) of two other patterns
 */

public class UnionPattern extends Pattern {

    protected Pattern p1, p2;
    private int nodeType = Type.NODE;
    private Expression variableBinding = null;      // local variable to which the current() node is bound

    /**
     * Constructor
     *
     * @param p1 the left-hand operand
     * @param p2 the right-hand operand
     */

    public UnionPattern(Pattern p1, Pattern p2) {
        this.p1 = p1;
        this.p2 = p2;
        if (p1.getNodeKind() == p2.getNodeKind()) {
            nodeType = p1.getNodeKind();
        }
    }


    /**
     * Set the executable containing this pattern
     *
     * @param executable the executable
     */

    public void setExecutable(Executable executable) {
        p1.setExecutable(executable);
        p2.setExecutable(executable);
        super.setExecutable(executable);
    }

    /**
     * Simplify the pattern: perform any context-independent optimisations
     *
     * @param visitor an expression visitor
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {
        p1 = p1.simplify(visitor);
        p2 = p2.simplify(visitor);
        return this;
    }

    /**
     * Type-check the pattern.
     * This is only needed for patterns that contain variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        p1 = p1.analyze(visitor, contextItemType);
        p2 = p2.analyze(visitor, contextItemType);
        return this;
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
        p1.resolveCurrent(let, offer, false);
        p2.resolveCurrent(let, offer, false);
        if (topLevel) {
            variableBinding = let;
        }
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
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        p1.promote(offer, parent);
        p2.promote(offer, parent);
    }

    /**
     * Replace a subexpression by a replacement subexpression
     * @param original    the expression to be replaced
     * @param replacement the new expression to be inserted in its place
     * @return true if the replacement was carried out
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        if (original == variableBinding) {
            variableBinding = replacement;
            return true;
        } else {
            return p1.replaceSubExpression(original, replacement) ||
                p2.replaceSubExpression(original, replacement);
        }
    }

    /**
     * Set the original text
     */

    public void setOriginalText(String pattern) {
        super.setOriginalText(pattern);
        p1.setOriginalText(pattern);
        p2.setOriginalText(pattern);
    }

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param env      the static context in the XSLT stylesheet
     * @param slotManager
     *@param nextFree the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(StaticContext env, SlotManager slotManager, int nextFree) {
        if (variableBinding != null) {
            nextFree = ExpressionTool.allocateSlots(variableBinding, nextFree, slotManager);
        }
        nextFree = p1.allocateSlots(env, slotManager, nextFree);
        nextFree = p2.allocateSlots(env, slotManager, nextFree);
        return nextFree;
    }

    /**
     * Gather the component (non-union) patterns of this union pattern
     * @param set the set into which the components will be added
     */

    public void gatherComponentPatterns(Set set) {
        if (p1 instanceof UnionPattern) {
            ((UnionPattern)p1).gatherComponentPatterns(set);
        } else {
            set.add(p1);
        }
        if (p2 instanceof UnionPattern) {
            ((UnionPattern)p2).gatherComponentPatterns(set);
        } else {
            set.add(p2);
        }
    }

     /**
     * Set an expression used to bind the variable that represents the value of the current() function
     * @param exp the expression that binds the variable
     */

    public void setVariableBindingExpression(Expression exp) {
        variableBinding = exp;
    }

    public Expression getVariableBindingExpression() {
        return variableBinding;
    }

    /**
     * Determine if the supplied node matches the pattern
     *
     * @param e the node to be compared
     * @return true if the node matches either of the operand patterns
     */

    public boolean matches(NodeInfo e, XPathContext context) throws XPathException {
        if (variableBinding != null) {
            XPathContext c2 = context;
            Item ci = context.getContextItem();
            if (!(ci instanceof NodeInfo && ((NodeInfo)ci).isSameNodeInfo(e))) {
                c2 = context.newContext();
                UnfailingIterator si = SingletonIterator.makeIterator(e);
                si.next();
                c2.setCurrentIterator(si);
            }
            variableBinding.evaluateItem(c2);
        }
        return p1.matches(e, context) || p2.matches(e, context);
    }

    /**
     * Determine whether this pattern matches a given Node within the subtree rooted at a given
     * anchor node. This method is used when the pattern is used for streaming.
     * @param node    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param anchor  The anchor node, which must match any AnchorPattern subpattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matchesBeneathAnchor(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        return p1.matchesBeneathAnchor(node, anchor, context) ||
                p2.matchesBeneathAnchor(node, anchor, context);
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Node.NODE
     *
     * @return the type of node matched by this pattern. e.g. Node.ELEMENT or Node.TEXT
     */

    public int getNodeKind() {
        return nodeType;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public NodeTest getNodeTest() {
        if (nodeType == Type.NODE) {
            return AnyNodeTest.getInstance();
        } else {
            return NodeKindTest.makeNodeKindTest(nodeType);
        }
    }


    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     *
     * @return the dependencies, as a bit-significant mask
     */

    public int getDependencies() {
        return p1.getDependencies() | p2.getDependencies();
    }

    /**
     * Iterate over the subexpressions within this pattern
     * @return an iterator over the subexpressions. 
     */

    public Iterator iterateSubExpressions() {
        List<Expression> list = new ArrayList<Expression>();
        if (variableBinding != null) {
            list.add(variableBinding);
        }
        for (Iterator<Expression> i1 = p1.iterateSubExpressions(); i1.hasNext();) {
            list.add(i1.next());
        }
        for (Iterator<Expression> i2 = p2.iterateSubExpressions(); i2.hasNext();) {
            list.add(i2.next());
        }
        return list.iterator();
    }

    /**
     * Get the LHS of the union
     *
     * @return the first operand of the union
     */

    public Pattern getLHS() {
        return p1;
    }

    /**
     * Get the RHS of the union
     *
     * @return the second operand of the union
     */

    public Pattern getRHS() {
        return p2;
    }

    /**
     * Override method to set the system ID, so it's set on both halves
     */

    public void setSystemId(String systemId) {
        super.setSystemId(systemId);
        p1.setSystemId(systemId);
        p2.setSystemId(systemId);
    }

    /**
     * Determine whether this pattern is the same as another pattern
     * @param other the other object
     */

    public boolean equals(Object other) {
        if (other instanceof UnionPattern) {
            Set s0 = new HashSet(10);
            gatherComponentPatterns(s0);
            Set s1 = new HashSet(10);
            ((UnionPattern)other).gatherComponentPatterns(s1);
            return s0.equals(s1);
        } else {
            return false;
        }
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x9bd723a6 ^ p1.hashCode() ^ p2.hashCode();
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
