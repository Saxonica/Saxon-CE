package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.sort.AtomicComparer;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.functions.NumberFn;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * GeneralComparison: a boolean expression that compares two expressions
 * for equals, not-equals, greater-than or less-than. This implements the operators
 * =, !=, <, >, etc. This version of the class implements general comparisons
 * in both XPath 1.0 backwards compatibility mode and 2.0 mode. As a result,
 * no static type checking is done.
*/

public class GeneralComparison extends BinaryExpression {

    private AtomicComparer comparer;
    private boolean backwardsCompatible;


    /**
    * Create a general comparison identifying the two operands and the operator
    * @param p0 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
    * @param p1 the right-hand operand
    */

    public GeneralComparison(Expression p0, int op, Expression p1) {
        super(p0, op, p1);
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    @Override
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        backwardsCompatible = visitor.getStaticContext().isInBackwardsCompatibleMode();
        return super.simplify(visitor);
    }

    /**
    * Type-check the expression
    * @return the checked expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);

        StaticContext env = visitor.getStaticContext();
        StringCollator comp = env.getConfiguration().getNamedCollation(env.getDefaultCollationName());
        if (comp==null) {
            comp = CodepointCollator.getInstance();
        }

        comparer = new GenericAtomicComparer(comp, env.getConfiguration().getImplicitTimezone());

        return this;
    }

    public void setAtomicComparer(AtomicComparer comparer) {
        this.comparer = comparer;
    }

    /**
    * Optimize the expression
    * @return the checked expression
    */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        Configuration config = visitor.getConfiguration();

        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);

        // Neither operand needs to be sorted

        operand0 = ExpressionTool.unsorted(config, operand0, false);
        operand1 = ExpressionTool.unsorted(config, operand1, false);

        return this;
    }

    /**
    * Evaluate the expression in a given context
    * @param context the given context for evaluation
    * @return a BooleanValue representing the result of the numeric comparison of the two operands
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the expression in a boolean context
    * @param context the given context for evaluation
    * @return a boolean representing the result of the numeric comparison of the two operands
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {

        Sequence v0 = SequenceExtent.makeSequenceExtent(operand0.iterate(context));
        Sequence v1 = SequenceExtent.makeSequenceExtent(operand1.iterate(context));

        if (backwardsCompatible) {
            // If either operand is a singleton boolean, convert the other to a singleton boolean
            if (v0 instanceof BooleanValue) {
                v1 = BooleanValue.get(ExpressionTool.effectiveBooleanValue(Value.asIterator(v1)));
            } else if (v1 instanceof BooleanValue) {
                v0 = BooleanValue.get(ExpressionTool.effectiveBooleanValue(Value.asIterator(v0)));
            }
        }

        // Atomize both operands

        SequenceIterator s0 = atomize(v0);
        SequenceIterator s1 = atomize(v1);

        if (backwardsCompatible) {
            // If the operator is <, >, etc, convert all items to double using the number() function
            if (operator == Token.LT || operator == Token.LE || operator == Token.GT || operator == Token.GE) {
                ItemMappingFunction map = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        return NumberFn.convert((AtomicValue)item);
                    }
                };
                if (!(v0 instanceof DoubleValue)) {
                    s0 = new ItemMappingIterator(s0, map, true);
                }
                if (!(v1 instanceof DoubleValue)) {
                    s1 = new ItemMappingIterator(s1, map, true);
                }
            }
        }

        // Compare all pairs of atomic values in the two atomized sequences

        Value val1 = Value.asValue(SequenceExtent.makeSequenceExtent(s1));
        int singletonOperator = getSingletonOperator(operator);

        while (true) {
            AtomicValue a = (AtomicValue)s0.next();
            if (a == null) {
                return false;
            }
            for (int j=0; j<val1.getLength(); j++) {
                AtomicValue b = (AtomicValue)val1.itemAt(j);
                if (compare(a, singletonOperator, b, comparer)) {
                    return true;
                }
            }
        }

    }

    /**
     * Atomize a sequence
     * @param v the sequence to be atomized
     * @return an iterator over the atomized sequence
     * @throws XPathException if atomization fails
     */

    private static SequenceIterator atomize(Sequence v) throws XPathException {
        if (v instanceof AtomicValue) {
            return SingletonIterator.makeIterator((AtomicValue)v);
        } else {
            return Atomizer.getAtomizingIterator(Value.asIterator(v));
        }
    }


    /**
    * Compare two atomic values
     *
     * @param a0 the first value to be compared
     * @param operator the comparison operator
     * @param a1 the second value to be compared
     * @param comparer the comparer to be used (perhaps including a collation)
     * @return the result of the comparison
     * @throws XPathException if comparison fails
    */

    private boolean compare(AtomicValue a0,
                                   int operator,
                                   AtomicValue a1,
                                   AtomicComparer comparer) throws XPathException {

        AtomicType t0 = a0.getItemType();
        AtomicType t1 = a1.getItemType();

        if (backwardsCompatible) {
            // If either operand is a number, convert both operands to xs:double using
            // the rules of the number() function, and compare them

            if (a0 instanceof NumericValue || a1 instanceof NumericValue) {
                DoubleValue v0 = NumberFn.convert(a0);
                DoubleValue v1 = NumberFn.convert(a1);
                return ValueComparison.compare(v0, operator, v1, comparer, false);
            }

            // If either operand is a string, or if both are untyped atomic, convert
            // both operands to strings and compare them

            if (t0.equals(AtomicType.STRING) || t1.equals(AtomicType.STRING) ||
                    (t0.equals(AtomicType.UNTYPED_ATOMIC) && t1.equals(AtomicType.UNTYPED_ATOMIC))) {
                StringValue s0 = (StringValue)a0.convert(AtomicType.STRING).asAtomic();
                StringValue s1 = (StringValue)a1.convert(AtomicType.STRING).asAtomic();
                return ValueComparison.compare(s0, operator, s1, comparer, false);
            }
        }

        // If either operand is untyped atomic,
        // convert it to the type of the other operand, and compare

        if (t0.equals(AtomicType.UNTYPED_ATOMIC)) {
            a0 = a0.convert(t1).asAtomic();
        }

        if (t1.equals(AtomicType.UNTYPED_ATOMIC)) {
            a1 = a1.convert(t0).asAtomic();
        }

        return ValueComparison.compare(a0, operator, a1, comparer, false);
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
     */

    public ItemType getItemType() {
        return AtomicType.BOOLEAN;
    }

    /**
    * Return the singleton form of the comparison operator, e.g. FEQ for EQUALS, FGT for GT
     * @param op the general comparison operator, for example Token.EQUALS
     * @return the corresponding value comparison operator, for example Token.FEQ
    */

    private static int getSingletonOperator(int op) {
        switch (op) {
            case Token.EQUALS:
                return Token.FEQ;
            case Token.GE:
                return Token.FGE;
            case Token.NE:
                return Token.FNE;
            case Token.LT:
                return Token.FLT;
            case Token.GT:
                return Token.FGT;
            case Token.LE:
                return Token.FLE;
            default:
                return op;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
