package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.ForEach;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;

/**
 * An UntypedAtomicConverter is an expression that converts any untypedAtomic items in
 * a sequence to a specified type
 */

public final class UntypedAtomicConverter extends UnaryExpression {

    private AtomicType requiredItemType;
    private boolean allConverted;
    private boolean singleton = false;
    private RoleLocator role;

    /**
     * Constructor
     *
     * @param sequence         this must be a sequence of atomic values. This is not checked; a ClassCastException
     *                         will occur if the precondition is not satisfied.
     * @param requiredItemType the item type to which untypedAtomic items in the sequence should be converted,
     *                         using the rules for "cast as".
     * @param allConverted     true if the result of this expression is a sequence in which all items
     *                         belong to the required type
     * @param role             Diagnostic information for use if conversion fails
     */

    public UntypedAtomicConverter(Expression sequence, AtomicType requiredItemType, boolean allConverted, RoleLocator role) {
        super(sequence);
        this.requiredItemType = requiredItemType;
        this.allConverted = allConverted;
        this.role = role;
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
     * Get the item type to which untyped atomic items must be converted
     * @return the required item type
     */

    public ItemType getRequiredItemType() {
        return requiredItemType;
    }

    /**
     * Determine whether all items are to be converted, or only the subset that are untypedAtomic
     * @return true if all items are to be converted
     */

    public boolean areAllItemsConverted() {
        return allConverted;
    }

    /**
     * Determine the data type of the items returned by the expression
     *
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        ItemType it = operand.getItemType(th);
        singleton = it.isAtomicType() && !Cardinality.allowsMany(operand.getCardinality());
        if (allConverted) {
            return requiredItemType;
        } else {
            return Type.getCommonSuperType(requiredItemType, operand.getItemType(th), th);
        }
    }

    public int computeCardinality() {
        if (singleton) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return super.computeCardinality();
        }
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (allConverted && requiredItemType == BuiltInAtomicType.QNAME) {
            typeError("Cannot convert untypedAtomic values to QNames", "XPTY0004", null);
        }
        operand = visitor.typeCheck(operand, contextItemType);
        if (operand instanceof Literal) {
            return Literal.makeLiteral(
                    ((Value)SequenceExtent.makeSequenceExtent(
                            iterate(visitor.getStaticContext().makeEarlyEvaluationContext()))).reduce());
        }
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        ItemType type = operand.getItemType(th);
        if (type instanceof NodeTest) {
            return this;
        }
        singleton = type.isAtomicType() && !Cardinality.allowsMany(operand.getCardinality());
                
        // If we're atomizing a node that always returns an untyped atomic value, and then converting
        // the untyped atomic value to a string, then we might as well take the string value of the node
        if (operand instanceof Atomizer &&
                type.equals(BuiltInAtomicType.UNTYPED_ATOMIC) &&
                requiredItemType == BuiltInAtomicType.STRING &&
                ((Atomizer)operand).getBaseExpression().getItemType(th) instanceof NodeTest) {
            Expression nodeExp = ((Atomizer)operand).getBaseExpression();
            if (nodeExp.getCardinality() != StaticProperty.EXACTLY_ONE) {
                // TODO: Saxon 9.2 as issued was converting to a call to string() when the
                // expected type is "xs:string?", which is fine for functions that treat an empty
                // sequence like a zero-length string, but fails for example on resolve-QName()
                // which treats them differently. It would be good to revert to the string() call
                // in all cases: or perhaps to a variant of string() that maps () to (). This would
                // enable further optimizations.
                SystemFunction fn = (SystemFunction)SystemFunction.makeSystemFunction(
                        "string", new Expression[]{new ContextItemExpression()});
                fn.setContainer(getContainer());
                ForEach map = new ForEach(nodeExp, fn, false);
                map.setContainer(getContainer());
                return map;
            } else {
                SystemFunction fn = (SystemFunction)SystemFunction.makeSystemFunction(
                        "string", new Expression[]{nodeExp});
                fn.setContainer(getContainer());
                return fn;
            }
        }
        if (type.equals(BuiltInAtomicType.ANY_ATOMIC) || type instanceof AnyItemType ||
                type.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            return this;
        }
        // the sequence can't contain any untyped atomic values, so there's no need for a converter
        return operand;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
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
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // If the underlying expression is casting to xs:untypedAtomic, there's scope for a short-circuit
        // (This happens when xsl:value-of is used unnecessarily)
        if (operand instanceof CastExpression) {
            ItemType it = ((CastExpression)operand).getTargetType();
            if (th.isSubType(it, BuiltInAtomicType.UNTYPED_ATOMIC)) {
                Expression e = ((CastExpression)operand).getBaseExpression();
                ItemType et = e.getItemType(th);
                if (et instanceof AtomicType && th.isSubType(et, requiredItemType)) {
                    return e;
                }
            }
        }
        return this;
    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE | StaticProperty.NOT_UNTYPED;
    }

    /**
     * Iterate over the sequence of values
     */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return new ItemMappingIterator(base, getMappingFunction(context), true);
    }

    /**
     * Get the mapping function that converts untyped atomic values to the required type
     * @param context  the dynamic evaluation context for the conversion
     * @return the mapping function
     */

    public ItemMappingFunction getMappingFunction(final XPathContext context) {
        return new ItemMappingFunction() {
            public Item mapItem(Item item) throws XPathException {
                if (item instanceof UntypedAtomicValue) {
                    ConversionResult val = ((UntypedAtomicValue)item).convert(requiredItemType, true);
                    if (val instanceof ValidationFailure) {
                        String msg = role.composeRequiredMessage(requiredItemType, context.getNamePool());
                        msg += ". " + ((ValidationFailure)val).getMessage();
                        XPathException err = new XPathException(msg);
                        err.setErrorCode(role.getErrorCode());
                        err.setLocator(UntypedAtomicConverter.this.getSourceLocator());
                        throw err;
                    }
                    return (AtomicValue)val;
                } else {
                    return item;
                }
            }
        };
    }

    /**
     * Evaluate as an Item. This should only be called if the UntypedAtomicConverter has cardinality zero-or-one
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item == null) {
            return null;
        }
        if (item instanceof UntypedAtomicValue) {
            ConversionResult val = ((UntypedAtomicValue)item).convert(requiredItemType, true);
            if (val instanceof ValidationFailure) {
                String msg = role.composeRequiredMessage(requiredItemType, context.getNamePool());
                msg += ". " + ((ValidationFailure)val).getMessage();
                XPathException err = new XPathException(msg);
                err.setErrorCode(role.getErrorCode());
                err.setLocator(UntypedAtomicConverter.this.getSourceLocator());
                throw err;
            } else {
                return (AtomicValue)val;
            }
        } else {
            return item;
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.