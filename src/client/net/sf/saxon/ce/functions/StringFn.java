package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.SimpleNodeConstructor;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.StringValue;

/**
* Implement XPath function string()
*/

public class StringFn extends SystemFunction {

    public StringFn newInstance() {
        return new StringFn();
    }

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault();
        argument[0].setFlattened(true);
        return simplifyArguments(visitor);
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
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (th.isSubType(argument[0].getItemType(th), BuiltInAtomicType.STRING) &&
                argument[0].getCardinality() == StaticProperty.EXACTLY_ONE) {
            return argument[0];
        }
        if (argument[0] instanceof SimpleNodeConstructor) {
            return ((SimpleNodeConstructor)argument[0]).getContentExpression();
        }
        return this;
    }


    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        try {
            Item arg = argument[0].evaluateItem(c);
            if (arg==null) {
                return StringValue.EMPTY_STRING;
            } else if (arg instanceof StringValue && ((StringValue)arg).getTypeLabel() == BuiltInAtomicType.STRING) {
                return arg;
            } else {
                return StringValue.makeStringValue(arg.getStringValueCS());
            }
        } catch (UnsupportedOperationException e) {
            // Cannot obtain the string value of a function item
            typeError(e.getMessage(), "FOTY0014", c);
            return null;
        }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
