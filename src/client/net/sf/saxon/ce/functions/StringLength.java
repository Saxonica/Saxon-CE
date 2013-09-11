package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.*;

/**
 * Implement the XPath string-length() function
 */

public class StringLength extends SystemFunction {

    public StringLength newInstance() {
        return new StringLength();
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class client.net.sf.saxon.ce.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        int d = super.getIntrinsicDependencies();
        if (argument.length == 0) {
            d |= StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
        }
        return d;
    }

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     * @return the expression, either unchanged, or pre-evaluated
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (argument.length == 0) {
            return this;
        } else {
            return Literal.makeLiteral(
                    (Value)evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext()));
        }
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (argument.length == 0 && contextItemType == null) {
            typeError("The context item for string-length() is undefined", "XPDY0002");
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv;
        if (argument.length == 0) {
            final Item contextItem = c.getContextItem();
            if (contextItem == null) {
                dynamicError("The context item for string-length() is not set", "XPDY0002");
                return null;
            }
            sv = StringValue.makeStringValue(contextItem.getStringValue());
        } else {
            sv = (AtomicValue)argument[0].evaluateItem(c);
        }
        if (sv==null) {
            return IntegerValue.ZERO;
        }

        if (sv instanceof StringValue) {

            return new IntegerValue(((StringValue)sv).getStringLength());
        } else {
            CharSequence s = sv.getStringValue();

            return new IntegerValue(StringValue.getStringLength(s));
        }
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
