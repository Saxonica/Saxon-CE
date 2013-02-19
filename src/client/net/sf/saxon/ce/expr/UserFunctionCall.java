package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.instruct.UserFunction;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.*;


/**
 * This class represents a call to a user-defined function in the stylesheet or query.
*/

public class UserFunctionCall extends FunctionCall {

    private SequenceType staticType;
    private UserFunction function;
    private boolean tailCall = false;
        // indicates only that this is a tail call, not necessarily a recursive tail call
    private int[] argumentEvaluationModes = null;

    /**
     * Create a function call to a user-written function in a query or stylesheet
     */

    public UserFunctionCall() {}

    /**
     * Set the static type
     * @param type the static type of the result of the function call
     */

    public void setStaticType(SequenceType type) {
        staticType = type;
    }

    /**
     * Create the reference to the function to be called
     * @param compiledFunction the function being called
     */

    public void setFunction(UserFunction compiledFunction) {
        function = compiledFunction;
    }

    /**
     * Check the function call against the declared function signature
     * @param compiledFunction the function being called
     * @param visitor an expression visitor
     */

    public void checkFunctionCall(UserFunction compiledFunction,
                            ExpressionVisitor visitor) throws XPathException {
        int n = compiledFunction.getNumberOfArguments();
        for (int i=0; i<n; i++) {
            RoleLocator role = new RoleLocator(
                    RoleLocator.FUNCTION, compiledFunction.getFunctionName(), i);
            role.setErrorCode("XTTE0790");
            argument[i] = TypeChecker.staticTypeCheck(
                                argument[i],
                                compiledFunction.getArgumentType(i),
                                false,
                                role, visitor);
        }
    }


