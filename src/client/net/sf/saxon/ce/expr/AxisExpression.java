package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;


/**
 * An AxisExpression is always obtained by simplifying a PathExpression.
 * It represents a PathExpression that starts at the context node, and uses
 * a simple node-test with no filters. For example "*", "title", "./item",
 * "@*", or "ancestor::chapter*".
 *
 * <p>An AxisExpression delivers nodes in axis order (not in document order).
 * To get nodes in document order, in the case of a reverse axis, the expression
 * should be wrapped in a call on reverse().</p>
*/

public final class AxisExpression extends Expression {

    private byte axis;
    private NodeTest test;
    private ItemType itemType = null;
    private ItemType contextItemType = null;
    int computedCardinality = -1;
    private boolean doneWarnings = false;

    /**
     * Constructor
     * @param axis       The axis to be used in this AxisExpression: relevant constants are defined
     *                   in class client.net.sf.saxon.ce.om.Axis.
     * @param nodeTest   The conditions to be satisfied by selected nodes. May be null,
     *                   indicating that any node on the axis is acceptable
     * @see client.net.sf.saxon.ce.om.Axis
     */

    public AxisExpression(byte axis, NodeTest nodeTest) {
        this.axis = axis;
        test = nodeTest;
    }

    /**
     * Simplify an expression
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) {

        if (axis == Axis.PARENT && (test==null || test instanceof AnyNodeTest)) {
            ParentNodeExpression p = new ParentNodeExpression();
            ExpressionTool.copyLocationInfo(this, p);
            return p;
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        StaticContext env = visitor.getStaticContext();
        if (contextItemType == null) {
            typeError(visitor, "Axis step " + toString() +
                    " cannot be used here: the context item is undefined", "XPDY0002", null);
        }
        if (contextItemType.isAtomicType()) {
            typeError(visitor, "Axis step " + toString() +
                    " cannot be used here: the context item is an atomic value", "XPTY0020", null);
        }

        if (this.contextItemType == contextItemType && doneWarnings) {
            return this;
        }

        this.contextItemType = contextItemType;
        doneWarnings = true;

        if (contextItemType instanceof NodeTest) {
            int origin = ((NodeTest)contextItemType).getRequiredNodeKind();
            if (origin != Type.NODE) {
                if (Axis.isAlwaysEmpty(axis, origin)) {
                    env.issueWarning("The " + Axis.axisName[axis] + " axis starting at " +
                            (origin==Type.ELEMENT || origin == Type.ATTRIBUTE ? "an " : "a ") +
                            NodeKindTest.nodeKindName(origin) + " node will never select anything",
                            getSourceLocator());
                    return Literal.makeEmptySequence();
                }
            }

            if (test != null) {
                int kind = test.getRequiredNodeKind();
                if (kind != Type.NODE) {
                    if (!Axis.containsNodeKind(axis, kind)) {
                        env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any " +
                            NodeKindTest.nodeKindName(kind) + " nodes",
                            getSourceLocator());
                        return Literal.makeEmptySequence();
                    }
                }
                if (axis==Axis.SELF && kind!=Type.NODE && origin!=Type.NODE && kind!=origin) {
                    env.issueWarning("The self axis will never select any " +
                            NodeKindTest.nodeKindName(kind) +
                            " nodes when starting at " +
                            (origin==Type.ELEMENT || origin == Type.ATTRIBUTE ? "an " : "a ")  +
                            NodeKindTest.nodeKindName(origin) + " node", getSourceLocator());
                    return Literal.makeEmptySequence();
                }
            }
        }

        return this;
    }

    /**
     * Get the static type of the context item for this AxisExpression. May be null if not known.
     * @return the statically-inferred type, or null if not known
     */

