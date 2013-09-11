package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.BooleanValue;

/**
* This class supports the XPath functions boolean(), not(), true(), and false()
*/


public class BooleanFn extends SystemFunction  {

    public static final int BOOLEAN = 0;
    public static final int NOT = 1;

    public BooleanFn(int operation) {
        this.operation = operation;
    }

    public BooleanFn newInstance() {
        return new BooleanFn(operation);
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        XPathException err = TypeChecker.ebvError(argument[0]);
        if (err != null) {
            err.setLocator(getSourceLocator());
            throw err;
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
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this) {
//            if (argument[0].getCardinality() == StaticProperty.EXACTLY_ONE) {
//                argIsSingleton = true;
//            }
            if (operation == BOOLEAN) {
                Expression ebv = rewriteEffectiveBooleanValue(argument[0], visitor, contextItemType);
                return (ebv == null ? this : ebv.optimize(visitor, contextItemType));
            } else {
                Expression ebv = rewriteEffectiveBooleanValue(argument[0], visitor, contextItemType);
                if (ebv != null) {
                    argument[0] = ebv;
                }
                return this;
            }
        }
        return e;
    }

    /**
     * Optimize an expression whose effective boolean value is required
     * @param exp the expression whose EBV is to be evaluated
     * @param visitor an expression visitor
     * @param contextItemType the type of the context item for this expression
     * @return an expression that returns the EBV of exp, or null if no optimization was possible
     * @throws XPathException if static errors are found
     */

    public static Expression rewriteEffectiveBooleanValue(
            Expression exp, ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        TypeHierarchy th = TypeHierarchy.getInstance();
        if (exp instanceof BooleanFn && ((BooleanFn)exp).operation == BooleanFn.BOOLEAN) {
            return ((BooleanFn)exp).getArguments()[0];
        } else if (th.isSubType(exp.getItemType(), BuiltInAtomicType.BOOLEAN) &&
                exp.getCardinality() == StaticProperty.EXACTLY_ONE) {
            return exp;
        } else if (exp instanceof Count) {
            // rewrite boolean(count(x)) => exists(x)
            FunctionCall exists = SystemFunction.makeSystemFunction("exists", ((Count)exp).getArguments());
            exists.setSourceLocator(exp.getSourceLocator());
            return exists.optimize(visitor, contextItemType);
        } else if (exp.getItemType() instanceof NodeTest) {
            // rewrite boolean(x) => exists(x)
            FunctionCall exists = SystemFunction.makeSystemFunction("exists", new Expression[]{exp});
            exists.setSourceLocator(exp.getSourceLocator());
            return exists.optimize(visitor, contextItemType);
        } else {
            return null;
        }
    }

 
    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the effective boolean value
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        boolean b = argument[0].effectiveBooleanValue(c);
        return (operation == BOOLEAN ? b : !b);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