    /**
    * Method called during the type checking phase
    */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        // these checks are now done in setFunction(), at the time when the function
        // call is bound to an actual function
    }

   /**
     * Get the qualified of the function being called
     * @return the qualified name
     */

    public final StructuredQName getFunctionName() {
        StructuredQName n = super.getFunctionName();
        if (n == null) {
            return function.getFunctionName();
        } else {
            return n;
        }
    }

    /**
    * Pre-evaluate a function at compile time. This version of the method suppresses
    * early evaluation by doing nothing.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Determine the data type of the expression, if possible
    * @return Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (staticType == null) {
            // the actual type is not known yet, so we return an approximation
            return AnyItemType.getInstance();
        } else {
            return staticType.getPrimaryType();
        }
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_USER_FUNCTIONS;
    }

    /**
    * Determine the cardinality of the result
    */

    public int computeCardinality() {
        if (staticType == null) {
            // the actual type is not known yet, so we return an approximation
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return staticType.getCardinality();
        }
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(visitor, contextItemType);
        if (function != null) {
            if (e == this) {
                computeArgumentEvaluationModes();
            }
            if (staticType == SequenceType.ANY_SEQUENCE) {
                // try to get a better type
                staticType = function.getResultType(visitor.getConfiguration().getTypeHierarchy());
            }
        }
        return e;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this && function != null) {
            computeArgumentEvaluationModes();
        }
        return e;
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            boolean changed = false;
            if (offer.action != PromotionOffer.UNORDERED) {
                for (int i=0; i<argument.length; i++) {
                    Expression a2 = doPromotion(argument[i], offer);
                    changed |= (a2 != argument[i]);
                    argument[i] = a2;
                }
            }
            if (changed && function != null) {
                computeArgumentEvaluationModes();
            }
            return this;
        }
    }

    /**
     * Compute the evaluation mode of each argument
     */

    public void computeArgumentEvaluationModes() {
        argumentEvaluationModes = new int[argument.length];
        for (int i=0; i<argument.length; i++) {
            int refs = function.getParameterDefinitions()[i].getReferenceCount();
            if (refs == 0) {
                // the argument is never referenced, so don't evaluate it
                argumentEvaluationModes[i] = ExpressionTool.RETURN_EMPTY_SEQUENCE;
            } else if (function.getParameterDefinitions()[i].isIndexedVariable()) {
                argumentEvaluationModes[i] = ExpressionTool.MAKE_INDEXED_VARIABLE;
            } else if ((argument[i].getDependencies() & StaticProperty.DEPENDS_ON_USER_FUNCTIONS) != 0) {
                // if the argument contains a call to a user-defined function, then it might be a recursive call.
                // It's better to evaluate it now, rather than waiting until we are on a new stack frame, as
                // that can blow the stack if done repeatedly. (See test func42)
                argumentEvaluationModes[i] = ExpressionTool.eagerEvaluationMode(argument[i]);
            } else {
                int m = ExpressionTool.lazyEvaluationMode(argument[i]);
                if (m == ExpressionTool.MAKE_CLOSURE && refs > 1) {
                    m = ExpressionTool.MAKE_MEMO_CLOSURE;
                }
                argumentEvaluationModes[i] = m;
            }
        }
    }


    /**
     * Mark tail-recursive calls on stylesheet functions. This marks the function call as tailRecursive if
     * if is a call to the containing function, and in this case it also returns "true" to the caller to indicate
     * that a tail call was found.
    */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        tailCall = true;
        return (getFunctionName().equals(qName) &&
               arity == getNumberOfArguments() ? 2 : 1);
    }

    public int getImplementationMethod() {
        if (Cardinality.allowsMany(getCardinality())) {
            return ITERATE_METHOD | PROCESS_METHOD;
        } else {
            return EVALUATE_METHOD;
        }
    }

    /**
    * Call the function, returning the value as an item. This method will be used
    * only when the cardinality is zero or one. If the function is tail recursive,
    * it returns an Object representing the arguments to the next (recursive) call
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        ValueRepresentation val = callFunction(c);
        return Value.asItem(val);
    }

    /**
    * Call the function, returning an iterator over the results. (But if the function is
    * tail recursive, it returns an iterator over the arguments of the recursive call)
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        ValueRepresentation result = callFunction(c);
        return Value.getIterator(result);
    }


    /**
     * This is the method that actually does the function call
     * @param c the dynamic context
     * @return the result of the function
     * @throws XPathException if dynamic errors occur
     */
    private ValueRepresentation callFunction(XPathContext c) throws XPathException {
        ValueRepresentation[] actualArgs = evaluateArguments(c);

        if (tailCall) {
            ((XPathContextMajor)c).requestTailCall(function, actualArgs);
            return EmptySequence.getInstance();
        }

        XPathContextMajor c2 = c.newCleanContext();
        c2.setTemporaryOutputState(true);
        return function.call(actualArgs, c2);
    }

    /**
     * Process the function call in push mode
     * @param context the XPath dynamic context
     * @throws XPathException
     */

    public void process(XPathContext context) throws XPathException {
        ValueRepresentation[] actualArgs = evaluateArguments(context);
        if (tailCall) {
            ((XPathContextMajor)context).requestTailCall(function, actualArgs);
        } else {
            SequenceReceiver out = context.getReceiver();
            XPathContextMajor c2 = context.newCleanContext();
            c2.setReceiver(out);
            function.process(actualArgs, c2);
        }
    }


    private ValueRepresentation[] evaluateArguments(XPathContext c) throws XPathException {
        int numArgs = argument.length;
        ValueRepresentation[] actualArgs = new ValueRepresentation[numArgs];
        if (argumentEvaluationModes == null) {
            // should have been done at compile time
            computeArgumentEvaluationModes();
        }
        for (int i=0; i<numArgs; i++) {

            int refs = function.getParameterDefinitions()[i].getReferenceCount();
            actualArgs[i] = ExpressionTool.evaluate(argument[i], argumentEvaluationModes[i], c, refs);

            if (actualArgs[i] == null) {
                actualArgs[i] = EmptySequence.getInstance();
            }
            // If the argument has come in as a (non-memo) closure but there are multiple references to it,
            // then we materialize it in memory now. This shouldn't really happen but it does (tour.xq)
            if (refs > 1 && actualArgs[i] instanceof Closure && !(actualArgs[i] instanceof MemoClosure)) {
                actualArgs[i] = ((Closure)actualArgs[i]).reduce();
            }
        }
        return actualArgs;
    }

    /**
     * Call the function dynamically. For this to be possible, the static arguments of the function call
     * must have been set up as {@link SuppliedParameterReference} objects. The actual arguments are placed on the
     * callee's stack, and the type conversion takes place "in situ".
     * @param suppliedArguments the values to be used for the arguments of the function
     * @param context the dynamic evaluation context
     * @return the result of evaluating the function
     */

    public ValueRepresentation dynamicCall(ValueRepresentation[] suppliedArguments, XPathContext context) throws XPathException {
        ValueRepresentation[] convertedArgs = new ValueRepresentation[suppliedArguments.length];
        XPathContextMajor c2 = context.newCleanContext();
        c2.setCaller(context);
        c2.openStackFrame(suppliedArguments.length);
        for (int i=0; i<suppliedArguments.length; i++) {
            c2.setLocalVariable(i, suppliedArguments[i]);
            convertedArgs[i] = ExpressionTool.lazyEvaluate(argument[i], c2, 10);
        }
        XPathContextMajor c3 = c2.newCleanContext();
        return function.call(convertedArgs, c3);
    }

    public StructuredQName getObjectName() {
        return getFunctionName();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
