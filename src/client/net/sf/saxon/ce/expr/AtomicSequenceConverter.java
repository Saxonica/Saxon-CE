package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.SequenceExtent;

/**
* An AtomicSequenceConverter is an expression that performs a cast on each member of
* a supplied sequence
*/

public final class AtomicSequenceConverter extends UnaryExpression {

    private AtomicType requiredItemType;

    /**
    * Constructor
    * @param sequence this must be a sequence of atomic values. This is not checked; a ClassCastException
    * will occur if the precondition is not satisfied.
    * @param requiredItemType the item type to which all items in the sequence should be converted,
    * using the rules for "cast as".
    */

    public AtomicSequenceConverter(Expression sequence, AtomicType requiredItemType) {
        super(sequence);
        this.requiredItemType = requiredItemType;
        ExpressionTool.copyLocationInfo(sequence, this);
    }


    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (operand instanceof Literal) {
            Sequence val = SequenceExtent.makeSequenceExtent(
                    iterate(new EarlyEvaluationContext(visitor.getConfiguration())));
            return Literal.makeLiteral(val);
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        final TypeHierarchy th = TypeHierarchy.getInstance();
        if (th.isSubType(operand.getItemType(), requiredItemType)) {
            return operand;
        } else if (!Cardinality.allowsMany(operand.getCardinality())) {
            CastExpression cast = new CastExpression(operand, requiredItemType,
                                        (operand.getCardinality() & StaticProperty.ALLOWS_ZERO) != 0);
            ExpressionTool.copyLocationInfo(this, cast);
            return cast;
        } else {
            return this;
        }
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
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemMappingFunction converter = new ItemMappingFunction() {
            public Item mapItem(Item item) throws XPathException {
                return ((AtomicValue)item).convert(requiredItemType).asAtomic();
            }
        };
        return new ItemMappingIterator(base, converter, true);
    }


    /**
    * Evaluate as an Item. This should only be called if the AtomicSequenceConverter has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        return ((AtomicValue)item).convert(
                requiredItemType).asAtomic();
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     */

	public ItemType getItemType() {
	    return requiredItemType;
	}

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        return operand.getCardinality();
	}

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredItemType == ((AtomicSequenceConverter)other).requiredItemType;
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ requiredItemType.hashCode();
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.