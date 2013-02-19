package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.IntegerValue;
import client.net.sf.saxon.ce.value.SequenceType;


/**
* A ForExpression maps an expression over a sequence.
* This version works with range variables, it doesn't change the context information
*/

public class ForExpression extends Assignation {

    int actionCardinality = StaticProperty.ALLOWS_MANY;

    /**
     * Create a "for" expression (for $x at $p in SEQUENCE return ACTION)
     */

    public ForExpression() {
    }

    /**
     * Set the slot number for the range variable
     * @param nr the slot number allocated to the range variable on the local stack frame.
     * This implicitly allocates the next slot number to the position variable if there is one.
    */

    public void setSlotNumber(int nr) {
        super.setSlotNumber(nr);
    }

    /**
     * Get the number of slots required.
     * @return normally 1, except for a FOR expression with an AT clause, where it is 2.
     */

    public int getRequiredSlots() {
        return 1;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = visitor.typeCheck(sequence, contextItemType);
        if (Literal.isEmptySequence(sequence)) {
            return sequence;
        }

        if (requiredType != null) {
            // if declaration is null, we've already done the type checking in a previous pass
            final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            SequenceType decl = requiredType;
            SequenceType sequenceType = SequenceType.makeSequenceType(
                    decl.getPrimaryType(), StaticProperty.ALLOWS_ZERO_OR_MORE);
            RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, variableName, 0
            );
            //role.setSourceLocator(this);
            sequence = TypeChecker.strictTypeCheck(
                                    sequence, sequenceType, role, visitor.getStaticContext());
            ItemType actualItemType = sequence.getItemType(th);
            refineTypeInformation(actualItemType,
                    getRangeVariableCardinality(),
                    null,
                    sequence.getSpecialProperties(), visitor, this);
        }

