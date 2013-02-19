package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.functions.BooleanFn;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.SequenceType;

/**
* A QuantifiedExpression tests whether some/all items in a sequence satisfy
* some condition.
*/

public class QuantifiedExpression extends Assignation {

    private int operator;       // Token.SOME or Token.EVERY

    /**
     * Set the operator, either {@link Token#SOME} or {@link Token#EVERY}
     * @param operator the operator
     */

    public void setOperator(int operator) {
        this.operator = operator;
    }

    /**
     * Get the operator, either {@link Token#SOME} or {@link Token#EVERY}
     * @return the operator
     */

    public int getOperator() {
        return operator;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = visitor.typeCheck(sequence, contextItemType);
        if (Literal.isEmptySequence(sequence)) {
            return Literal.makeLiteral(BooleanValue.get(operator != Token.SOME));
        }

        // "some" and "every" have no ordering constraints

        Optimizer opt = visitor.getConfiguration().getOptimizer();
        sequence = ExpressionTool.unsorted(opt, sequence, false);

        SequenceType decl = getRequiredType();
        SequenceType sequenceType = SequenceType.makeSequenceType(decl.getPrimaryType(),
                                             StaticProperty.ALLOWS_ZERO_OR_MORE);
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, getVariableQName(), 0);
        //role.setSourceLocator(this);
        sequence = TypeChecker.strictTypeCheck(
                                sequence, sequenceType, role, visitor.getStaticContext());
        ItemType actualItemType = sequence.getItemType(th);
        refineTypeInformation(actualItemType,
                StaticProperty.EXACTLY_ONE,
                null,
                sequence.getSpecialProperties(), visitor, this);

        //declaration = null;     // let the garbage collector take it

        action = visitor.typeCheck(action, contextItemType);
        XPathException err = TypeChecker.ebvError(action, visitor.getConfiguration().getTypeHierarchy());
        if (err != null) {
            err.setLocator(this.getSourceLocator());
            throw err;
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

        Optimizer opt = visitor.getConfiguration().getOptimizer();

        sequence = visitor.optimize(sequence, contextItemType);
        action = visitor.optimize(action, contextItemType);
        Expression ebv = BooleanFn.rewriteEffectiveBooleanValue(action, visitor, contextItemType);
        if (ebv != null) {
            action = ebv;
            adoptChildExpression(ebv);
        }
        PromotionOffer offer = new PromotionOffer(opt);
        offer.containingExpression = this;
        offer.action = PromotionOffer.RANGE_INDEPENDENT;
        offer.bindingList = new Binding[] {this};
        action = doPromotion(action, offer);
        if (offer.containingExpression instanceof LetExpression) {
            offer.containingExpression =
                    visitor.optimize(visitor.typeCheck(offer.containingExpression, contextItemType), contextItemType);
        }
        Expression e2 = offer.containingExpression;
        if (e2 != this) {
            return e2;
        }

        return this;
    }


    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return child == action;
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
    * Evaluate the expression to return a singleton value
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Get the result as a boolean
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {

        // First create an iteration of the base sequence.

        SequenceIterator base = sequence.iterate(context);

        // Now test to see if some or all of the tests are true. The same
        // logic is used for the SOME and EVERY operators

        final boolean some = (operator==Token.SOME);
        int slot = getLocalSlotNumber();
        while (true) {
            final Item it = base.next();
            if (it == null) {
                break;
            }
            context.setLocalVariable(slot, it);
            if (some == action.effectiveBooleanValue(context)) {
                return some;
            }
        }
        return !some;
    }


    /**
    * Determine the data type of the items returned by the expression
    * @return Type.BOOLEAN
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
	    return BuiltInAtomicType.BOOLEAN;
	}

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     * @return a representation of the expression as a string
     */

    public String toString() {
        return (operator==Token.SOME ? "some" : "every") + " $" + getVariableName() +
                " in " + sequence.toString() + " satisfies " + action.toString();
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.