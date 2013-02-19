package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.DocumentInstr;
import client.net.sf.saxon.ce.expr.instruct.GlobalVariable;
import client.net.sf.saxon.ce.expr.instruct.TailCall;
import client.net.sf.saxon.ce.expr.instruct.TailCallReturner;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.SequenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * A LetExpression is modelled on the XQuery syntax let $x := expr return expr. This syntax
 * is not available in the surface XPath language, but it is used internally in an optimized
 * expression tree.
 */

public class LetExpression extends Assignation implements TailCallReturner {

    // This integer holds an approximation to the number of times that the declared variable is referenced.
    // The value 1 means there is only one reference and it is not in a loop, which means that the value will
    // not be retained in memory. If there are multiple references or references within a loop, the value will
    // be a small integer > 1. The special value FILTERED indicates that there is a reference within a loop
    // in the form $x[predicate], which indicates that the value should potentially be indexable.  The initial
    // value 2 is for safety; if a LetExpression is optimized without first being typechecked (which happens
    // in the case of optimizer-created variables) then this ensures that no damaging rewrites are done.

    int evaluationMode = ExpressionTool.UNDECIDED;

    /**
     * Create a LetExpression
     */

    public LetExpression() {
        //System.err.println("let");
    }

    /**
     * Set the evaluation mode
     */

    public void setEvaluationMode(int mode) {
        evaluationMode = mode;
    }

    /**
     * Type-check the expression. This also has the side-effect of counting the number of references
     * to the variable (treating references that occur within a loop specially)
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        sequence = visitor.typeCheck(sequence, contextItemType);

        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, getVariableQName(), 0);
        //role.setSourceLocator(this);
        sequence = TypeChecker.strictTypeCheck(
                sequence, requiredType, role, visitor.getStaticContext());
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        final ItemType actualItemType = sequence.getItemType(th);

        refineTypeInformation(actualItemType,
                sequence.getCardinality(),
                (sequence instanceof Literal ? ((Literal) sequence).getValue() : null),
                sequence.getSpecialProperties(), visitor, this);

        refCount = 0;
        action = visitor.typeCheck(action, contextItemType);
        return this;
    }


    /**
     * Determine whether this expression implements its own method for static type checking
     *
     * @return true - this expression has a non-trivial implementation of the staticTypeCheck()
     *         method
     */

    public boolean implementsStaticTypeCheck() {
        return true;
    }

    /**
     * Static type checking for let expressions is delegated to the expression itself,
     * and is performed on the "action" expression, to allow further delegation to the branches
     * of a conditional
     * @param req the required type
     * @param backwardsCompatible true if backwards compatibility mode applies
     * @param role the role of the expression in relation to the required type
     * @param visitor an expression visitor
     * @return the expression after type checking (perhaps augmented with dynamic type checking code)
     * @throws XPathException if failures occur, for example if the static type of one branch of the conditional
     * is incompatible with the required type
     */

    public Expression staticTypeCheck(SequenceType req,
                                             boolean backwardsCompatible,
                                             RoleLocator role, ExpressionVisitor visitor)
    throws XPathException {
        action = TypeChecker.staticTypeCheck(action, req, backwardsCompatible, role, visitor);
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

        StaticContext env = visitor.getStaticContext();
        Optimizer opt = visitor.getConfiguration().getOptimizer();

        // if this is a construct of the form "let $j := EXP return $j" replace it with EXP
        // Remarkably, people do write this, and it can also be produced by previous rewrites
        // Note that type checks will already have been added to the sequence expression

        if (action instanceof VariableReference &&
                ((VariableReference) action).getBinding() == this) {
            Expression e2 = visitor.optimize(sequence, contextItemType);
            return e2;
        }

        /**
         * Unless this has already been done, find and count the references to this variable
         */

        // if this is an XSLT construct of the form <xsl:variable>text</xsl:variable>, try to replace
        // it by <xsl:variable select=""/>. This can be done if all the references to the variable use
        // its value as a string (rather than, say, as a node or as a boolean)
        if (sequence instanceof DocumentInstr && ((DocumentInstr) sequence).isTextOnly()) {
            if (allReferencesAreFlattened()) {
                sequence = ((DocumentInstr) sequence).getStringValueExpression(env);
                requiredType = SequenceType.SINGLE_UNTYPED_ATOMIC;
                adoptChildExpression(sequence);
            }
        }

        // refCount is initialized during the typeCheck() phase
        if (refCount == 0) {
            // variable is not used - no need to evaluate it
            Expression a = visitor.optimize(action, contextItemType);
            ExpressionTool.copyLocationInfo(this, a);
            return a;
        }

        int tries = 0;
        while (tries++ < 5) {
            Expression seq2 = visitor.optimize(sequence, contextItemType);
            if (seq2 == sequence) {
                break;
            }
            sequence = seq2;
            adoptChildExpression(sequence);
            visitor.resetStaticProperties();
        }

        tries = 0;
        while (tries++ < 5) {
            Expression act2 = visitor.optimize(action, contextItemType);
            if (act2 == action) {
                break;
            }
            action = act2;
            adoptChildExpression(action);
            visitor.resetStaticProperties();
        }

        evaluationMode = ExpressionTool.lazyEvaluationMode(sequence);
        return this;
    }

