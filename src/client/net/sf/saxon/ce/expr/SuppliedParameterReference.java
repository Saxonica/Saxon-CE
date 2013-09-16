package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Value;

/**
 * Supplied parameter reference: this is an internal expression used to refer to
 * the value of the n'th parameter supplied on a template call (apply-templates).
 * It is used within a type-checking expression designed to check the consistency
 * of the supplied value with the required type. This type checking is all done
 * at run-time, because the binding of apply-templates to actual template rules
 * is entirely dynamic.
 */

public class SuppliedParameterReference extends Expression {

    int slotNumber;
    SequenceType type;

    /**
    * Constructor
    * @param slot identifies this parameter. The value -1 indicates that the value is to be obtained
     * from the dynamic stack held in the context object.
    */

    public SuppliedParameterReference(int slot) {
        slotNumber = slot;
    }

    /**
     * Set the type of the supplied value if known
     * @param type of the supplied value
     */

    public void setSuppliedType(SequenceType type) {
        this.type = type;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
    * Determine the data type of the expression, if possible.
    * @return Type.ITEM, because we don't know the type of the supplied value
    * in advance.
     */

    public ItemType getItemType() {
        if (type != null) {
            return type.getPrimaryType();
        } else {
            return AnyItemType.getInstance();
        }
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class client.net.sf.saxon.ce.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
    }

    /**
    * Get the static cardinality
     * @return ZERO_OR_MORE, unless we know the type of the supplied value
     * in advance.
    */

    public int computeCardinality() {
        if (type != null) {
            return type.getCardinality();
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    /**
    * Get the value of this expression in a given context.
    * @param c the XPathContext which contains the relevant variable bindings
    * @return the value of the variable, if it is defined
    * @throws XPathException if the variable is undefined
    */

    public Sequence evaluateVariable(XPathContext c) throws XPathException {
        return c.evaluateLocalVariable(slotNumber);
    }

    /**
    * Get the value of this expression in a given context.
    * @param context the XPathContext which contains the relevant variable bindings
    * @return the value of the variable, if it is defined
    * @throws XPathException if the variable is undefined
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return Value.asIterator(evaluateVariable(context));
    }

    /**
      * Evaluate an expression as a single item. This always returns either a single Item or
      * null (denoting the empty sequence). No conversion is done. This method should not be
      * used unless the static type of the expression is a subtype of "item" or "item?": that is,
      * it should not be called if the expression may return a sequence. There is no guarantee that
      * this condition will be detected.
      *
      * @param context The context in which the expression is to be evaluated
      * @exception client.net.sf.saxon.ce.trans.XPathException if any dynamic error occurs evaluating the
      *     expression
      * @return the node or atomic value that results from evaluating the
      *     expression; or null to indicate that the result is an empty
      *     sequence
      */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return Value.asItem(evaluateVariable(context));
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     * @return a representation of the expression as a string
     */

    public String toString() {
        return "suppliedParam(" + slotNumber + ")";
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
