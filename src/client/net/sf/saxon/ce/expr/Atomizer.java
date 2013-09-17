package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.Block;
import client.net.sf.saxon.ce.expr.instruct.ValueOf;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
* An Atomizer is an expression corresponding essentially to the fn:data() function: it
* maps a sequence by replacing nodes with their typed values
*/

public final class Atomizer extends UnaryExpression  {

    /**
     * Constructor
     * @param sequence the sequence to be atomized
     *  */

    public Atomizer(Expression sequence) {
        super(sequence);
        sequence.setFlattened(true);
    }

    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (operand instanceof Literal) {
            Sequence val = ((Literal)operand).getValue();
            if (val instanceof AtomicValue) {
                return operand;
            }
            SequenceIterator iter = val.iterate();
            while (true) {
                // if all items in the sequence are atomic (they generally will be, since this is
                // done at compile time), then return the sequence
                Item i = iter.next();
                if (i == null) {
                    return operand;
                }
                if (i instanceof NodeInfo) {
                    return this;
                }
            }
        } else if (operand instanceof ValueOf) {
            // XSLT users tend to use ValueOf unnecessarily
            return ((ValueOf)operand).convertToCastAsString();
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        // If the configuration allows typed data, check whether the content type of these particular nodes is untyped
        final TypeHierarchy th = TypeHierarchy.getInstance();
        visitor.resetStaticProperties();
        ItemType operandType = operand.getItemType();
        if (th.isSubType(operandType, AtomicType.ANY_ATOMIC)) {
            return operand;
        }
        operand.setFlattened(true);
        return this;
    }


    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp == this) {
            final TypeHierarchy th = TypeHierarchy.getInstance();
            if (th.isSubType(operand.getItemType(), AtomicType.ANY_ATOMIC)) {
                return operand;
            }
            if (operand instanceof ValueOf) {
                // XSLT users tend to use ValueOf unnecessarily
                return ((ValueOf)operand).convertToCastAsString();
            }
            if (operand instanceof Block) {
                // replace atomize((x,y,z)) by (atomize(x), atomize(y), atomize(z)) as some of the atomizers
                // may prove to be redundant. (Also, it helps streaming)
                Expression[] children = ((Block)operand).getChildren();
                Expression[] atomizedChildren = new Expression[children.length];
                for (int i=0; i<children.length; i++) {
                    atomizedChildren[i] = new Atomizer(children[i]);
                }
                Block newBlock = new Block();
                newBlock.setChildren(atomizedChildren);
                return newBlock.typeCheck(visitor, contextItemType).optimize(visitor, contextItemType);
            }
        }
        return exp;
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

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return getAtomizingIterator(base);
    }

    /**
    * Evaluate as an Item. This should only be called if the Atomizer has cardinality zero-or-one,
    * which will only be the case if the underlying expression has cardinality zero-or-one.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item i = operand.evaluateItem(context);
        return (i==null ? null : i.getTypedValue());
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER. For this class, the
     * result is always an atomic type, but it might be more specific.
     */

	public ItemType getItemType() {
        return getAtomizedItemType(operand, true);
    }

    /**
     * Compute the type that will result from atomizing the result of a given expression
     *
     * @param operand the given expression
     * @param alwaysUntyped true if it is known that nodes will always be untyped
     * @return the item type of the result of evaluating the operand expression, after atomization
     */

    public static ItemType getAtomizedItemType(Expression operand, boolean alwaysUntyped) {
        ItemType in = operand.getItemType();
        if (in.isAtomicType()) {
            return in;
        }
        if (in instanceof NodeTest) {

            if (in instanceof EmptySequenceTest) {
                return in;
            }
            int kinds = ((NodeTest)in).getNodeKindMask();
            if (alwaysUntyped) {
                // Some node-kinds always have a typed value that's a string

                if ((kinds | STRING_KINDS) == STRING_KINDS) {
                    return AtomicType.STRING;
                }
                // Some node-kinds are always untyped atomic; some are untypedAtomic provided that the configuration
                // is untyped

                if ((kinds | UNTYPED_IF_UNTYPED_KINDS) == UNTYPED_IF_UNTYPED_KINDS) {
                    return AtomicType.UNTYPED_ATOMIC;
                }
            } else {
                if ((kinds | UNTYPED_KINDS) == UNTYPED_KINDS) {
                    return AtomicType.UNTYPED_ATOMIC;
                }
            }

            return in.getAtomizedItemType();
        }
	    return AtomicType.ANY_ATOMIC;
	}

    /**
     * Node kinds whose typed value is always a string
     */
    private static final int STRING_KINDS =
            (1<<Type.NAMESPACE) | (1<<Type.COMMENT) | (1<<Type.PROCESSING_INSTRUCTION);

    /**
     * Node kinds whose typed value is always untypedAtomic
     */

    private static final int UNTYPED_KINDS =
            (1<<Type.TEXT) | (1<<Type.DOCUMENT);

    /**
     * Node kinds whose typed value is untypedAtomic if the configuration is untyped
     */

    private static final int UNTYPED_IF_UNTYPED_KINDS =
            (1<<Type.TEXT) | (1<<Type.ELEMENT) | (1<<Type.DOCUMENT) | (1<<Type.ATTRIBUTE);

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        return operand.getCardinality();
	}


    /**
     * Get an iterator that returns the result of atomizing the sequence delivered by the supplied
     * iterator
     * @param base the supplied iterator, the input to atomization
     * @return an iterator that returns atomic values, the result of the atomization
     */

    public static SequenceIterator getAtomizingIterator(SequenceIterator base) throws XPathException {
        ItemMappingFunction imf = new ItemMappingFunction() {
            public Item mapItem(Item item) throws XPathException {
                return item.getTypedValue();
            }
        };
        return new ItemMappingIterator(base, imf);
    }


}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.