    /**
     * Determine whether all references to this variable are using the value either
     * (a) by atomizing it, or (b) by taking its string value. (This excludes usages
     * such as testing the existence of a node or taking the effective boolean value).
     * @return true if all references are known to atomize (or stringify) the value,
     * false otherwise. The value false may indicate "not known".
     */

    private boolean allReferencesAreFlattened() {
        List references = new ArrayList();
        ExpressionTool.gatherVariableReferences(action, this, references);
        for (int i=references.size()-1; i>=0; i--) {
            VariableReference bref = (VariableReference)references.get(i);
            if (bref.isFlattened()) {
                // OK, it's a string context
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Iterate over the result of the expression to return a sequence of items
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        return let.action.iterate(context);
    }


    /**
     * Evaluate the variable.
     * @param context the dynamic evaluation context
     * @return the result of evaluating the expression that is bound to the variable
     */

    protected ValueRepresentation eval(XPathContext context) throws XPathException {
        if (evaluationMode == ExpressionTool.UNDECIDED) {
            evaluationMode = ExpressionTool.lazyEvaluationMode(sequence);
        }
        return ExpressionTool.evaluate(sequence, evaluationMode, context, 10);
    }

    /**
     * Evaluate the expression as a singleton
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        return let.action.evaluateItem(context);
    }

     /**
     * Process this expression as an instruction, writing results to the current
     * outputter
     */

    public void process(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        let.action.process(context);
    }


    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @param th the type hierarchy cache
     * @return one of the values Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
     */

    public ItemType getItemType(TypeHierarchy th) {
        return action.getItemType(th);
    }

    /**
     * Determine the static cardinality of the expression
     */

    public int computeCardinality() {
        return action.getCardinality();
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int props = action.getSpecialProperties();
        int seqProps = sequence.getSpecialProperties();
        if ((seqProps & StaticProperty.NON_CREATIVE) == 0) {
            props &= ~StaticProperty.NON_CREATIVE;
        }
        return props;
    }

    /**
     * Mark tail function calls
     */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        return ExpressionTool.markTailFunctionCalls(action, qName, arity);
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            // pass the offer on to the sequence expression
            Expression seq2 = doPromotion(sequence, offer);
            if (seq2 != sequence) {
                // if we've extracted a global variable, it may need to be marked indexable
                if (seq2 instanceof VariableReference) {
                    Binding b = ((VariableReference)seq2).getBinding();
                    if (b instanceof GlobalVariable) {
                        ((GlobalVariable)b).setReferenceCount(refCount < 10 ? 10 : refCount);
                    }
                }
                sequence = seq2;
            }
            if (offer.action == PromotionOffer.UNORDERED ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                action = doPromotion(action, offer);
            } else if (offer.action == PromotionOffer.RANGE_INDEPENDENT ||
                    offer.action == PromotionOffer.FOCUS_INDEPENDENT) {
                // Pass the offer to the action expression after adding the variable bound by this let expression,
                // so that a subexpression must depend on neither variable if it is to be promoted
                Binding[] savedBindingList = offer.bindingList;
                offer.bindingList = extendBindingList(offer.bindingList);
                action = doPromotion(action, offer);
                offer.bindingList = savedBindingList;
            }
            return this;
        }
    }


    /**
     * ProcessLeavingTail: called to do the real work of this instruction.
     * The results of the instruction are written
     * to the current Receiver, which can be obtained via the Controller.
     *
     * @param context The dynamic context of the transformation, giving access to the current node,
     *                the current variables, etc.
     * @return null if the instruction has completed execution; or a TailCall indicating
     *         a function call or template call that is delegated to the caller, to be made after the stack has
     *         been unwound so as to save stack space.
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            ValueRepresentation val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.action instanceof LetExpression) {
                let = (LetExpression) let.action;
            } else {
                break;
            }
        }
        if (let.action instanceof TailCallReturner) {
            return ((TailCallReturner) let.action).processLeavingTail(context);
        } else {
            let.action.process(context);
            return null;
        }
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     * @return a representation of the expression as a string
     */

    public String toString() {
        return "let $" + getVariableName() + " := " + sequence.toString() + " return " + action.toString();
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.