        action = visitor.typeCheck(action, contextItemType);
        if (Literal.isEmptySequence(action)) {
            return action;
        }
        actionCardinality = action.getCardinality();
        return this;
    }

    /**
     * Get the cardinality of the range variable
     * @return the cardinality of the range variable (StaticProperty.EXACTLY_ONE). Can be overridden
     * in a subclass
     */

    protected int getRangeVariableCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        // Try to promote any WHERE clause appearing immediately within the FOR expression

        if (Choose.isSingleBranchChoice(action)) {
            Expression act2 = visitor.optimize(action, contextItemType);
            if (act2 != action) {
                action = act2;
                adoptChildExpression(action);
                visitor.resetStaticProperties();
            }
        }

        Expression seq2 = visitor.optimize(sequence, contextItemType);
        if (seq2 != sequence) {
            sequence = seq2;
            adoptChildExpression(sequence);
            visitor.resetStaticProperties();
            return optimize(visitor, contextItemType);
        }

        if (Literal.isEmptySequence(sequence)) {
            return sequence;
        }

        Expression act2 = visitor.optimize(action, contextItemType);
        if (act2 != action) {
            action = act2;
            adoptChildExpression(action);
            visitor.resetStaticProperties();
            // it's now worth re-attempting the "where" clause optimizations
            return optimize(visitor, contextItemType);
        }

        if (Literal.isEmptySequence(action)) {
            return action;
        }

        Expression e2 = extractLoopInvariants(visitor, contextItemType);
        if (e2 != null && e2 != this) {
            return visitor.optimize(e2, contextItemType);
        }

        // Simplify an expression of the form "for $b in a/b/c return $b/d".
        // (XQuery users seem to write these a lot!)

        if (sequence instanceof SlashExpression && action instanceof SlashExpression) {
            SlashExpression path2 = (SlashExpression)action;
            Expression start2 = path2.getControllingExpression();
            Expression step2 = path2.getControlledExpression();
            if (start2 instanceof VariableReference && ((VariableReference)start2).getBinding() == this &&
                    ExpressionTool.getReferenceCount(action, this, false) == 1 &&
                    ((step2.getDependencies() & (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST)) == 0)) {
                Expression newPath = new SlashExpression(sequence, path2.getControlledExpression());
                ExpressionTool.copyLocationInfo(this, newPath);
                newPath = visitor.typeCheck(visitor.simplify(newPath), contextItemType);
                if (newPath instanceof SlashExpression) {
                    // if not, it has been wrapped in a DocumentSorter or Reverser, which makes it ineligible.
                    // see test qxmp299, where this condition isn't satisfied
                    return visitor.optimize(newPath, contextItemType);
                }
            }
        }

        // Simplify an expression of the form "for $x in EXPR return $x". These sometimes
        // arise as a result of previous optimization steps.

        if (action instanceof VariableReference && ((VariableReference)action).getBinding() == this) {
            return sequence;
        }

        // If the cardinality of the sequence is exactly one, rewrite as a LET expression

        if (sequence.getCardinality() == StaticProperty.EXACTLY_ONE) {
            LetExpression let = new LetExpression();
            let.setVariableQName(variableName);
            let.setRequiredType(SequenceType.makeSequenceType(
                    sequence.getItemType(visitor.getConfiguration().getTypeHierarchy()),
                    StaticProperty.EXACTLY_ONE));
            let.setSequence(sequence);
            let.setAction(action);
            let.setSlotNumber(slotNumber);
            ExpressionTool.rebindVariableReferences(action, this, let);
            return let.optimize(visitor, contextItemType);
        }

        //declaration = null;     // let the garbage collector take it
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
     * Extract subexpressions in the action part that don't depend on the range variable
     * @param visitor the expression visitor
     * @param contextItemType the item type of the context item
     * @return the optimized expression if it has changed, or null if no optimization was possible
     */

    private Expression extractLoopInvariants(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        // Extract subexpressions that don't depend on the range variable or the position variable
        // If a subexpression is (or might be) creative, this is, if it creates new nodes, we don't
        // extract it from the loop, but we do extract its non-creative subexpressions

        PromotionOffer offer = new PromotionOffer(visitor.getConfiguration().getOptimizer());
        offer.containingExpression = this;
        offer.action = PromotionOffer.RANGE_INDEPENDENT;
        offer.bindingList = new Binding[] {this};
        action = doPromotion(action, offer);
        if (offer.containingExpression instanceof LetExpression) {
            // a subexpression has been promoted
            //offer.containingExpression.setParentExpression(container);
            // try again: there may be further subexpressions to promote
            offer.containingExpression = visitor.optimize(offer.containingExpression, contextItemType);
        }
        return offer.containingExpression;

    }

    /**
     * Mark tail function calls: only possible if the for expression iterates zero or one times.
     * (This arises in XSLT/XPath, which does not have a LET expression, so FOR gets used instead)
     */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        if (!Cardinality.allowsMany(sequence.getCardinality())) {
            return ExpressionTool.markTailFunctionCalls(action, qName, arity);
        } else {
            return 0;
        }
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD | PROCESS_METHOD;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // First create an iteration of the base sequence.

        // Then create a MappingIterator which applies a mapping function to each
        // item in the base sequence. The mapping function is essentially the "return"
        // expression, wrapped in a MappingAction object that is responsible also for
        // setting the range variable at each step.

        SequenceIterator base = sequence.iterate(context);
        int pslot = -1;
        MappingAction map = new MappingAction(context, getLocalSlotNumber(), pslot, action);
        switch (actionCardinality) {
            case StaticProperty.EXACTLY_ONE:
                return new ItemMappingIterator(base, map, true);
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return new ItemMappingIterator(base, map, false);
            default:
                return new MappingIterator(base, map);
        }
    }

    /**
     * Process this expression as an instruction, writing results to the current
     * outputter
     */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = sequence.iterate(context);
        int position = 1;
        int slot = getLocalSlotNumber();
        int pslot = -1;
        while (true) {
            Item item = iter.next();
            if (item == null) break;
            context.setLocalVariable(slot, item);
            if (pslot >= 0) {
                context.setLocalVariable(pslot, IntegerValue.makeIntegerValue(position++));
            }
            action.process(context);
        }
    }


    /**
     * Determine the data type of the items returned by the expression, if possible
     * @return one of the values Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     * or Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
	    return action.getItemType(th);
	}

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        int c1 = sequence.getCardinality();
        int c2 = action.getCardinality();
        return Cardinality.multiply(c1, c2);
	}

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     * @return a representation of the expression as a string
     */

    public String toString() {
        return "for $" + getVariableName() +
                " in " + (sequence==null ? "(...)" : sequence.toString()) +
                " return " + (action==null ? "(...)" : action.toString());
    }


    /**
     * The MappingAction represents the action to be taken for each item in the
     * source sequence. It acts as the MappingFunction for the mapping iterator, and
     * also as the Binding of the position variable (at $n) in XQuery, if used.
     */

    protected static class MappingAction implements MappingFunction, ItemMappingFunction, StatefulMappingFunction {

        private XPathContext context;
        private int slotNumber;
        private Expression action;
        private int pslot = -1;
        private int position = 1;

        public MappingAction(XPathContext context,
                                int slotNumber,
                                int pslot,
                                Expression action) {
            this.context = context;
            this.slotNumber = slotNumber;
            this.pslot = pslot;
            this.action = action;
        }

        public SequenceIterator map(Item item) throws XPathException {
            context.setLocalVariable(slotNumber, item);
            if (pslot >= 0) {
                context.setLocalVariable(pslot, IntegerValue.makeIntegerValue(position++));
            }
            return action.iterate(context);
        }

        public Item mapItem(Item item) throws XPathException {
            context.setLocalVariable(slotNumber, item);
            if (pslot >= 0) {
                context.setLocalVariable(pslot, IntegerValue.makeIntegerValue(position++));
            }
            return action.evaluateItem(context);
        }

        public StatefulMappingFunction getAnother() {
            // Create a copy of the stack frame, so that changes made to local variables by the cloned
            // iterator are not seen by the original iterator
            XPathContextMajor c2 = context.newContext();
            StackFrame oldstack = context.getStackFrame();
            ValueRepresentation[] vars = oldstack.getStackFrameValues();
            ValueRepresentation[] newvars = new ValueRepresentation[vars.length];
            System.arraycopy(vars, 0, newvars, 0, vars.length);
            c2.setStackFrame(oldstack.getStackFrameMap(), newvars);
            return new MappingAction(c2, slotNumber, pslot, action);
        }
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.