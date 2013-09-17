package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;

/**
 * Implement the XPath normalize-space() function
 */

public class NormalizeSpace extends SystemFunction {

    public NormalizeSpace newInstance() {
        return new NormalizeSpace();
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

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (argument.length == 0 && contextItemType == null) {
            typeError("The context item for normalize-space() is undefined", "XPDY0002");
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
    * Pre-evaluate a function at compile time. Functions that do not allow
    * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (argument.length == 0) {
            return this;
        } else {
            return Literal.makeLiteral(evaluateItem(
                    visitor.getStaticContext().makeEarlyEvaluationContext()));
        }
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        if (argument.length == 0) {
            Item item = c.getContextItem();
            if (item == null) {
                dynamicError("Context item for normalize-space() is undefined", "FONC0001");
                return null;
            }
            return StringValue.makeStringValue(
                    Whitespace.collapseWhitespace(item.getStringValue()));
        } else {
            AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
            if (sv==null) {
                return StringValue.EMPTY_STRING;
            }
            return StringValue.makeStringValue(
                    Whitespace.collapseWhitespace(sv.getStringValue()));
        }
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * <p>This method is implemented for normalize-space() because it is quite often used in a
     * boolean context to test whether a value exists and is non-white, and because testing for the
     * presence of non-white characters is a lot more efficient than constructing the normalized
     * string, especially because of early-exit.</p>
     *
     * @param c The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs evaluating the expression
     */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        CharSequence cs;
        if (argument.length == 0) {
            Item item = c.getContextItem();
            if (item == null) {
                dynamicError("Context item for normalize-space() is undefined", "FONC0001");
                return false;
            }
            cs = item.getStringValue();
        } else {
            AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
            if (sv==null) {
                return false;
            }
            cs = sv.getStringValue();
        }
        return !Whitespace.isWhite(cs);
    }
}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
