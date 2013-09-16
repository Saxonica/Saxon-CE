package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Assignation is an abstract superclass for the kinds of expression
* that declare range variables: for, some, and every.
*/

public abstract class Assignation extends Expression implements Binding {

    protected int slotNumber = -999;     // slot number for range variable
                                         // (initialized to ensure a crash if no real slot is allocated)
    protected Expression sequence;       // the expression over which the variable ranges
    protected Expression action;         // the action performed for each value of the variable
    protected StructuredQName variableName;
    protected SequenceType requiredType;

    //protected RangeVariable declaration;



    /**
     * Set the required type (declared type) of the variable
     * @param requiredType the required type
     */
    public void setRequiredType(SequenceType requiredType) {
        this.requiredType = requiredType;
    }

    /**
     * Set the name of the variable
     * @param variableName the name of the variable
     */

    public void setVariableQName(StructuredQName variableName) {
        this.variableName = variableName;
    }


    /**
     * Get the name of the variable
     * @return the variable name, as a QName
     */

    public StructuredQName getVariableQName() {
        return variableName;
    }

    public StructuredQName getObjectName() {
        return variableName;
    }


    /**
     * Get the declared type of the variable
     *
     * @return the declared type
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    /**
    * Get the value of the range variable
    */

    public Sequence evaluateVariable(XPathContext context) throws XPathException {
        return context.evaluateLocalVariable(slotNumber);
    }

    /**
     * Add the "return" or "satisfies" expression, and fix up all references to the
     * range variable that occur within that expression
     * @param action the expression that occurs after the "return" keyword of a "for"
     * expression, the "satisfies" keyword of "some/every", or the ":=" operator of
     * a "let" expression.
     *
     * 
     */

    public void setAction(Expression action) {
        this.action = action;
        adoptChildExpression(action);
    }

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     */

    public final boolean isGlobal() {
        return false;
    }

    /**
     * Get the action expression
     * @return the action expression (introduced by "return" or "satisfies")
     */

    public Expression getAction() {
        return action;
    }

    /**
     * Set the "sequence" expression - the one to which the variable is bound
     * @param sequence the expression to which the variable is bound
     */

    public void setSequence(Expression sequence) {
        this.sequence = sequence;
        adoptChildExpression(sequence);
    }

    /**
     * Get the "sequence" expression - the one to which the variable is bound
     * @return the expression to which the variable is bound
     */

    public Expression getSequence() {
        return sequence;
    }

    /**
    * Set the slot number for the range variable
     * @param nr the slot number to be used
    */

    public void setSlotNumber(int nr) {
        slotNumber = nr;
    }

    /**
    * Simplify the expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        sequence = visitor.simplify(sequence);
        action = visitor.simplify(action);
        return this;
    }


    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            sequence = doPromotion(sequence, offer);
            if (offer.action == PromotionOffer.UNORDERED ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                action = doPromotion(action, offer);
            } else if (offer.action == PromotionOffer.RANGE_INDEPENDENT ||
                    offer.action == PromotionOffer.FOCUS_INDEPENDENT) {
                // Pass the offer to the action expression only if the action isn't dependent on the
                // variable bound by this assignation
                Binding[] savedBindingList = offer.bindingList;
                offer.bindingList = extendBindingList(offer.bindingList);
                action = doPromotion(action, offer);
                offer.bindingList = savedBindingList;
            }
            return this;
        }
    }

    /**
     * Extend an array of variable bindings to include the binding(s) defined in this expression
     * @param in a set of variable bindings
     * @return a set of variable bindings including all those supplied plus this one
     */

    public Binding[] extendBindingList(Binding[] in) {
        Binding[] newBindingList;
        if (in == null) {
            newBindingList = new Binding[1];
        } else {
            newBindingList = new Binding[in.length + 1];
            System.arraycopy(in, 0, newBindingList, 0, in.length);
        }
        newBindingList[newBindingList.length - 1] = this;
        return newBindingList;
    }


    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator<Expression> iterateSubExpressions() {
        return nonNullChildren(sequence, action);
    }

    /**
     * Get the display name of the range variable, for diagnostics only
     * @return the lexical QName of the range variable
    */

    public String getVariableName() {
        if (variableName == null) {
            return "zz:var" + hashCode();
        } else {
            return variableName.getDisplayName();
        }
    }

    /**
     * Refine the type information associated with this variable declaration. This is useful when the
     * type of the variable has not been explicitly declared (which is common); the variable then takes
     * a static type based on the type of the expression to which it is bound. The effect of this call
     * is to update the static expression type for all references to this variable.
     * @param type the inferred item type of the expression to which the variable is bound
     * @param cardinality the inferred cardinality of the expression to which the variable is bound
     * @param constantValue the constant value to which the variable is bound (null if there is no constant value)
     * @param properties other static properties of the expression to which the variable is bound
     * @param visitor an expression visitor to provide context information
     * @param currentExpression the expression that binds the variable
     */

    public void refineTypeInformation(ItemType type, int cardinality,
                                      Value constantValue, int properties,
                                      ExpressionVisitor visitor,
                                      Assignation currentExpression) {
        List<VariableReference> references = new ArrayList<VariableReference>();
        ExpressionTool.gatherVariableReferences(currentExpression.getAction(), this, references);
        for (VariableReference ref : references) {
            ref.refineVariableType(type, cardinality, constantValue, properties, visitor);
            visitor.resetStaticProperties();
        }
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.