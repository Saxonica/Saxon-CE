package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.NoDynamicContextException;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceExtent;

import java.util.Arrays;
import java.util.Iterator;

/**
* Abstract superclass for calls to system-defined and user-defined functions
*/

public abstract class FunctionCall extends Expression {

    /**
     * The name of the function
     */

    private StructuredQName name;

    /**
    * The array of expressions representing the actual parameters
    * to the function call
    */

    protected Expression[] argument;

    /**
     * Set the name of the function being called
     * @param name the name of the function
     */

    public final void setFunctionName(StructuredQName name) {
        this.name = name;
    }

    /**
     * Get the qualified of the function being called
     * @return the qualified name 
     */

    public StructuredQName getFunctionName() {
        return name;
    }

    /**
     * Determine the number of actual arguments supplied in the function call
     * @return the arity (the number of arguments)
     */

    public final int getNumberOfArguments() {
        return argument.length;
    }

    /**
     * Method called by the expression parser when all arguments have been supplied
     * @param args the expressions contained in the argument list of the function call
     */

    public void setArguments(Expression[] args) {
        argument = args;
        for (int a=0; a<args.length; a++) {
            adoptChildExpression(args[a]);
        }
    }

    /**
     * Get the expressions supplied as actual arguments to the function
     * @return the array of expressions supplied in the argument list of the function call
     */

    public Expression[] getArguments() {
        return argument;
    }

    /**
    * Simplify the function call. Default method is to simplify each of the supplied arguments and
    * evaluate the function if all are now known.
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        return simplifyArguments(visitor); 
    }

    /**
    * Simplify the arguments of the function.
    * Called from the simplify() method of each function.
    * @return the result of simplifying the arguments of the expression
     * @param visitor an expression visitor
     */

    protected final Expression simplifyArguments(ExpressionVisitor visitor) throws XPathException {
        for (int i=0; i<argument.length; i++) {
            Expression exp = visitor.simplify(argument[i]);
            if (exp != argument[i]) {
                adoptChildExpression(exp);
                argument[i] = exp;
            }
        }
        return this;
    }

    /**
    * Type-check the expression. This also calls preEvaluate() to evaluate the function
    * if all the arguments are constant; functions that do not require this behavior
    * can override the preEvaluate method.
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        boolean fixed = true;
        for (int i=0; i<argument.length; i++) {
            Expression exp = visitor.typeCheck(argument[i], contextItemType);
            if (exp != argument[i]) {
                adoptChildExpression(exp);
                argument[i] = exp;
            }
            if (!(argument[i] instanceof Literal)) {
                fixed = false;
            }
        }
        checkArguments(visitor);
        if (fixed) {
            try {
                return preEvaluate(visitor);
            } catch (NoDynamicContextException err) {
                // Early evaluation failed, typically because the implicit timezone is not yet known.
                // Try again later at run-time.
                return this;
            }
        } else {
            return this;
        }
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
        boolean fixed = true;
        for (int i=0; i<argument.length; i++) {
            Expression exp = visitor.optimize(argument[i], contextItemType);
            if (exp != argument[i]) {
                adoptChildExpression(exp);
                argument[i] = exp;
            }
            if (fixed && !(argument[i] instanceof Literal)) {
                fixed = false;
            }
        }
        checkArguments(visitor);
        if (fixed) {
            return preEvaluate(visitor);
        } else {
            return this;
        }
    }

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     * @return the result of the early evaluation, or the original expression, or potentially
     * a simplified expression
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (getIntrinsicDependencies() != 0) {
            return this;
        }
        try {
            Literal lit = Literal.makeLiteral(
                    SequenceExtent.makeSequenceExtent(
                            iterate(visitor.getStaticContext().makeEarlyEvaluationContext())));
            ExpressionTool.copyLocationInfo(this, lit);
            return lit;
        } catch (NoDynamicContextException e) {
            // early evaluation failed, usually because implicit timezone required
            return this;
        }
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                for (int i=0; i<argument.length; i++) {
                    argument[i] = doPromotion(argument[i], offer);
                }
            }
            return this;
        }
    }

    /**
     * Method supplied by each class of function to check arguments during parsing, when all
     * the argument expressions have been read
     * @param visitor the expression visitor
    */

    protected abstract void checkArguments(ExpressionVisitor visitor) throws XPathException;

    /**
    * Check number of arguments. <BR>
    * A convenience routine for use in subclasses.
    * @param min the minimum number of arguments allowed
    * @param max the maximum number of arguments allowed
    * @param visitor an expression visitor
     * @return the actual number of arguments
    * @throws client.net.sf.saxon.ce.trans.XPathException if the number of arguments is out of range
    */

    protected int checkArgumentCount(int min, int max, ExpressionVisitor visitor) throws XPathException {
        int numArgs = argument.length;
        if (min==max && numArgs != min) {
            throw new XPathException("Function " + getDisplayName() + " must have "
                    + min + pluralArguments(min),
                    getSourceLocator());
        }
        if (numArgs < min) {
            throw new XPathException("Function " + getDisplayName() + " must have at least "
                    + min + pluralArguments(min),
                    getSourceLocator());
        }
        if (numArgs > max) {
            throw new XPathException("Function " + getDisplayName() + " must have no more than "
                    + max + pluralArguments(max),
                    getSourceLocator());
        }
        return numArgs;
    }

    /**
     * Utility routine used in constructing error messages: get the word "argument" or "arguments"
     * @param num the number of arguments
     * @return the singular or plural word
    */

    private static String pluralArguments(int num) {
        if (num==1) return " argument";
        return " arguments";
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator<Expression> iterateSubExpressions() {
        return Arrays.asList(argument).iterator();
    }

    /**
     * Get the name of the function for display in messages
     * @return  the name of the function as a lexical QName
     */

    public final String getDisplayName() {
        StructuredQName fName = getFunctionName();
        return (fName == null ? "(anonymous)" : fName.getDisplayName());
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        FastStringBuffer buff = new FastStringBuffer(FastStringBuffer.SMALL);
        buff.append(getDisplayName());
        Iterator iter = iterateSubExpressions();
        boolean first = true;
        while (iter.hasNext()) {
            buff.append(first ? "(" : ", ");
            buff.append(iter.next().toString());
            first = false;
        }
        buff.append(first ? "()" : ")");
        return buff.toString();
    }

    /**
     * Determine whether two expressions are equivalent
     */

    public boolean equals(Object o) {
        if (!(o instanceof FunctionCall)) {
            return false;
        }
        FunctionCall f = (FunctionCall)o;
        if (!getFunctionName().equals(f.getFunctionName())) {
            return false;
        }
        if (getNumberOfArguments() != f.getNumberOfArguments()) {
            return false;
        }
        for (int i=0; i<getNumberOfArguments(); i++) {
            if (!argument[i].equals(f.argument[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get hashCode in support of equals() method
     */

    public int hashCode() {
        int h = getFunctionName().hashCode();
        for (int i=0; i<getNumberOfArguments(); i++) {
            h ^= argument[i].hashCode();
        }
        return h;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
