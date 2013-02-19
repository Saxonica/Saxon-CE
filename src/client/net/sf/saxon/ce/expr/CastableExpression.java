package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.BooleanValue;

/**
* Castable Expression: implements "Expr castable as atomic-type?".
* The implementation simply wraps a cast expression with a try/catch.
*/

public final class CastableExpression extends UnaryExpression {

    AtomicType targetType;
    boolean allowEmpty;

    /**
     * Create a "castable" expression of the form "source castable as target"
     * @param source The source expression
     * @param target The type being tested against
     * @param allowEmpty true if an empty sequence is acceptable, that is if the expression
     * was written as "source castable as target?"
     */

    public CastableExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source);
        targetType = target;
        this.allowEmpty = allowEmpty;
    }

    /**
     * Simplify the expression
     * @return the simplified expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        return preEvaluate(visitor);
    }

    private Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (Literal.isAtomic(operand)) {
            return Literal.makeLiteral(
                    BooleanValue.get(effectiveBooleanValue(visitor.getStaticContext().makeEarlyEvaluationContext())));
        }
        if (Literal.isEmptySequence(operand)) {
            return new Literal(BooleanValue.get(allowEmpty));
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);

        // We need to take care here. The usual strategy of wrapping the operand in an expression that
        // does type-checking doesn't work here, because an error in the type checking should be caught,
        // while an error in evaluating the expression as written should not.

//        SequenceType atomicType = SequenceType.makeSequenceType(
//                                 BuiltInAtomicType.ANY_ATOMIC,
//                                 (allowEmpty ? StaticProperty.ALLOWS_ZERO_OR_ONE
//                                             : StaticProperty.EXACTLY_ONE));
//
//        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "castable as", 0, null);
//        role.setSourceLocator(this);
//        try {
//            operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, env);
//        } catch (XPathException err) {
//            return Literal.makeLiteral(BooleanValue.FALSE);
//        }

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (!CastExpression.isPossibleCast(
                operand.getItemType(th).getAtomizedItemType().getPrimitiveType(),
                targetType.getPrimitiveType())) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        return preEvaluate(visitor);
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        return preEvaluate(visitor);
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((CastableExpression)other).targetType &&
                allowEmpty == ((CastableExpression)other).allowEmpty;
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ targetType.hashCode();
    }

    /**
     * Determine the data type of the result of the Castable expression
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        int count = 0;
        SequenceIterator iter = operand.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            AtomicValue av = item.getTypedValue();
            count++;
            if (count > 1) {
                return false;
            }
            if (!!(av.convert(targetType, true) instanceof ValidationFailure)) {
                return false;
            }
        }
        return count != 0 || allowEmpty;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.