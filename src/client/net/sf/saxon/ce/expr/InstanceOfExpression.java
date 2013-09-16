package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.SequenceType;

/**
* InstanceOf Expression: implements "Expr instance of data-type"
*/

public final class InstanceOfExpression extends UnaryExpression {

    ItemType targetType;
    int targetCardinality;

    /**
     * Construct an "instance of" expression in the form "source instance of target"
     * @param source the expression whose type is to be tested
     * @param target the type against which it is tested
     */

    public InstanceOfExpression(Expression source, SequenceType target) {
        super(source);
        targetType = target.getPrimaryType();
        if (targetType == null) {
            throw new IllegalArgumentException("Primary item type must not be null");
        }
        targetCardinality = target.getCardinality();
    }

    /**
     * Get the item type that we are testing for membership of
     * @return the item type
     */

    public ItemType getRequiredItemType() {
        return targetType;
    }

    /**
     * Get the cardinality that we are testing for membership of
     * @return the required cardinality
     */

    public int getRequiredCardinality() {
        return targetCardinality;
    }

    /**
    * Type-check the expression
    * @return the checked expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        if (operand instanceof Literal) {
            Literal lit = Literal.makeLiteral(
                    (AtomicValue)evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext()));
            ExpressionTool.copyLocationInfo(this, lit);
            return lit;
        }

        // See if we can get the answer by static analysis.

        if (Cardinality.subsumes(targetCardinality, operand.getCardinality())) {
            final TypeHierarchy th = TypeHierarchy.getInstance();
            int relation = th.relationship(operand.getItemType(), targetType);
            if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                Literal lit = Literal.makeLiteral(BooleanValue.TRUE);
                ExpressionTool.copyLocationInfo(this, lit);
                return lit;
            } else if (relation == TypeHierarchy.DISJOINT) {
                // if the item types are disjoint, the result might still be true if both sequences are empty
                if (!Cardinality.allowsZero(targetCardinality) || !Cardinality.allowsZero(operand.getCardinality())) {
                    Literal lit =  Literal.makeLiteral(BooleanValue.FALSE);
                    ExpressionTool.copyLocationInfo(this, lit);
                    return lit;
                }
            }
        }
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
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        if (Cardinality.subsumes(targetCardinality, operand.getCardinality())) {
            final TypeHierarchy th = TypeHierarchy.getInstance();
            int relation = th.relationship(operand.getItemType(), targetType);
            if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                return Literal.makeLiteral(BooleanValue.TRUE);
            } else if (relation == TypeHierarchy.DISJOINT) {
                // if the item types are disjoint, the result might still be true if both sequences are empty
                if (!Cardinality.allowsZero(targetCardinality) || !Cardinality.allowsZero(operand.getCardinality())) {
                    return Literal.makeLiteral(BooleanValue.FALSE);
                }
            }
        }
        return this;
    }


    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((InstanceOfExpression)other).targetType &&
                targetCardinality == ((InstanceOfExpression)other).targetCardinality;
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ targetType.hashCode() ^ targetCardinality;
    }

    /**
     * Determine the cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the data type of the result of the InstanceOf expression
     */

    public ItemType getItemType() {
        return AtomicType.BOOLEAN;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the expression as a boolean
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        return isInstance(iter, context);
    }

    private boolean isInstance(SequenceIterator iter, XPathContext context) throws XPathException {
        int count = 0;
        while (true) {
            Item item = iter.next();
            if (item == null) break;
            count++;
            if (!targetType.matchesItem(item, false, context.getConfiguration())) {
                return false;
            }
            if (count==2 && !Cardinality.allowsMany(targetCardinality)) {
                return false;
            }
        }
        return !(count == 0 && ((targetCardinality & StaticProperty.ALLOWS_ZERO) == 0));
    }

    /**
     * Return an iterator over the results of the expression, given an iterator over the principal
     * operand to the expression
     * @param base an interator over the input to the expression
     * @param context dynamic evaluation context
     * @return an iterator over the results of the expression
     * @throws XPathException
     */

    public SequenceIterator getMappingIterator(SequenceIterator base, XPathContext context) throws XPathException {
        boolean b = isInstance(base, context);
        return SingletonIterator.makeIterator(BooleanValue.get(b));
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.