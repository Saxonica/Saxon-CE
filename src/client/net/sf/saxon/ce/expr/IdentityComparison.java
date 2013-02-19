package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.expr.sort.GlobalOrderComparer;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.SequenceType;


/**
* IdentityComparison: a boolean expression that compares two nodes
* for equals, not-equals, greater-than or less-than based on identity and
* document ordering
*/

public final class IdentityComparison extends BinaryExpression {

    /**
    * Create an identity comparison identifying the two operands and the operator
    * @param p1 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
    * @param p2 the right-hand operand
    */

    public IdentityComparison(Expression p1, int op, Expression p2) {
        super(p1, op, p2);
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        operand0 = TypeChecker.staticTypeCheck(
                operand0, SequenceType.OPTIONAL_NODE, false, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        operand1 = TypeChecker.staticTypeCheck(
                operand1, SequenceType.OPTIONAL_NODE, false, role1, visitor);
        return this;
    }

     /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo node1 = getNode(operand0, context);
        if (node1==null) {
            return null;
        }

        NodeInfo node2 = getNode(operand1, context);
        if (node2==null) {
            return null;
        }

        return BooleanValue.get(compareIdentity(node1, node2));
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        NodeInfo node1 = getNode(operand0, context);
        if (node1==null) {
            return false;
        }

        NodeInfo node2 = getNode(operand1, context);
        return node2 != null && compareIdentity(node1, node2);

    }

    private boolean compareIdentity(NodeInfo node1, NodeInfo node2) {

        switch (operator) {
        case Token.IS:
            return node1.isSameNodeInfo(node2);
        case Token.PRECEDES:
            return GlobalOrderComparer.getInstance().compare(node1, node2) < 0;
        case Token.FOLLOWS:
            return GlobalOrderComparer.getInstance().compare(node1, node2) > 0;
        default:
            throw new UnsupportedOperationException("Unknown node identity test");
        }
    }

    private static NodeInfo getNode(Expression exp, XPathContext c) throws XPathException {
        return (NodeInfo)exp.evaluateItem(c);
    }


    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.