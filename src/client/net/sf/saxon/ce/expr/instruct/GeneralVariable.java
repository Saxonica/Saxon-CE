package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.SequenceType;

import java.util.Collections;
import java.util.Iterator;

/**
* This class defines common behaviour across xsl:variable, xsl:param, and xsl:with-param;
*/

public abstract class GeneralVariable extends Instruction implements Binding {


    private static final int REQUIRED = 4;
    private static final int TUNNEL = 8;
    private static final int IMPLICITLY_REQUIRED = 16;  // a parameter that is required because the fallback
                                                        // value is not a valid instance of the type.

    private byte properties = 0;
    Expression select = null;
    protected StructuredQName variableQName;
    SequenceType requiredType;
    protected int slotNumber;
    protected int referenceCount = 10;
    protected int evaluationMode = ExpressionTool.UNDECIDED;

    /**
     * Create a general variable
     */

    public GeneralVariable() {}

    /**
     * Initialize the properties of the variable
     * @param select the expression to which the variable is bound
     * @param qName the name of the variable
     */

    public void init(Expression select, StructuredQName qName) {
        this.select = select;
        variableQName = qName;
        adoptChildExpression(select);
    }

    /**
     * Set the expression to which this variable is bound
     * @param select the initializing expression
     */

    public void setSelectExpression(Expression select) {
        this.select = select;
        evaluationMode = ExpressionTool.UNDECIDED;
        adoptChildExpression(select);
    }

    /**
     * Get the expression to which this variable is bound
     * @return the initializing expression
     */

    public Expression getSelectExpression() {
        return select;
    }

    /**
     * Set the required type of this variable
     * @param required the required type
     */

    public void setRequiredType(SequenceType required) {
        requiredType = required;
    }

    /**
     * Get the required type of this variable
     * @return the required type
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * Indicate that this variable represents a required parameter
     * @param requiredParam true if this is a required parameter
     */

    public void setRequiredParam(boolean requiredParam) {
        if (requiredParam) {
            properties |= REQUIRED;
        } else {
            properties &= ~REQUIRED;
        }
    }

    /**
     * Indicate that this variable represents a parameter that is implicitly required (because there is no
     * usable default value)
     * @param requiredParam true if this is an implicitly required parameter
     */

    public void setImplicitlyRequiredParam(boolean requiredParam) {
        if (requiredParam) {
            properties |= IMPLICITLY_REQUIRED;
        } else {
            properties &= ~IMPLICITLY_REQUIRED;
        }
    }

    /**
     * Indicate whether this variable represents a tunnel parameter
     * @param tunnel true if this is a tunnel parameter
     */

    public void setTunnel(boolean tunnel) {
        if (tunnel) {
            properties |= TUNNEL;
        } else {
            properties &= ~TUNNEL;
        }
    }

    /**
     * Set the nominal number of references to this variable
     * @param refCount the nominal number of references
     */

    public void setReferenceCount(int refCount) {
        referenceCount = refCount;
    }

    /**
     * Get the type of the result of this instruction. An xsl:variable instruction returns nothing, so the
     * type is empty.
     * @return the empty type.
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return EmptySequenceTest.getInstance();
    }

    /**
     * Get the cardinality of the result of this instruction. An xsl:variable instruction returns nothing, so the
     * type is empty.
     * @return the empty cardinality.
     */

    public int getCardinality() {
        return StaticProperty.EMPTY;
    }

    public boolean isGlobal() {
        return false;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    /**
     * Ask whether this variable represents a required parameter
     * @return true if this is a required parameter
     */

    public final boolean isRequiredParam() {
        return (properties & REQUIRED) != 0;
    }

    /**
     * Ask whether this variable represents a parameter that is implicitly required, because there is no usable
     * default value
     * @return true if this variable is an implicitly required parameter
     */

    public final boolean isImplicitlyRequiredParam() {
        return (properties & IMPLICITLY_REQUIRED) != 0;
    }

    /**
     * Ask whether this variable represents a tunnel parameter
     * @return true if this is a tunnel parameter
     */

    public final boolean isTunnelParam() {
        return (properties & TUNNEL) != 0;
    }

    /**
     * Get the name of this instruction (that is xsl:variable, xsl:param etc) for diagnostics
     * @return the name of this instruction, as a name pool name code
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_VARIABLE;
    }

    /**
     * Simplify this expression
     * @param visitor an expression
     * @return the simplified expression
     * @throws XPathException
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        if (select != null) {
            select = visitor.simplify(select);
        }
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (select != null) {
            select = visitor.typeCheck(select, contextItemType);
            adoptChildExpression(select);
        }
        checkAgainstRequiredType(visitor);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (select != null) {
            select = visitor.optimize(select, contextItemType);
            adoptChildExpression(select);
            evaluationMode = computeEvaluationMode();
        }
        return this;
    }

    private int computeEvaluationMode() {
        return ExpressionTool.lazyEvaluationMode(select);
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("GeneralVariable.copy()");
    }

    /**
     * Check the select expression against the required type.
     * @param visitor an expression visitor
     * @throws XPathException
     */

    private void checkAgainstRequiredType(ExpressionVisitor visitor)
    throws XPathException {
        // Note, in some cases we are doing this twice.
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, variableQName, 0);
        //role.setSourceLocator(this);
        SequenceType r = requiredType;
        if (r != null && select != null) {
            // check that the expression is consistent with the required type
            select = TypeChecker.staticTypeCheck(select, requiredType, false, role, visitor);
        }
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        process(context);
        return null;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation relies on the process() method: it
     * "pushes" the results of the instruction to a sequence in memory, and then
     * iterates over this in-memory sequence.
     *
     * In principle instructions should implement a pipelined iterate() method that
     * avoids the overhead of intermediate storage.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        evaluateItem(context);
        return EmptyIterator.getInstance();
    }

    /**
     * Evaluate the variable. That is,
     * get the value of the select expression if present or the content
     * of the element otherwise, either as a tree or as a sequence
     * @param context the XPath dynamic context
     * @return the result of evaluating the variable
    */

    public ValueRepresentation getSelectValue(XPathContext context) throws XPathException {
        if (select==null) {
            throw new AssertionError("*** No select expression!!");
            // The value of the variable is a sequence of nodes and/or atomic values

        } else {
            // There is a select attribute: do a lazy evaluation of the expression,
            // which will already contain any code to force conversion to the required type.

            return ExpressionTool.evaluate(select, evaluationMode, context, referenceCount);

        }
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (select != null) {
            Expression e2 = doPromotion(select, offer);
            if (e2 != select) {
                select = e2;
                evaluationMode = computeEvaluationMode();
            }
        }
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */


    public Iterator<Expression> iterateSubExpressions() {
        if (select != null) {
            return monoIterator(select);
        } else {
            return Collections.EMPTY_LIST.iterator();
        }
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        return found;
    }


    /**
     * Get the slot number allocated to this variable
     * @return the slot number, that is the position allocated to the variable on its stack frame
     */

    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Set the slot number of this variable
     * @param s the slot number, that is, the position allocated to this variable on its stack frame
     */

    public void setSlotNumber(int s) {
        slotNumber = s;
    }

    /**
     * Set the name of the variable
     * @param s the name of the variable (a QName)
     */

    public void setVariableQName(StructuredQName s) {
        variableQName = s;
    }

    /**
     * Get the name of this variable
     * @return the name of this variable (a QName)
     */

    public StructuredQName getVariableQName() {
        return variableQName;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