    public ItemType getContextItemType() {
        return contextItemType;
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
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) {
        return this;
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        if (!(other instanceof AxisExpression)) {
            return false;
        }
        if (axis != ((AxisExpression)other).axis) {
            return false;
        }
        if (test==null) {
            return ((AxisExpression)other).test==null;
        }
        return test.toString().equals(((AxisExpression)other).test.toString());
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        // generate an arbitrary hash code that depends on the axis and the node test
        int h = 9375162 + axis<<20;
        if (test != null) {
            h ^= test.getRequiredNodeKind()<<16;
            h ^= test.getRequiredNodeName().hashCode();
        }
        return h;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
    * XPathContext.CURRENT_NODE
    */

    public int getIntrinsicDependencies() {
	    return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    private Expression copy() {
        AxisExpression a2 = new AxisExpression(axis, test);
        a2.itemType = itemType;
        a2.contextItemType = contextItemType;
        a2.computedCardinality = computedCardinality;
        a2.doneWarnings = false;
        return a2;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        return StaticProperty.CONTEXT_DOCUMENT_NODESET |
               StaticProperty.SINGLE_DOCUMENT_NODESET |
               StaticProperty.NON_CREATIVE |
               (Axis.isForwards[axis] ? StaticProperty.ORDERED_NODESET  : StaticProperty.REVERSE_DOCUMENT_ORDER) |
               (Axis.isPeerAxis[axis] ? StaticProperty.PEER_NODESET : 0) |
               (Axis.isSubtreeAxis[axis] ? StaticProperty.SUBTREE_NODESET : 0) |
               ((axis==Axis.ATTRIBUTE || axis==Axis.NAMESPACE) ? StaticProperty.ATTRIBUTE_NS_NODESET : 0);
    }

    /**
     * Determine the data type of the items returned by this expression
     * @return Type.NODE or a subtype, based on the NodeTest in the axis step, plus
     * information about the content type if this is known from schema analysis
     * @param th the type hierarchy cache
     */

    public final ItemType getItemType(TypeHierarchy th) {
        if (itemType != null) {
            return itemType;
        }
        int p = Axis.principalNodeType[axis];
        switch (p) {
        case Type.ATTRIBUTE:
        case Type.NAMESPACE:
            return NodeKindTest.makeNodeKindTest(p);
        default:
            if (test==null) {
                return AnyNodeTest.getInstance();
            } else {
                return test;
                //return NodeKindTest.makeNodeKindTest(test.getPrimitiveType());
            }
        }
    }

    /**
    * Determine the cardinality of the result of this expression
    */

    public final int computeCardinality() {
        if (computedCardinality != -1) {
            // This takes care of the case where cardinality was computed during type checking of the child axis
            return computedCardinality;
        }


        if (axis == Axis.ATTRIBUTE && test instanceof NameTest) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else if (axis == Axis.SELF) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
        // the parent axis isn't handled by this class
    }

    /**
     * Determine whether the expression can be evaluated without reference to the part of the context
     * document outside the subtree rooted at the context node.
     * @return true if the expression has no dependencies on the context node, or if the only dependencies
     *         on the context node are downward selections using the self, child, descendant, attribute, and namespace
     *         axes.
     */

    public boolean isSubtreeExpression() {
        return Axis.isSubtreeAxis[axis];
    }

    /**
     * Get the axis
     * @return the axis number, for example {@link Axis#CHILD}
    */

    public byte getAxis() {
        return axis;
    }

    /**
     * Get the NodeTest. Returns null if the AxisExpression can return any node.
     * @return the node test, or null if all nodes are returned
    */

    public NodeTest getNodeTest() {
        return test;
    }


    /**
    * Evaluate the path-expression in a given context to return a NodeSet
    * @param context the evaluation context
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        try {
            if (test==null) {
                return ((NodeInfo)item).iterateAxis(axis);
            } else {
                return ((NodeInfo)item).iterateAxis(axis, test);
            }
        } catch (Exception exe) {
            String cName = toString();
            boolean isCCE = (exe instanceof ClassCastException);
           if (exe instanceof NullPointerException || item == null || isCCE) {
                String appendText = " is " + ((isCCE)? "not a node" : "undefined");
                String code = (isCCE)? "XPTY0020" : "XPDY0002";
	            XPathException err = new XPathException("The context item for axis step " +
	            		cName + appendText);
	            err.setErrorCode(code);
	            err.setXPathContext(context);
	            err.setLocator(getSourceLocator());
	            err.setIsTypeError(true);
	            throw err;
            } else { // if (exe instanceof UnsupportedOperationException) {
	            if (exe.getCause() instanceof XPathException) {
	                XPathException ec = (XPathException)exe.getCause();
	                ec.maybeSetLocation(getSourceLocator());
	                ec.maybeSetContext(context);
	                throw ec;
	            } else {
	                // the namespace axis is not supported for all tree implementations
	                dynamicError("Axis Expression Error on: " + cName + " " + exe.getMessage(), "XPST0010", context);
	                return null;
	            }
            }
        }
    }

    /**
     * Represent the expression as a string for diagnostics
     */

    public String toString() {
        return Axis.axisName[axis] +
                "::" +
                (test==null ? "node()" : test.toString());
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
